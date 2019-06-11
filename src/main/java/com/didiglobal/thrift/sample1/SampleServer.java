package com.didiglobal.thrift.sample1;

import org.apache.thrift.TBaseProcessor;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class SampleServer {
    protected static final Logger LOGGER = LoggerFactory.getLogger(SampleServer.class);

    private TServer server;
    private int port;

    public SampleServer(int port) {
        this.port = port;
    }

    protected abstract TBaseProcessor createProcessor();

    public SampleServer start() {
        new Thread(() -> {
            try {
                TServerSocket serverTransport = new TServerSocket(port);
                TBinaryProtocol.Factory protFactory = new TBinaryProtocol.Factory();
                TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverTransport);
                args.processor(createProcessor());
                args.protocolFactory(protFactory);
                args.executorService(aCustomExecutorService());

                server = new TThreadPoolServer(args);
                LOGGER.info("Starting server on port " + port + " ...");
                server.serve();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }, "server").start();
        return this;
    }

    private ExecutorService aCustomExecutorService() {
        CustomThreadFactory threadFactory = new CustomThreadFactory("server");
        new ThreadPoolExecutor()
        return Executors.newScheduledThreadPool(1, threadFactory);

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