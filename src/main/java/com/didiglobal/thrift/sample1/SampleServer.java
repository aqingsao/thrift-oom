package com.didiglobal.thrift.sample1;

import org.apache.thrift.TBaseProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.THsHaServer;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TNonblockingServerSocket;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SampleServer {
    protected static final Logger LOGGER = LoggerFactory.getLogger(SampleServer.class);

    private TServer server;
    private int port;
    private boolean tFramedTransport;

    public SampleServer(int port) {
        this.port = port;
    }

    public SampleServer start() {
        new Thread(() -> {
            try {
                if (tFramedTransport) {
                    server = aNonblockingServer(port);
                } else {
                    server = aSocketServer(port);
                }
                LOGGER.info("Starting server on port " + port + " ...");
                server.serve();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }, "server").start();
        return this;
    }

    private TThreadPoolServer aSocketServer(int port) throws TTransportException {
        TServerSocket serverTransport = new TServerSocket(port);
        TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverTransport);
        args.processor(aCustomizedProcessor());
        args.protocolFactory(new TBinaryProtocol.Factory());
        args.executorService(aCustomizedExecutorService());

        return new TThreadPoolServer(args);
    }

    private TServer aNonblockingServer(int port) throws TTransportException {
        TNonblockingServerSocket serverSocket = new TNonblockingServerSocket(port);
        THsHaServer.Args args = new THsHaServer.Args(serverSocket);
        args.processor(aCustomizedProcessor());
        args.protocolFactory(new TBinaryProtocol.Factory());
        args.executorService(aCustomizedExecutorService());

        return new THsHaServer(args);
    }

    private ExecutorService aCustomizedExecutorService() {
        SynchronousQueue<Runnable> executorQueue = new SynchronousQueue<>();
        CustomThreadFactory threadFactory = new CustomThreadFactory("server");
        return new ThreadPoolExecutor(5, 100, 60, TimeUnit.SECONDS, executorQueue, threadFactory);
    }

    protected abstract TBaseProcessor aCustomizedProcessor();

    public SampleServer withTFramedTransport(boolean tFramedTransport) {
        this.tFramedTransport = tFramedTransport;
        return this;
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    static class CustomThreadFactory implements ThreadFactory {
        private final AtomicInteger poolNumber = new AtomicInteger(1);
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;
        private final ThreadGroup group;

        public CustomThreadFactory(String poolName) {
            namePrefix = poolName + "-" + poolNumber.getAndIncrement() + "-";
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement());
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            if (t.getPriority() != Thread.NORM_PRIORITY) {
                t.setPriority(Thread.NORM_PRIORITY);
            }
            return t;
        }
    }
}