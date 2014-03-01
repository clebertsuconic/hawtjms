/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fusesource.amqpjms.provider.failover;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.fusesource.amqpjms.jms.message.JmsInboundMessageDispatch;
import org.fusesource.amqpjms.jms.message.JmsOutboundMessageDispatch;
import org.fusesource.amqpjms.jms.meta.JmsResource;
import org.fusesource.amqpjms.provider.AsyncProvider;
import org.fusesource.amqpjms.provider.BlockingProvider;
import org.fusesource.amqpjms.provider.DefaultBlockingProvider;
import org.fusesource.amqpjms.provider.DefaultProviderListener;
import org.fusesource.amqpjms.provider.ProviderConstants.ACK_TYPE;
import org.fusesource.amqpjms.provider.ProviderFactory;
import org.fusesource.amqpjms.provider.ProviderListener;
import org.fusesource.amqpjms.provider.ProviderRequest;
import org.fusesource.amqpjms.provider.amqp.AmqpConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A Provider Facade that provides services for detection dropped Provider connections
 * and attempting to reconnect to a different remote peer.  Upon establishment of a new
 * connection the FailoverProvider will initiate state recovery of the active JMS
 * framework resources.
 */
public class FailoverProvider extends DefaultProviderListener implements BlockingProvider {

    private static final Logger LOG = LoggerFactory.getLogger(AmqpConnection.class);

    private ProviderListener proxied;
    private AsyncProvider provider;
    private final URI originalURI;
    private final Map<String, String> extraOptions;

    private final ExecutorService serializer;
    private final ScheduledExecutorService connectionHub;
    private final AtomicBoolean closed = new AtomicBoolean();
    private final LinkedList<FailoverRequest<?>> requests = new LinkedList<FailoverRequest<?>>();

    // All state and configuration values related to reconnection.
    private boolean firstConnection = true;
    private final long reconnectDelay = TimeUnit.SECONDS.toMillis(5);
    private final long initialReconnectDealy = 0L;

    public FailoverProvider(URI uri) {
        this(uri, null);
    }

    public FailoverProvider(URI uri, Map<String, String> extraOptions) {
        this.originalURI = uri;
        if (extraOptions != null) {
            this.extraOptions = extraOptions;
        } else {
            this.extraOptions = Collections.emptyMap();
        }

        this.serializer = Executors.newSingleThreadExecutor(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable runner) {
                Thread serial = new Thread(runner);
                serial.setDaemon(true);
                serial.setName("FailoverProvider:");
                return serial;
            }
        });

        // All Connection attempts happen in this schedulers thread.  Once a connection
        // is established it will hand the open connection back to the serializer thread
        // for state recovery.
        this.connectionHub = Executors.newScheduledThreadPool(1, new ThreadFactory() {

            @Override
            public Thread newThread(Runnable runner) {
                Thread serial = new Thread(runner);
                serial.setDaemon(true);
                serial.setName("FailoverProvider: connect thread");
                return serial;
            }
        });
    }

    @Override
    public void connect() throws IOException {
        checkClosed();
        triggerReconnect();
    }

    @Override
    public void close() {
        if (closed.compareAndSet(false, true)) {
            final ProviderRequest<Void> request = new ProviderRequest<Void>();
            serializer.execute(new Runnable() {

                @Override
                public void run() {
                    try {
                        for (FailoverRequest<?> request : requests) {
                            request.onFailure(new IOException("Closed"));
                        }

                        if (provider != null) {
                            provider.close();
                        }
                    } catch (Exception e) {
                        LOG.debug("Caught exception while closing proton connection");
                    } finally {

                        if (connectionHub != null) {
                            connectionHub.shutdown();
                        }

                        if (serializer != null) {
                            serializer.shutdown();
                        }

                        request.onSuccess(null);
                    }
                }
            });

            // TODO - Add a close timeout.
            try {
                request.getResponse();
            } catch (IOException e) {
                LOG.warn("Error caught while closing Provider: ", e.getMessage());
            }
        }
    }

    @Override
    public JmsResource create(final JmsResource resource) throws IOException {
        checkClosed();
        final FailoverRequest<JmsResource> request = new FailoverRequest<JmsResource>() {
            @Override
            public void doTask() throws IOException {
                provider.create(resource, this);
            }
        };

        serializer.execute(request);
        return request.getResponse();
    }

    @Override
    public void destroy(final JmsResource resource) throws IOException {
        checkClosed();
        final FailoverRequest<Void> request = new FailoverRequest<Void>() {
            @Override
            public void doTask() throws IOException {
                provider.destroy(resource, this);
            }
        };

        serializer.execute(request);
        request.getResponse();
    }

    @Override
    public void send(final JmsOutboundMessageDispatch envelope) throws IOException {
        checkClosed();
        final FailoverRequest<Void> request = new FailoverRequest<Void>() {
            @Override
            public void doTask() throws IOException {
                provider.send(envelope, this);
            }
        };

        serializer.execute(request);
        request.getResponse();
    }

    @Override
    public void acknowledge(final JmsInboundMessageDispatch envelope, final ACK_TYPE ackType) throws IOException {
        checkClosed();
        final FailoverRequest<Void> request = new FailoverRequest<Void>() {
            @Override
            public void doTask() throws IOException {
                provider.acknowledge(envelope, ackType, this);
            }
        };

        serializer.execute(request);
        request.getResponse();
    }

    @Override
    public void receoveryComplate() throws IOException {
        checkClosed();
        // TODO Auto-generated method stub
    }

    @Override
    public void setProviderListener(ProviderListener listener) {
        this.proxied = listener;
    }

    @Override
    public ProviderListener getProviderListener() {
        return proxied;
    }

    @Override
    public URI getRemoteURI() {
        AsyncProvider provider = this.provider;
        if (provider != null) {
            return provider.getRemoteURI();
        }
        return null;
    }

    protected void checkClosed() throws IOException {
        if (closed.get()) {
            throw new IOException("The Provider is already closed");
        }
    }

    private void triggerReconnect() {
        if (!closed.get()) {

            long delay = initialReconnectDealy;
            if (!firstConnection) {
                delay += reconnectDelay;
            }

            LOG.info("Scheduling reconnect attempt to fire in {} milliseconds", delay);

            connectionHub.schedule(new Runnable() {
                @Override
                public void run() {
                    if (provider == null) {
                        try {
                            AsyncProvider provider = ProviderFactory.createAsync(originalURI);
                            provider.connect();
                            handleNewConnection(provider);
                        } catch (Throwable e) {
                            LOG.info("Connection attempt to: {} failed.", originalURI);
                            triggerReconnect();
                        }
                    }
                }
            }, delay, TimeUnit.MILLISECONDS);
        }
    }

    private void handleNewConnection(final AsyncProvider provider) {
        this.serializer.execute(new Runnable() {
            @Override
            public void run() {
                if (firstConnection) {
                    firstConnection = false;
                    FailoverProvider.this.provider = provider;

                    for (FailoverRequest<?> request : requests) {
                        request.run();
                    }
                } else {
                    // TODO - Attempt state recovery.
                    proxied.onConnectionRecovery(new DefaultBlockingProvider(provider));
                    FailoverProvider.this.provider = provider;
                    proxied.onConnectionRestored();
                }
            }
        });
    }

    @Override
    public void onMessage(JmsInboundMessageDispatch envelope) {
        this.proxied.onMessage(envelope);
    }

    @Override
    public void onConnectionFailure(IOException ex) {
        if (closed.get()) {
            return;
        }
        serializer.execute(new Runnable() {

            @Override
            public void run() {
                if (!closed.get()) {
                    provider = null;
                    triggerReconnect();
                    proxied.onConnectionInterrupted();
                }
                // TODO - start reconnection
                //        signal transport interrupted.
                //        this could contend with a request
                //        so we must be careful not to schedule two reconnect attempts.
            }
        });
    }

    // Wraps incoming requests so they can be queued if offline.
    private abstract class FailoverRequest<T> extends ProviderRequest<T> implements Runnable {

        @Override
        public void run() {
            if (provider == null) {
                requests.addLast(this);
            } else {
                try {
                    doTask();
                } catch (IOException e) {
                    this.onFailure(e);
                }
            }
        }

        public abstract void doTask() throws IOException;

    }
}
