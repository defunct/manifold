package com.goodworkalan.manifold;

//TODO Document.
public class Accepted implements Runnable {
    // TODO Document.
    private final Conversation conversation;

    // TODO Document.
    public Accepted(Conversation conversation) {
        this.conversation = conversation;
    }

    // TODO Document.
    public void run() {
        conversation.session.accepted(conversation);
        if (!conversation.closed && conversation.out.isEmpty()) {
            conversation.plenum.listen(conversation);
        }
    }
}
