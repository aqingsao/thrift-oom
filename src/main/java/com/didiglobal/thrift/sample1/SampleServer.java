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
        return Executors.newScheduledThreadPool(1, threadFactory);

    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    static class CustomThreadFactory implements ThreadFactory {
        private final String poolName;
        private final String namePrefix;
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        public CustomThreadFactory(String poolName) {
            this.poolName = poolName;
            namePrefix = this.poolName + "-" + poolNumber.getAndIncrement() +
                    "-";
        }

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, namePrefix + threadNumber.getAndIncrement());
        }
    }
}