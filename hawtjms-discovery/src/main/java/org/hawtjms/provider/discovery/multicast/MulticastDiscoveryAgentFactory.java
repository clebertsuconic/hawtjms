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

import io.hawtjms.util.PropertyUtil;
import io.hawtjms.util.URISupport;

import java.net.URI;
import java.util.Map;

import org.hawtjms.provider.discovery.DiscoveryAgent;
import org.hawtjms.provider.discovery.DiscoveryAgentFactory;

/**
 * Creates and configures a new instance of the mutlicast agent.
 */
public class MulticastDiscoveryAgentFactory extends DiscoveryAgentFactory {

    @Override
    public DiscoveryAgent createDiscoveryAgent(URI discoveryURI) throws Exception {
        MulticastDiscoveryAgent agent = new MulticastDiscoveryAgent(discoveryURI);
        Map<String, String> options = URISupport.parseParameters(discoveryURI);
        PropertyUtil.setProperties(agent, options);
        return agent;
    }

    @Override
    public String getName() {
        return "multicast";
    }
}
