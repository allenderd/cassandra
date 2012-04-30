/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cassandra.db;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.apache.cassandra.io.IVersionedSerializer;
import org.apache.cassandra.net.Message;
import org.apache.cassandra.utils.ByteBufferUtil;
import org.apache.cassandra.utils.FBUtilities;


/*
 * This message is sent back the row mutation verb handler
 * and basically specifies if the write succeeded or not for a particular
 * key in a table
 */
public class WriteResponse
{
    private static final WriteResponseSerializer serializer = new WriteResponseSerializer();

    public static WriteResponseSerializer serializer()
    {
        return serializer;
    }

    public static Message makeWriteResponseMessage(Message original, WriteResponse respose) throws IOException
    {
        byte[] bytes = FBUtilities.serialize(respose, WriteResponse.serializer(), original.getVersion());
        return original.getReply(FBUtilities.getBroadcastAddress(), bytes, original.getVersion());
    }

    private final String table;
    private final ByteBuffer key;
    private final boolean status;

    public WriteResponse(String table, ByteBuffer key, boolean bVal)
    {
        this.table = table;
        this.key = key;
        this.status = bVal;
    }

    public String table()
    {
        return table;
    }

    public ByteBuffer key()
    {
        return key;
    }

    public boolean isSuccess()
    {
        return status;
    }

    public static class WriteResponseSerializer implements IVersionedSerializer<WriteResponse>
    {
        public void serialize(WriteResponse wm, DataOutput dos, int version) throws IOException
        {
            dos.writeUTF(wm.table());
            ByteBufferUtil.writeWithShortLength(wm.key(), dos);
            dos.writeBoolean(wm.isSuccess());
        }

        public WriteResponse deserialize(DataInput dis, int version) throws IOException
        {
            String table = dis.readUTF();
            ByteBuffer key = ByteBufferUtil.readWithShortLength(dis);
            boolean status = dis.readBoolean();
            return new WriteResponse(table, key, status);
        }

        public long serializedSize(WriteResponse response, int version)
        {
            DBTypeSizes typeSizes = DBTypeSizes.NATIVE;
            int utfSize = FBUtilities.encodedUTF8Length(response.table());
            int keySize = response.key().remaining();
            int size = typeSizes.sizeof((short) utfSize) + utfSize;
            size += typeSizes.sizeof((short) keySize) + keySize;
            size += typeSizes.sizeof(response.isSuccess());
            return size;
        }
    }
}
