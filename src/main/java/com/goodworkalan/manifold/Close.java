package com.goodworkalan.manifold;

// TODO Document.
public class Close implements Runnable
{
    // TODO Document.
    private final Conversation conversation;
    
    // TODO Document.
    public Close(Conversation conversation)
    {
        this.conversation = conversation;
    }
    
    // TODO Document.
    public void run()
    {
        conversation.session.close();
    }
}
