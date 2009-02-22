package com.goodworkalan.manifold;

import java.nio.ByteBuffer;
import java.util.Collection;

public interface Sender
{
    public void send(Collection<ByteBuffer> data);
    
    public void send(ByteBuffer...data);
    
    public void close();
}
