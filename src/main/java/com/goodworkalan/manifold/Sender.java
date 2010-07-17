package com.goodworkalan.manifold;

import java.nio.ByteBuffer;
import java.util.Collection;

// TODO Document.
public interface Sender {
    // TODO Document.
    public void send(Collection<ByteBuffer> data);

    // TODO Document.
    public void send(ByteBuffer... data);

    // TODO Document.
    public void close();

    // TODO Document.
    public void setWrapper(Wrapper wrapper);
}
