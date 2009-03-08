package com.goodworkalan.manifold;

// TODO Document.
public class Terminate implements Runnable
{
    // TODO Document.
    private final Conversation conversation;
    
    // TODO Document.
    public Terminate(Conversation conversation)
    {
        this.conversation = conversation;
    }
    
    // TODO Document.
    public void run()
    {
        conversation.session.terminate(conversation);
        conversation.close();
    }
}
