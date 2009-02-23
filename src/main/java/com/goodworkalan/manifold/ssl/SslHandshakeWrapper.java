package com.goodworkalan.manifold.ssl;

import static javax.net.ssl.SSLEngineResult.HandshakeStatus.FINISHED;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_TASK;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_UNWRAP;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NEED_WRAP;
import static javax.net.ssl.SSLEngineResult.HandshakeStatus.NOT_HANDSHAKING;
import static javax.net.ssl.SSLEngineResult.Status.BUFFER_UNDERFLOW;
import static javax.net.ssl.SSLEngineResult.Status.OK;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import com.goodworkalan.manifold.Sender;
import com.goodworkalan.manifold.Wrapper;

public class SslHandshakeWrapper implements Wrapper
{
    /** An empty buffer to wrap.  */
    private final static ByteBuffer BLANK = ByteBuffer.allocate(0);

    /** The SSL engine to use for this socket. */
    private final SSLEngine sslEngine;

    /** A buffer for incoming SSL encrypted data. */
    private ByteBuffer incoming;

    public SslHandshakeWrapper(SSLEngine sslEngine)
    {
        this.sslEngine = sslEngine;
        this.incoming = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
        this.incoming.limit(0);
    }
    
    public ByteBuffer[] unwrap(ByteBuffer wrapped, Sender sender)
    {
        SSLSession session = sslEngine.getSession();
        
        ByteBuffer destination = ByteBuffer.allocate(session.getApplicationBufferSize());
        try
        {
            boolean finished = false;
            int wrappedLimit = wrapped.limit();
            SSLEngineResult.HandshakeStatus handShakeStatus;
            SSLEngineResult.Status status = OK;
            do
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
                
                handShakeStatus = sslEngine.getHandshakeStatus();
                switch (handShakeStatus)
                {
                case NOT_HANDSHAKING:
                    sslEngine.beginHandshake();
                    break;
                case FINISHED:
                    break;
                case NEED_TASK:
                    Runnable runnable = null;
                    while ((runnable = sslEngine.getDelegatedTask()) != null)
                    {
                        runnable.run();
                    }
                    break;
                case NEED_WRAP:
                    sender.send(BLANK);
                    break;
                case NEED_UNWRAP:
                    SSLEngineResult result = sslEngine.unwrap(incoming, destination);
                    status = result.getStatus();
                    finished = result.getHandshakeStatus() == FINISHED;
                    System.out.println(destination.remaining() == destination.capacity());
                    break;
                }
            } while (
                !finished
                &&
                (status == OK || (status == BUFFER_UNDERFLOW && wrappedLimit - wrapped.position() != 0))
                &&
                (handShakeStatus == NOT_HANDSHAKING ||handShakeStatus == NEED_UNWRAP || handShakeStatus == NEED_TASK));
            
            if (finished)
            {
                sender.setWrapper(new SslWrapper(sslEngine));
            }
            
            return new ByteBuffer[0];
        }
        catch (SSLException e)
        {
            throw new RuntimeException("Unable to get going here");
        }       
    }

    public ByteBuffer[] wrap(ByteBuffer unwrapped, Sender sender)
    {
        try
        {
            List<ByteBuffer> byteBuffers = new ArrayList<ByteBuffer>();
            SSLEngineResult.HandshakeStatus handShakeStatus;
            boolean finished = false;
            do
            {
                handShakeStatus = sslEngine.getHandshakeStatus();
                switch (handShakeStatus)
                {
                case NOT_HANDSHAKING:
                    break;
                case FINISHED:
                    break;
                case NEED_TASK:
                    Runnable runnable = null;
                    while ((runnable = sslEngine.getDelegatedTask()) != null)
                    {
                        runnable.run();
                    }
                    break;
                case NEED_WRAP:
                    ByteBuffer packet = ByteBuffer.allocate(sslEngine.getSession().getPacketBufferSize());
                    SSLEngineResult result = sslEngine.wrap(BLANK, packet);
                    finished = result.getHandshakeStatus() == FINISHED;
                    switch (result.getStatus())
                    {
                    case OK:
                        packet.flip();
                        byteBuffers.add(packet);
                        break;
                    }
                    break;
                case NEED_UNWRAP:
                    break;
                }
            } while (handShakeStatus == NEED_WRAP || handShakeStatus == NEED_TASK);

            if (finished)
            {
                sender.setWrapper(new SslWrapper(sslEngine));
            }
            
            return byteBuffers.toArray(new ByteBuffer[byteBuffers.size()]);
        }
        catch (SSLException e)
        {
            return new ByteBuffer[0];
        }
    }
}
