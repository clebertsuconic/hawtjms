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
package io.hawtjms.transports;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Base class for all hawtJMS Transport instances.
 */
public interface Transport {

    /**
     * Performs the protocol connect operation for the implemented Transport type
     * such as a TCP socket connection etc.
     *
     * @throws IOException if an error occurs while attempting the connect.
     */
    void connect() throws IOException;

    /**
     * @return true if transport is connected or false if the connection is down.
     */
    boolean isConnected();

    /**
     * Close the Transport, no additional send operations are accepted.
     *
     * @throws IOException if an error occurs while closing the connection.
     */
    void close() throws IOException;

    /**
     * Sends a chunk of data over the Transport connection.
     *
     * @param output
     *        The buffer of data that is to be transmitted.
     *
     * @throws IOException if an error occurs during the send operation.
     */
    void send(ByteBuffer output) throws IOException;

}
