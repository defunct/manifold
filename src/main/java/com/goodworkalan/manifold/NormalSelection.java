package com.goodworkalan.manifold;

import java.io.IOException;
import java.nio.channels.Selector;
import java.util.concurrent.atomic.AtomicBoolean;

// TODO Document.
public class NormalSelection implements Selection {
    // TODO Document.
    private final Selector selector;

    // TODO Document.
    private final AtomicBoolean terminated;

    // TODO Document.
    public NormalSelection(Selector selector, AtomicBoolean terminated) {
        this.selector = selector;
        this.terminated = terminated;
    }

    // TODO Document.
    public boolean select() throws IOException {
        selector.select();
        return !terminated.get();
    }
}
