package com.didiglobal.thrift.sample1.sampleold;

import com.didiglobal.thrift.sample1.samplenew.SampleNewServer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;

public class OldClientNewServerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OldClientNewServerTest.class);

    private static SampleNewServer sampleNewServer;
    private static int port = 8111;

    @BeforeClass
    public static void beforeAll() throws InterruptedException {
        Runnable runnable = () -> {
            LOGGER.info("Before all: start thrift server on port " + port);
            sampleNewServer = new SampleNewServer();
            sampleNewServer.start(port);
        };
        Thread t = new Thread(runnable, "server");
        t.start();
        Thread.sleep(3 * 1000);
    }

    @AfterClass
    public static void afterAll() {
        try {
            Thread.sleep(3600 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (sampleNewServer != null) {
            sampleNewServer.stop();
        }
        LOGGER.info("After all: close thrift server");
    }

    @Test
    public void oldclient_should_oom_at_concurrency_10() {
        int concurrency = 10;
        CountDownLatch latch = new CountDownLatch(concurrency);
        for (int i = 0; i < concurrency; i++) {
            new Worker(i, 5, latch).run();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    @Test
    public void oldclient_should_not_oom_with_strictRead_true_at_concurrency_10() {
        int concurrency = 100;
        CountDownLatch latch = new CountDownLatch(concurrency);
        for (int i = 0; i < concurrency; i++) {
            new Worker(i, 15000, latch, true).run();
        }
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }


    class Worker {
        private String name;
        private int count;
        private CountDownLatch latch;
        private boolean strictRead;

        public Worker(int index, int count, CountDownLatch latch) {
            this(index, count, latch, false);
        }

        public Worker(int index, int count, CountDownLatch latch, boolean strictRead) {
            this.name = "worker " + index;
            this.count = count;
            this.latch = latch;
            this.strictRead = strictRead;
        }

        public void run() {
            new Thread(() -> {
                Sample.Client client = aClient(port, strictRead);
                for (int i = 0; i < count; i++) {
                    try {
                        LOGGER.info(name + " sent " + i);
                        client.getItems();
                        LOGGER.info(name + " received " + i);
                    } catch (TException e) {
                        LOGGER.error(name + " error " + i + ": " + e.getMessage());
                    }
                }
                latch.countDown();
            }, this.name).start();
        }

        private Sample.Client aClient(int port, boolean strictRead) {
            try {
                TTransport transport = new TSocket("127.0.0.1", port);
                transport.open();

                TProtocol protocol = new TBinaryProtocol(transport, strictRead, true);
                return new Sample.Client(protocol);
            } catch (TTransportException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }
}