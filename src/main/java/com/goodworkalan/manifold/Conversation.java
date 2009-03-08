package com.goodworkalan.manifold;

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

/**
 * A key used to identify a conversation on a specific socket channel in calls
 * between the selector thread and worker threads.
 * <p>
 * The only logical key to identify an open socket is the socket channel, but we
 * don't want to expose the socket channel to the work threads. This class wraps
 * the socket channel, but uses it to implement equals and hash code, so that
 * this class can be used as a key to identify a conversation on a specific
 * socket.
 * 
 * @author Alan Guiterrez
 */
public class Conversation implements Sender
{
    // TODO Document.
    final Plenum plenum;
    
    /** The underlying socket channel. */
    final SocketChannel socketChannel;
    
    /** A linked list of operations to perform. */
    final Queue<Runnable> operations = new LinkedList<Runnable>();
    
    // TODO Document.
    final Queue<ByteBuffer> out = new LinkedList<ByteBuffer>();
    
    /** The session state. */
    final Session session;
    
    // TODO Document.
    Wrapper wrapper = new NullWrapper();
    
    // TODO Document.
    boolean registered;
    
    // TODO Document.
    boolean closed;

    /**
     * Create a key using the given socket channel.
     * 
     * @param socketChannel
     *            The socket channel.
     * @param session
     *            The session state.
     */
    public Conversation(Plenum plenum, SocketChannel socketChannel, Session session)
    {
        this.plenum = plenum;
        this.socketChannel = socketChannel;
        this.session = session;
    }
    
    // TODO Document.
    public void send(Collection<ByteBuffer> data)
    {
        for (ByteBuffer unwrapped : data)
        {
            for (ByteBuffer wrapped : wrapper.wrap(unwrapped, this))
            {
                out.add(wrapped);
            }
        }
    }
    
    // TODO Document.
    public void send(ByteBuffer...data)
    {
        for (int i = 0; i < data.length; i++)
        {
            for (ByteBuffer wrapped : wrapper.wrap(data[i], this))
            {
                out.add(wrapped);
            }
        }
    }
    
    // TODO Document.
    public void close()
    {
        closed = true;
    }
    
    // TODO Document.
    public void setWrapper(Wrapper wrapper)
    {
        this.wrapper = wrapper;
    }
}
