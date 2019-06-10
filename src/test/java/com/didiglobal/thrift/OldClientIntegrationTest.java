package com.didiglobal.thrift;

import com.didiglobal.thrift.samplenew.SampleNewServer;
import com.didiglobal.thrift.sampleold.Sample;
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

public class OldClientIntegrationTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OldClientIntegrationTest.class);

    private static SampleNewServer sampleNewServer;
    private static int port = 8111;

    @BeforeClass
    public static void beforeAll() throws InterruptedException {
        Runnable runnable = () -> {
            LOGGER.info("Before all: start thrift server on port " + port);
            sampleNewServer = new SampleNewServer();
            sampleNewServer.start(port);
        };
        Thread t = new Thread(runnable);
        t.start();
        Thread.sleep(3 * 1000);
    }

    @AfterClass
    public static void afterAll() {
        if (sampleNewServer != null) {
            sampleNewServer.stop();
        }
        LOGGER.info("After all: close thrift server");
    }

    @Test
    public void getCards_should_oom_at_concurrency_10() {
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
    public void gradually_add_worker() {
        int maxWorkers = 100;
        CountDownLatch latch = new CountDownLatch(maxWorkers);
        for (int i = 0; i < maxWorkers; i++) {
            new Worker(i, Integer.MAX_VALUE, latch).run();
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
            }
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

        public Worker(int index, int count, CountDownLatch latch) {
            this.name = "worker " + index;
            this.count = count;
            this.latch = latch;
        }

        public void run() {
            new Thread(() -> {
                Sample.Client client = aClient(port);
                for (int i = 0; i < count; i++) {
                    try {
                        LOGGER.info(name + " sent " + i);
                        client.getItem();
                        LOGGER.info(name + " received " + i);
                    } catch (TException e) {
                        LOGGER.error(name + " error " + i + ": " + e.getMessage());
                    }
                }
                latch.countDown();
            }).start();
        }

        private Sample.Client aClient(int port) {
            try {
                TTransport transport = new TSocket("127.0.0.1", port);
                transport.open();

                TProtocol protocol = new TBinaryProtocol(transport, true, true);
                return new Sample.Client(protocol);
            } catch (TTransportException e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
    }
}
