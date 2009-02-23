package com.goodworkalan.manifold.sasl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.security.sasl.SaslException;
import javax.security.sasl.SaslServer;

import com.goodworkalan.manifold.Sender;
import com.goodworkalan.manifold.Wrapper;

public class SaslWrapper implements Wrapper
{
    private final SaslServer saslServer;
    
    private byte[] temporary = new byte[1024];
    
    int incomingSize = 0;
    
    int ougoingSize = 0;
    
    private ByteBuffer incoming = ByteBuffer.allocate(1024);

    public SaslWrapper(SaslServer saslServer)
    {
        this.saslServer = saslServer;
    }
    
    public ByteBuffer[] wrap(ByteBuffer unwrapped, Sender sender)
    {
        int size = unwrapped.remaining();
        if (size < temporary.length)
        {
            temporary = new byte[size];
        }
        unwrapped.get(temporary);
        try
        {
            byte[] wrapped = saslServer.unwrap(temporary, 0, size);
            ByteBuffer out = ByteBuffer.allocate(4 + wrapped.length);
            out.putInt(wrapped.length);
            out.put(wrapped);
            out.flip();
            return new ByteBuffer[] { out };
        }
        catch (SaslException e)
        {
            return new ByteBuffer[0];
        }
    }
    
    public ByteBuffer[] unwrap(ByteBuffer wrapped, Sender sender)
    {

        try
        {
            List<ByteBuffer> byteBuffers = new ArrayList<ByteBuffer>();
            for (;;)
            {
                if (incomingSize == 0)
                {
                    int needed = 4 - incoming.remaining();
                    if (needed > 0)
                    {
                        incoming.compact();
                        while (needed-- != 0 && wrapped.remaining() != 0)
                        {
                            incoming.put(wrapped.get());
                        }
                        incoming.flip();
                    }
                    if (incoming.remaining() != 0)
                    {
                        incomingSize = incoming.getInt();
                    }
                }
                
                if (incomingSize == 0)
                {
                    break;
                }

                int remaining = incoming.remaining() + wrapped.remaining();
                if (remaining < incomingSize)
                {
                    if (incoming.capacity() < remaining)
                    {
                        incoming.compact();
                        incoming.put(wrapped);
                        incoming.flip();
                    }
                    else
                    {
                        ByteBuffer newIncoming = ByteBuffer.allocate(remaining);
                        newIncoming.put(incoming);
                        newIncoming.put(wrapped);
                        newIncoming.flip();
                    }
                    break;
                }
                else
                {
                    if (temporary.length < incomingSize)
                    {
                        temporary = new byte[incomingSize];
                    }
                    int fromIncoming = Math.min(incoming.remaining(), incomingSize);
                    incoming.get(temporary, 0, fromIncoming);
                    if (fromIncoming < incomingSize)
                    {
                        wrapped.get(temporary, fromIncoming, incomingSize - fromIncoming);
                    }
                    byteBuffers.add(ByteBuffer.wrap(saslServer.wrap(temporary, 0, incomingSize)));
                    incomingSize = 0;
                }
            }

            return byteBuffers.toArray(new ByteBuffer[byteBuffers.size()]);
        }
        catch (SaslException e)
        {
            return new ByteBuffer[0];
        }
    }
}
