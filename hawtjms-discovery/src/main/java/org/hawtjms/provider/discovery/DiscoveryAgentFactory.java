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
package org.hawtjms.provider.discovery;

import io.hawtjms.util.FactoryFinder;

import java.io.IOException;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory used to find and create instances of DiscoveryAgent using the name
 * of the desired agent to locate it's factory class definition file.
 */
public abstract class DiscoveryAgentFactory {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryAgentFactory.class);

    private static final FactoryFinder<DiscoveryAgentFactory> AGENT_FACTORY_FINDER =
        new FactoryFinder<DiscoveryAgentFactory>(DiscoveryAgentFactory.class, "META-INF/services/io/hawtjms/discovery/agents");

    /**
     * Creates an instance of the given AsyncProvider and configures it using the
     * properties set on the given remote broker URI.
     *
     * @param remoteURI
     *        The URI used to connect to a remote Broker.
     *
     * @return a new AsyncProvider instance.
     *
     * @throws Exception if an error occurs while creating the Provider instance.
     */
    public abstract DiscoveryAgent createDiscoveryAgent(URI remoteURI) throws Exception;

    /**
     * @return the name of this JMS Provider, e.g. STOMP, AMQP, MQTT...etc
     */
    public abstract String getName();

    /**
     * Static create method that performs the DiscoveryAgent search and handles the
     * configuration and setup.
     *
     * @param remoteURI
     *        the URI used to configure the discovery mechanism.
     *
     * @return a new DiscoveryAgent instance that is ready for use.
     *
     * @throws Exception if an error occurs while creating the DiscoveryAgent instance.
     */
    public static DiscoveryAgent createAgent(URI remoteURI) throws Exception {
        DiscoveryAgent result = null;

        try {
            DiscoveryAgentFactory factory = findAgentFactory(remoteURI);
            result = factory.createDiscoveryAgent(remoteURI);
        } catch (Exception ex) {
            LOG.error("Failed to create BlockingProvider instance for: {}", remoteURI.getScheme());
            LOG.trace("Error: ", ex);
            throw ex;
        }

        return result;
    }

    /**
     * Searches for a DiscoveryAgentFactory by using the scheme from the given URI.
     *
     * The search first checks the local cache of discovery agent factories before moving on
     * to search in the classpath.
     *
     * @param location
     *        The URI whose scheme will be used to locate a DiscoveryAgentFactory.
     *
     * @return a Discovery Agent factory instance matching the URI's scheme.
     *
     * @throws IOException if an error occurs while locating the factory.
     */
    protected static DiscoveryAgentFactory findAgentFactory(URI location) throws IOException {
        String scheme = location.getScheme();
        if (scheme == null) {
            throw new IOException("No Discovery Agent scheme specified: [" + location + "]");
        }

        DiscoveryAgentFactory factory = null;
        if (factory == null) {
            try {
                factory = AGENT_FACTORY_FINDER.newInstance(scheme);
            } catch (Throwable e) {
                throw new IOException("Discovery Agent scheme NOT recognized: [" + scheme + "]", e);
            }
        }

        return factory;
    }
}
