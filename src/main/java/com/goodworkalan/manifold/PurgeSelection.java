package com.goodworkalan.manifold;

import java.io.IOException;
import java.nio.channels.Selector;

public class PurgeSelection implements Selection
{
    private final Selector selector;
    
    public PurgeSelection(Selector selector)
    {
        this.selector = selector;
    }
    
    public boolean select() throws IOException
    {
        return selector.selectNow() != 0;
    }
}
