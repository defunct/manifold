package com.goodworkalan.manifold;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A selector socket thread manager that reads and writes to a server sockets
 * and delegates data processing to a fixed array of worker threads.
 * 
 * @author Alan Gutierrez
 */
public class Manifold
{
    private final CountDownLatch running;
    
    private final ServerSocketChannel serverSocketChannel;
    
    private final Selector selector;
    
    private final ByteBuffer in;
    
    private final Map<SocketChannel, Conversation> conversations;
    
    private final Queue<ChangeOps> opChanges;
    
    private final Queue<Conversation> closing;
    
    private final ExecutorService executorService;
    
    private final SessionFactory sessionFactory;
    
    private final AtomicBoolean terminated;
    
    public Manifold(SessionFactory sessionFactory, ExecutorService executorService) throws IOException
    {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.configureBlocking(false);
        InetSocketAddress socketAddress = new InetSocketAddress("localhost", 8143);
        serverSocketChannel.socket().bind(socketAddress);
        Selector selector = SelectorProvider.provider().openSelector();
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        this.selector = selector;
        this.in = ByteBuffer.allocateDirect(1024 * 1024);
        this.opChanges = new ConcurrentLinkedQueue<ChangeOps>();
        this.closing = new ConcurrentLinkedQueue<Conversation>();
        this.conversations = new ConcurrentHashMap<SocketChannel, Conversation>();
        this.executorService = executorService;
        this.sessionFactory = sessionFactory;
        this.terminated = new AtomicBoolean();
        this.running = new CountDownLatch(1);
    }
    
    void terminate(Conversation conversation)
    {
        synchronized (conversation)
        {
            conversation.operations.add(new Terminate(conversation));
        }
        executorService.execute(new Operation(conversation));
    }

    void close(Conversation conversation)
    {
        closing.add(conversation);
        selector.wakeup();
    }

    void listen(Conversation conversation)
    {
        ChangeOps changeOps = new ChangeOps(conversation.socketChannel, SelectionKey.OP_READ);
        opChanges.add(changeOps);
        selector.wakeup();
    }
    
    void send(Conversation conversation)
    {
        ChangeOps changeOps = new ChangeOps(conversation.socketChannel, SelectionKey.OP_WRITE);
        synchronized (opChanges)
        {
            opChanges.add(changeOps);
        }
        selector.wakeup();
    }

    private void accept(SelectionKey key) throws IOException
    {
        // For an accept to be pending the channel must be a server socket channel.
        ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

        // Accept the connection and make it non-blocking
        SocketChannel socketChannel = serverSocketChannel.accept();
        
        Session session = sessionFactory.accept(socketChannel.socket().getInetAddress());
        if (session == null)
        {
            socketChannel.close();
        }
        else
        {
            socketChannel.configureBlocking(false);
            
            Conversation conversation = new Conversation(this, socketChannel, session);
            conversation.operations.add(new Accepted(conversation));
            conversations.put(socketChannel, conversation);
    
            executorService.execute(new Operation(conversation));
        }
    }
    
    private void read(SelectionKey key) throws IOException
    {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        in.clear();
        int read = -1;
        try
        {
            read = socketChannel.read(in);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        
        Conversation conversation = conversations.get(socketChannel);
        if (read == -1)
        {
            // Either read returned -1 or else it threw an I/O exception. Either
            // way we are going to close the socket and cancel the key.
            close(conversation);
        }
        else if (key.isValid())
        {
            byte[] data = new byte[read];
            
            in.flip();
            in.get(data);
    
            synchronized (conversation.operations)
            {
                conversation.operations.add(new Read(conversation, ByteBuffer.wrap(data)));
            }
            
            executorService.execute(new Operation(conversation));
        }
    }
    
    private void write(SelectionKey key) throws IOException
    {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        Conversation conversation = conversations.get(socketChannel);
        boolean close = false;
        synchronized (conversation)
        {
            Queue<ByteBuffer> queue = conversation.out;
            
            ByteBuffer data = null;
            // Write until the queue is empty.
            while ((data = queue.peek()) != null)
            {
                socketChannel.write(data);
                if (data.remaining() != 0)
                {
                    // The socket buffer has filled up so try again later.
                    break;
                }
                queue.remove();
            }
            if (queue.isEmpty())
            {
                key.interestOps(SelectionKey.OP_READ);
            }
            close = conversation.closed;
        }
        if (close)
        {
            close(conversation);
        }
    }

    public void bind() throws IOException
    {
        running.countDown();
        bind(new NormalSelection(selector, terminated));
        serverSocketChannel.keyFor(selector).cancel();
        for (Conversation conversation : conversations.values())
        {
            terminate(conversation);
        }
        selector.wakeup();
        bind(new ShutdownSelection(selector, conversations));
        bind(new PurgeSelection(selector));
        serverSocketChannel.close();
    }
    
    public void waitForStartup()
    {
        try
        {
            running.await();
        }
        catch (InterruptedException e)
        {
        }
    }
    
    private void bind(Selection selection) throws IOException
    {
        while (selection.select())
        {
            ChangeOps changeOps;
            while ((changeOps = opChanges.poll()) != null)
            {
                Conversation conversation = conversations.get(changeOps.socketChannel);
                if (conversation != null)
                {
                    if (conversation.registered)
                    {
                        changeOps.socketChannel.keyFor(selector).interestOps(changeOps.ops);
                    }
                    else
                    {
                        changeOps.socketChannel.register(selector, changeOps.ops);
                        conversation.registered = true; 
                    }
                }
            }
            
            Conversation close;
            while ((close = closing.poll()) != null)
            {
                synchronized (close.operations)
                {
                    close.operations.clear();
                    close.operations.add(new Close(close));
                }
                if (close.registered)
                {
                    close.socketChannel.keyFor(selector).cancel();
                }
                close.socketChannel.close();
                executorService.execute(new Operation(close));
                conversations.remove(close.socketChannel);
            }
    
            Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
            while (selectedKeys.hasNext())
            {
                SelectionKey key = selectedKeys.next();
                selectedKeys.remove();
                
                if (!key.isValid())
                {
                    continue;
                }
                
                if (key.isAcceptable())
                {
                    accept(key);
                }
                else if (key.isReadable())
                {
                    read(key);
                }
                else if (key.isWritable())
                {
                    write(key);
                }
            }
        }
    }
    
    public void shutdown()
    {
        terminated.set(true);
        selector.wakeup();
    }
}
