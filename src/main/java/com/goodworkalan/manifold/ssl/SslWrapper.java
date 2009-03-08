package com.goodworkalan.manifold.ssl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import com.goodworkalan.manifold.Sender;
import com.goodworkalan.manifold.Wrapper;

// TODO Document.
public class SslWrapper implements Wrapper
{
    // TODO Document.
    private final SSLEngine sslEngine;
    
    // TODO Document.
    private ByteBuffer unencrypted;
    
    // TODO Document.
    private ByteBuffer incoming;
    
    // TODO Document.
    private ByteBuffer encrypted;

    // TODO Document.
    public SslWrapper(SSLEngine sslEngine)
    {
        SSLSession sslSession = sslEngine.getSession();
        this.sslEngine = sslEngine;
        this.unencrypted = ByteBuffer.allocate(sslSession.getApplicationBufferSize());
        this.incoming = ByteBuffer.allocate(sslSession.getPacketBufferSize());
        this.incoming.limit(0);
        this.encrypted = ByteBuffer.allocate(sslSession.getPacketBufferSize());
    }
    
    // TODO Document.
    public ByteBuffer[] wrap(ByteBuffer unwrapped, Sender sender)
    {
        try
        {
            List<ByteBuffer> byteBuffers = new ArrayList<ByteBuffer>(); 
            SSLSession sslSession = sslEngine.getSession();

            int unwrappedLimit = unwrapped.limit();
            while (unwrapped.remaining() != 0)
            {
                if (unwrapped.remaining() > sslSession.getApplicationBufferSize())
                {
                    unwrapped.limit(unwrappedLimit - (unwrapped.remaining() - sslSession.getApplicationBufferSize()));
                }
                
//                int slicePosition = encrypted.position();
                SSLEngineResult result = sslEngine.wrap(unwrapped, encrypted);
                switch (result.getHandshakeStatus())
                {
                case NEED_TASK:
                    Runnable runnable = null;
                    while ((runnable = sslEngine.getDelegatedTask()) != null)
                    {
                        runnable.run();
                    }
                    break;
                }
                switch (result.getStatus())
                {
                case OK:
//                    int position = encrypted.position();
//                    
//                    encrypted.position(slicePosition);
//                    encrypted.limit(position);
//                    byteBuffers.add(encrypted.slice());
//                    
//                    encrypted.position(position);
//                    encrypted.limit(encrypted.capacity());
                    encrypted.flip();
                    byteBuffers.add(encrypted);
                    encrypted = ByteBuffer.allocate(sslSession.getPacketBufferSize());
                    break;
                case BUFFER_OVERFLOW:
                    encrypted = ByteBuffer.allocate(sslSession.getPacketBufferSize());
                    break;
                }
                
                unwrapped.limit(unwrappedLimit);
            }
            
            return byteBuffers.toArray(new ByteBuffer[byteBuffers.size()]);
        }
        catch (SSLException e)
        {
            return new ByteBuffer[0];
        }
    }
    
    // TODO Document.
    public ByteBuffer[] unwrap(ByteBuffer wrapped, Sender sender)
    {
        try
        {
            List<ByteBuffer> byteBuffers = new ArrayList<ByteBuffer>(); 
            SSLSession sslSession = sslEngine.getSession();

            int wrappedLimit = wrapped.limit();
            while (wrapped.remaining() != 0)
            {
                int incomingRemaining = incoming.remaining();

                incoming.compact();

                if (incomingRemaining < incoming.capacity() && wrappedLimit - wrapped.position() != 0)
                {
                    int overflow = (wrappedLimit - wrapped.position()) - incoming.remaining();
                    if (overflow > 0)
                    {
                        wrapped.limit(wrapped.position() + incoming.remaining());
                    }
                    else
                    {
                        wrapped.limit(wrappedLimit);
                    }
                    incomingRemaining += wrapped.remaining();
                    incoming.put(wrapped);
                }
                
                incoming.flip();
                
//                int slicePosition = unencrypted.position();
                SSLEngineResult result = sslEngine.unwrap(incoming, unencrypted);
                switch (result.getHandshakeStatus())
                {
                case NEED_TASK:
                    Runnable runnable = null;
                    while ((runnable = sslEngine.getDelegatedTask()) != null)
                    {
                        runnable.run();
                    }
                    break;
                }
                switch (result.getStatus())
                {
                case OK:
//                    int position = unencrypted.position();
//                    
//                    unencrypted.position(slicePosition);
//                    unencrypted.limit(position);
//                    byteBuffers.add(unencrypted.slice());
//                    
//                    unencrypted.position(position);
//                    unencrypted.limit(unencrypted.capacity());
                    unencrypted.flip();
                    byteBuffers.add(unencrypted);
                    unencrypted = ByteBuffer.allocate(sslSession.getApplicationBufferSize());
                    break;
                case BUFFER_UNDERFLOW:
                    break;
                case BUFFER_OVERFLOW:
                    unencrypted = ByteBuffer.allocate(sslSession.getApplicationBufferSize());
                    break;
                }
                
                wrapped.limit(wrappedLimit);
            }
            
            return byteBuffers.toArray(new ByteBuffer[byteBuffers.size()]);
        }
        catch (SSLException e)
        {
            return new ByteBuffer[0];
        }
    }
}
