package com.goodworkalan.manifold;

import java.nio.ByteBuffer;

/**
 * A worker that handles accept and read events for a server implementation.
 * 
 * @author Alan Gutierrez
 */
public interface Session
{
    /**
     * Accept a new conversation by instructing the selector thread to either
     * listen to the conversation socket or else send data over the conversation
     * socket.
     * 
     * @param remote
     *            The IP address of the client.
     * @param conversation
     *            The new conversation.
     */
    public void accepted(Sender sender);

    /**
     * Read the given data that has arrived over the socket associated with the
     * given socket.
     * 
     * @param conversation
     *            The socket conversation.
     * @param data
     *            Data that has arrived over the conversation socket.
     */
    public void read(ByteBuffer data, Sender sender);
    
    public void terminate(Sender sender);
    
    public void close();
}
