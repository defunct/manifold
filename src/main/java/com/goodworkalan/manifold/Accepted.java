package com.goodworkalan.manifold;

public class Accepted implements Runnable
{
    private final Conversation conversation;
    
    public Accepted(Conversation conversation)
    {
        this.conversation = conversation;
    }
    
    public void run()
    {
        conversation.session.accepted(conversation);
        if (!conversation.closed && conversation.out.isEmpty())
        {
            conversation.plenum.listen(conversation);
        }
    }
}
