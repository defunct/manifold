package com.goodworkalan.manifold;

import java.nio.ByteBuffer;

// TODO Document.
public class NullWrapper implements Wrapper {
    // TODO Document.
    public ByteBuffer[] wrap(ByteBuffer unwrapped, Sender sender) {
        return new ByteBuffer[] { unwrapped };
    }

    // TODO Document.
    public ByteBuffer[] unwrap(ByteBuffer wrapped, Sender sender) {
        return new ByteBuffer[] { wrapped };
    }
}
