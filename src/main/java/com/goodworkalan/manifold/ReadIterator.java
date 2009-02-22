package com.goodworkalan.manifold;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;

public class ReadIterator implements Iterator<ByteBuffer>
{
    private final LinkedList<ByteBuffer> queue;
    
    private boolean visited;
    
    public ReadIterator(LinkedList<ByteBuffer> queue)
    {
        this.queue = queue;
    }
    
    public boolean hasNext()
    {
        return !visited;
    }
    
    public ByteBuffer next() 
    {
        if (visited)
        {
            return null;
        }
        visited = true;
        return queue.removeFirst();
    }
    
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
