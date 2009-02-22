package com.goodworkalan.manifold;

import java.net.InetAddress;

public interface SessionFactory
{
    public Session accept(InetAddress remote);
}
