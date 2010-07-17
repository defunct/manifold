package com.goodworkalan.manifold;

public class Operation implements Runnable {
    private final Conversation conversation;

    public Operation(Conversation conversation) {
        this.conversation = conversation;
    }

    public void run() {
        boolean close = false;
        boolean write = false;
        synchronized (conversation) {
            boolean closed = conversation.closed;
            Runnable runnable = conversation.operations.poll();
            if (runnable != null) {
                runnable.run();
            }
            write = !conversation.out.isEmpty();
            if (closed != conversation.closed) {
                conversation.operations.clear();
                close = true;
            }
        }
        if (write) {
            conversation.plenum.send(conversation);
        } else if (close) {
            conversation.plenum.close(conversation);
        }
    }
}
