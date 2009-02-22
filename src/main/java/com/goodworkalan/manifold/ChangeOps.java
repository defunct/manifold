package com.goodworkalan.manifold;

import java.nio.channels.SocketChannel;

public class ChangeOps
{
    public final SocketChannel socketChannel;
    
    public final int ops;
    
    public ChangeOps(SocketChannel socketChannel, int ops)
    {
        this.socketChannel = socketChannel;
        this.ops = ops;
    }
}
