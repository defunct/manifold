package com.goodworkalan.manifold;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicBoolean;

public class NormalSelection implements Selection
{
    private final Selector selector;
    
    private final AtomicBoolean terminated;
    
    public NormalSelection(Selector selector, AtomicBoolean terminated)
    {
        this.selector = selector;
        this.terminated = terminated;        
    }

    public boolean select() throws IOException
    {
        selector.select();
        return !terminated.get();
    }
}
