package com.didiglobal.thrift.sample1.samplenew;

import com.didiglobal.thrift.sample1.SampleServer;
import com.didiglobal.thrift.sample1.SampleWorkers;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NewClientNewServerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(NewClientNewServerTest.class);
    private static SampleServer sampleServer;
    private static int port = 8111;

    @BeforeClass
    public static void beforeAll() throws InterruptedException {
//        sampleServer = new SampleServer(port) {
//            @Override
//            protected TBaseProcessor createProcessor() {
//                return new Sample.Processor(id -> {
//                    LOGGER.info("server receives {}", id);
//                    Items items = new Items();
//                    items.setId(id);
//                    for (int i = 0; i < 5; i++) {
//                        Item item = new Item();
//                        item.name = "name " + i;
//                        item.image = "image " + i;
//                        item.contents = new ArrayList<>();
//                        for (int j = 0; j < 5; j++) {
//                            item.contents.add("content " + i + " " + j);
//                        }
//                        items.addToItems(item);
//                    }
//
//                    return items;
//                });
//            }
//        }.start();
        Thread.sleep(3 * 1000);
    }

    @AfterClass
    public static void afterAll() {
        if (sampleServer != null) {
            sampleServer.stop();
        }
        LOGGER.info("After all: close thrift server");
    }

    @Test
    public void newclient_getItems_should_not_oom_at_concurrency_10() {
        int concurrency = 2;
        int requestPerWorker = 500;
        new SampleWorkers<Sample.Client>("172.24.28.9", port, concurrency, requestPerWorker) {
            @Override
            protected Sample.Client createClient(TTransport transport) {
                TProtocol protocol = new TBinaryProtocol(transport);
                return new Sample.Client(protocol);
            }

            @Override
            protected void sendRequest(Sample.Client client, long seq) throws Exception {
                client.getItems(seq);
            }
        }.startAll();
    }

}
