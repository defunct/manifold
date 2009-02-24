package com.goodworkalan.manifold;


public class ChangeOps
{
    public final Conversation conversation;
    
    public final int ops;
    
    public ChangeOps(Conversation conversation, int ops)
    {
        this.conversation = conversation;
        this.ops = ops;
    }
}
