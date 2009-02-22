package com.goodworkalan.manifold;

public class Terminate implements Runnable
{
    private final Conversation conversation;
    
    public Terminate(Conversation conversation)
    {
        this.conversation = conversation;
    }
    
    public void run()
    {
        conversation.session.terminate(conversation);
        conversation.close();
    }
}
