package com.goodworkalan.manifold;

import java.nio.ByteBuffer;

public class NullWrapper implements Wrapper
{
    public ByteBuffer[] wrap(ByteBuffer unwrapped, Sender sender)
    {
        return new ByteBuffer[] { unwrapped };
    }
    
    public ByteBuffer[] unwrap(ByteBuffer wrapped, Sender sender)
    {
        return new ByteBuffer[] { wrapped };
    }
}
