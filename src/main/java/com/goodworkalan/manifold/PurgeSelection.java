package com.goodworkalan.manifold;

import java.io.IOException;
import java.nio.channels.Selector;

// TODO Document.
public class PurgeSelection implements Selection {
    // TODO Document.
    private final Selector selector;

    // TODO Document.
    public PurgeSelection(Selector selector) {
        this.selector = selector;
    }

    // TODO Document.
    public boolean select() throws IOException {
        return selector.selectNow() != 0;
    }
}
