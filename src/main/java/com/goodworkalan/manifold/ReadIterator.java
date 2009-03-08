package com.goodworkalan.manifold;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.LinkedList;

// TODO Document.
public class ReadIterator implements Iterator<ByteBuffer>
{
    // TODO Document.
    private final LinkedList<ByteBuffer> queue;
    
    // TODO Document.
    private boolean visited;
    
    // TODO Document.
    public ReadIterator(LinkedList<ByteBuffer> queue)
    {
        this.queue = queue;
    }
    
    // TODO Document.
    public boolean hasNext()
    {
        return !visited;
    }
    
    // TODO Document.
    public ByteBuffer next() 
    {
        if (visited)
        {
            return null;
        }
        visited = true;
        return queue.removeFirst();
    }
    
    // TODO Document.
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
}
