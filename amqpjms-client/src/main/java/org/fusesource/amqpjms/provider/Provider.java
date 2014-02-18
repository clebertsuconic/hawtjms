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
package org.fusesource.amqpjms.provider;

import java.io.IOException;
import java.net.URI;

/**
 * Defines the interface that is implemented by a Protocol Provider object
 * in order to map JMS functionality into the given wire level protocol.
 */
public interface Provider {

    /**
     * Performs the initial low level connection for this provider such as TCP or
     * SSL connection to a remote Broker.  If this operation fails then the Provider
     * is considered to be unusable and no further operations should be attempted
     * using this Provider.
     *
     * @throws IOException if the remote resource can not be contacted.
     */
    void connect() throws IOException;

    /**
     * Closes this Provider terminating all connections and canceling any pending
     * operations.  The Provider is considered unusable after this call.
     */
    void close();

    /**
     * Called to indicate that a signaled recovery cycle is not complete.
     *
     * This method is used only when the Provider is a fault tolerant implementation and the
     * recovery started event has been fired after a reconnect.  This allows for the fault
     * tolerant implementation to perform any intermediate processing before a transition
     * to a recovered state.
     *
     * @throws IOException if an error occurs during recovery completion processing.
     */
    void receoveryComplate() throws IOException;

    /**
     * Returns the URI used to configure this Provider and specify the remote address of the
     * Broker it connects to.
     *
     * @return the URI used to configure this Provider.
     */
    URI getRemoteURI();

}