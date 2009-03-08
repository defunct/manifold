package com.goodworkalan.manifold;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

// TODO Document.
public class ShutdownSelection implements Selection
{
    // TODO Document.
    private final Selector selector;
    
    // TODO Document.
    private final Map<SocketChannel, Conversation> conversations;
    
    // TODO Document.
    public ShutdownSelection(Selector selector, Map<SocketChannel, Conversation> conversations)
    {
        this.selector = selector;
        this.conversations = conversations;
    }
    
    // TODO Document.
    public boolean select() throws IOException
    {
        if (conversations.size() == 0)
        {
            return false;
        }
        selector.select();
        return true;
    }
}
