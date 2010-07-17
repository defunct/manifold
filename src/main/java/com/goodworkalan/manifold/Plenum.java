package com.goodworkalan.manifold;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/** This will the thread for the read/write sockets. */
// TODO Document.
public class Plenum implements Runnable {
    // TODO Document.
    private final ExecutorService executorService;

    // TODO Document.
    private final Selector selector;

    // TODO Document.
    private final ByteBuffer in;

    // TODO Document.
    private final Queue<Conversation> accepting;

    // TODO Document.
    private final Queue<ChangeOps> opChanges;

    // TODO Document.
    private final Queue<Conversation> closing;

    // TODO Document.
    private final AtomicInteger average;

    // TODO Document.
    private final AtomicBoolean shutdown;

    // TODO Document.
    private final AtomicReference<Plenum> successor;

    // TODO Document.
    private int averageSum;

    // TODO Document.
    private int averageCount;

    // TODO Document.
    private long then;

    // TODO Document.
    public Plenum(ExecutorService executorService) throws IOException {
        this.executorService = executorService;
        this.selector = SelectorProvider.provider().openSelector();
        this.in = ByteBuffer.allocateDirect(1024 * 1024);
        this.average = new AtomicInteger();
        this.accepting = new ConcurrentLinkedQueue<Conversation>();
        this.opChanges = new ConcurrentLinkedQueue<ChangeOps>();
        this.closing = new ConcurrentLinkedQueue<Conversation>();
        this.shutdown = new AtomicBoolean();
        this.successor = new AtomicReference<Plenum>();
        this.then = System.currentTimeMillis() - 60000;
    }

    // TODO Document.
    public int getAverage() {
        return average.get();
    }

    // TODO Document.
    public void accept(Conversation conversation) {
        accepting.add(conversation);
        selector.wakeup();
    }

    // TODO Document.
    public void adopt(Conversation conversation, int ops) {
        ChangeOps changeOps = new ChangeOps(conversation, ops);
        opChanges.add(changeOps);
        selector.wakeup();
    }

    // TODO Document.
    public void handoff(Plenum plenum) {
        successor.set(plenum);
        shutdown.set(true);
        selector.wakeup();
    }

    // TODO Document.
    public void termiante() {
        shutdown.set(true);
        selector.wakeup();
    }

    // TODO Document.
    private void terminate() throws IOException {
        if (successor.get() != null) {
            for (SelectionKey key : selector.keys()) {
                if (key.isValid()) {
                    Conversation conversation = (Conversation) key.attachment();
                    synchronized (conversation) {
                        conversation.registered = false;
                    }
                    successor.get().adopt(conversation, key.interestOps());
                }
            }
            selector.close();
        } else {
            for (SelectionKey key : selector.keys()) {
                if (key.isValid()) {
                    Conversation conversation = (Conversation) key.attachment();
                    synchronized (conversation) {
                        conversation.operations
                                .add(new Terminate(conversation));
                    }
                    executorService.execute(new Operation(conversation));
                }
            }
        }

    }

    // TODO Document.
    public void run() {
        boolean terminated = false;
        for (;;) {
            try {
                selector.select();

                if (shutdown.get()) {
                    if (!terminated) {
                        terminate();
                        terminated = true;
                    }
                    if (selector.isOpen() && selector.keys().isEmpty()) {
                        selector.close();
                    }
                    if (!selector.isOpen()) {
                        break;
                    }
                }

                if (averageCount == 500) {
                    averageSum = 100 * (averageSum / averageCount);
                    averageCount = 100;
                }

                long now = System.currentTimeMillis();
                int elapsed = (int) (now - then);
                then = now;

                averageSum += elapsed;
                averageCount++;

                average.set(averageSum / averageCount);

                Conversation accept;
                while ((accept = accepting.poll()) != null) {
                    accept.socketChannel.configureBlocking(false);
                    accept.operations.add(new Accepted(accept));
                    executorService.execute(new Operation(accept));
                }

                ChangeOps changeOps;
                while ((changeOps = opChanges.poll()) != null) {
                    Conversation conversation = changeOps.conversation;
                    SocketChannel socketChannel = conversation.socketChannel;
                    if (conversation.registered) {
                        socketChannel.keyFor(selector).interestOps(
                                changeOps.ops);
                    } else {
                        SelectionKey key = socketChannel.register(selector,
                                changeOps.ops);
                        key.attach(conversation);
                        conversation.registered = true;
                    }
                }

                Conversation close;
                while ((close = closing.poll()) != null) {
                    synchronized (close.operations) {
                        close.operations.clear();
                        close.operations.add(new Close(close));
                    }
                    if (close.registered) {
                        close.socketChannel.keyFor(selector).cancel();
                    }
                    close.socketChannel.close();
                    executorService.execute(new Operation(close));
                }

                Iterator<SelectionKey> selectedKeys = selector.selectedKeys()
                        .iterator();
                while (selectedKeys.hasNext()) {
                    SelectionKey key = selectedKeys.next();
                    selectedKeys.remove();

                    if (key.isValid()) {
                        if (key.isReadable()) {
                            read(key);
                        } else if (key.isWritable()) {
                            write(key);
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // TODO Document.
    private void read(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        in.clear();
        int read = -1;
        try {
            read = socketChannel.read(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Conversation conversation = (Conversation) key.attachment();
        if (read == -1) {
            // Either read returned -1 or else it threw an I/O exception. Either
            // way we are going to close the socket and cancel the key.
            close(conversation);
        } else if (key.isValid()) {
            byte[] data = new byte[read];

            in.flip();
            in.get(data);

            synchronized (conversation.operations) {
                conversation.operations.add(new Read(conversation, ByteBuffer
                        .wrap(data)));
            }

            executorService.execute(new Operation(conversation));
        }
    }

    // TODO Document.
    private void write(SelectionKey key) throws IOException {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        Conversation conversation = (Conversation) key.attachment();
        boolean close = false;
        synchronized (conversation) {
            Queue<ByteBuffer> queue = conversation.out;

            ByteBuffer data = null;
            // Write until the queue is empty.
            while ((data = queue.peek()) != null) {
                socketChannel.write(data);
                if (data.remaining() != 0) {
                    // The socket buffer has filled up so try again later.
                    break;
                }
                queue.remove();
            }
            if (queue.isEmpty()) {
                key.interestOps(SelectionKey.OP_READ);
            }
            close = conversation.closed;
        }
        if (close) {
            close(conversation);
        }
    }

    // TODO Document.
    void close(Conversation conversation) {
        closing.add(conversation);
        selector.wakeup();
    }

    // TODO Document.
    void listen(Conversation conversation) {
        ChangeOps changeOps = new ChangeOps(conversation, SelectionKey.OP_READ);
        opChanges.add(changeOps);
        selector.wakeup();
    }

    // TODO Document.
    void send(Conversation conversation) {
        ChangeOps changeOps = new ChangeOps(conversation, SelectionKey.OP_WRITE);
        opChanges.add(changeOps);
        selector.wakeup();
    }
}
