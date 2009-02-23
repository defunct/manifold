package com.goodworkalan.manifold;

import java.nio.ByteBuffer;

public interface Wrapper
{
    public ByteBuffer[] wrap(ByteBuffer unwrapped, Sender sender);
    
    public ByteBuffer[] unwrap(ByteBuffer wrapped, Sender sender);
}
