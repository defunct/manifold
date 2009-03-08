package com.goodworkalan.manifold;

import java.nio.ByteBuffer;

// TODO Document.
public class Read implements Runnable
{
    // TODO Document.
    private final Conversation conversation;

    // TODO Document.
    private final ByteBuffer data;

    // TODO Document.
    public Read(Conversation conversation, ByteBuffer data)
    {
        this.conversation = conversation;
        this.data = data;
    }
    
    // TODO Document.
    public void run()
    {
        for (ByteBuffer unwapped : conversation.wrapper.unwrap(data, conversation))
        {
            conversation.session.read(unwapped, conversation);
        }
    }
}
