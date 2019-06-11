package com.didiglobal.thrift.sample1;

import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

public abstract class SampleWorkers<C extends org.apache.thrift.TServiceClient> {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleWorkers.class);
    private static final AtomicLong INDEX = new AtomicLong();
    private final String serverHost;
    private final int serverPort;
    private final int workerCount;
    private final int totalRequestCount;

    public SampleWorkers(String serverHost, int serverPort, int workerCount, int totalRequestCount) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.workerCount = workerCount;
        this.totalRequestCount = totalRequestCount;
    }

    protected C aClient(String serverHost, int serverPort) {
        try {
            TTransport transport = new TSocket(serverHost, serverPort);
            transport.open();

            return createClient(transport);
        } catch (TTransportException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void startAll() {
        CountDownLatch latch = new CountDownLatch(workerCount);
        int requestPerWorker = totalRequestCount / workerCount;
        int remainder = totalRequestCount % workerCount;

        for (int i = 0; i < workerCount; i++) {
            int requestOfThisWorker = i < remainder ? requestPerWorker + 1 : requestPerWorker;
            Thread t = new Thread(() -> {
                C client = aClient(this.serverHost, this.serverPort);
                for (int j = 0; j < requestOfThisWorker; j++) {
                    long seq = INDEX.getAndIncrement();
                    try {
                        LOGGER.info("sent {}", seq);
                        sendRequest(client, seq);
                        LOGGER.info("received {}", seq);
                    } catch (Exception e) {
                        LOGGER.info("error received {}: {}", seq, e.getMessage());
                    }
                }
                latch.countDown();
            }, "worker-" + i);
            t.start();
        }

        try {
            latch.await();
        } catch (InterruptedException e) {
            LOGGER.error(e.getMessage(), e);
        }

    }

    protected abstract C createClient(TTransport transport);

    protected abstract void sendRequest(C client, long seq) throws Exception;
}
