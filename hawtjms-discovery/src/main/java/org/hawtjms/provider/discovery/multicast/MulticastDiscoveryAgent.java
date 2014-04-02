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
package org.hawtjms.provider.discovery.multicast;

import java.io.IOException;
import java.net.URI;

import org.hawtjms.provider.discovery.DiscoveryAgent;
import org.hawtjms.provider.discovery.DiscoveryListener;

/**
 * Discovery agent that listens on a multicast address for new Broker advisories.
 */
public class MulticastDiscoveryAgent implements DiscoveryAgent {

    private DiscoveryListener listener;
    private final URI discoveryURI;

    public MulticastDiscoveryAgent(URI discoveryURI) {
        this.discoveryURI = discoveryURI;
    }

    @Override
    public void setDiscoveryListener(DiscoveryListener listener) {
        this.listener = listener;
    }

    public DiscoveryListener getDiscoveryListener() {
        return this.listener;
    }

    @Override
    public void start() throws IOException, IllegalStateException {
        if (listener == null) {
            throw new IllegalStateException("No DiscoveryListener configured.");
        }
    }

    @Override
    public void close() {
        // TODO
    }

    @Override
    public void suspend() {
        // TODO Auto-generated method stub
    }

    @Override
    public void resume() {
        // TODO Auto-generated method stub
    }

    public URI getDiscvoeryURI() {
        return this.discoveryURI;
    }
}
