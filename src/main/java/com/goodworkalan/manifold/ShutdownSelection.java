package com.goodworkalan.manifold;

import java.io.IOException;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;

public class ShutdownSelection implements Selection
{
    private final Selector selector;
    
    private final Map<SocketChannel, Conversation> conversations;
    
    
    public ShutdownSelection(Selector selector, Map<SocketChannel, Conversation> conversations)
    {
        this.selector = selector;
        this.conversations = conversations;
    }
    
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
