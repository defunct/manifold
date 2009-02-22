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
    final Manifold manifold;
    
    /** The underlying socket channel. */
    final SocketChannel socketChannel;
    
    /** A linked list of operations to perform. */
    final Queue<Runnable> operations = new LinkedList<Runnable>();
    
    final Queue<ByteBuffer> out = new LinkedList<ByteBuffer>();
    
    /** The session state. */
    final Session session;
    
    boolean registered;
    
    boolean closed;

    /**
     * Create a key using the given socket channel.
     * 
     * @param socketChannel
     *            The socket channel.
     * @param session
     *            The session state.
     */
    public Conversation(Manifold manifold, SocketChannel socketChannel, Session session)
    {
        this.manifold = manifold;
        this.socketChannel = socketChannel;
        this.session = session;
    }
    
    public void send(Collection<ByteBuffer> data)
    {
        out.addAll(data);
        manifold.send(this);
    }
    
    public void send(ByteBuffer...data)
    {
        for (int i = 0; i < data.length; i++)
        {
            out.add(data[i]);
        }
        manifold.send(this);
    }
    
    public void close()
    {
        closed = true;
    }
}
