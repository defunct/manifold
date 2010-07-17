package com.goodworkalan.manifold;

import java.nio.ByteBuffer;

// TODO Document.
public interface Wrapper {
    // TODO Document.
    public ByteBuffer[] wrap(ByteBuffer unwrapped, Sender sender);

    // TODO Document.
    public ByteBuffer[] unwrap(ByteBuffer wrapped, Sender sender);
}
