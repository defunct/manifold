package com.goodworkalan.manifold;

public class Close implements Runnable
{
    private final Conversation conversation;
    
    public Close(Conversation conversation)
    {
        this.conversation = conversation;
    }
    
    public void run()
    {
        conversation.session.close();
    }
}
