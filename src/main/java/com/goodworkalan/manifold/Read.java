package com.goodworkalan.manifold;

import java.nio.ByteBuffer;

public class Read implements Runnable
{
    private final Conversation conversation;

    private final ByteBuffer data;

    public Read(Conversation conversation, ByteBuffer data)
    {
        this.conversation = conversation;
        this.data = data;
    }
    
    public void run()
    {
        conversation.session.read(data, conversation);
    }
}
