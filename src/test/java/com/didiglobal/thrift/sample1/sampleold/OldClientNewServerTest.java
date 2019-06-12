package com.didiglobal.thrift.sample1.sampleold;

import com.didiglobal.thrift.sample1.SampleServer;
import com.didiglobal.thrift.sample1.SampleWorkers;
import com.didiglobal.thrift.sample1.samplenew.SampleNewServer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TTransport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OldClientNewServerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OldClientNewServerTest.class);

    private static SampleServer sampleServer;
    private static int port = 8111;

    @BeforeClass
    public static void beforeAll() throws InterruptedException {
        sampleServer = new SampleNewServer(port).start();
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
    public void oldclient_should_oom_at_concurrency_10() {
        int concurrency = 10;
        int requestPerWorker = 50;
        new SampleWorkers<Sample.Client>("127.0.0.1", port, concurrency, requestPerWorker) {
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

    @Test
    public void oldclient_should_not_oom_if_strictRead_true_at_concurrency_10() {
        int concurrency = 10;
        int requestPerWorker = 50;
        new SampleWorkers<Sample.Client>("127.0.0.1", port, concurrency, requestPerWorker) {
            @Override
            protected Sample.Client createClient(TTransport transport) {
                TProtocol protocol = new TBinaryProtocol(transport, true, true);
                return new Sample.Client(protocol);
            }

            @Override
            protected void sendRequest(Sample.Client client, long seq) throws Exception {
                client.getItems(seq);
            }
        }.startAll();
    }

    /**
     * 执行该测试，需修改代码以清除输入流的缓存，具体位置和方式如下
     * 1. 下载Apache Thrift源文件到本地，https://thrift.apache.org/download
     * 2. 在IDEA中把${apache thrift}/lib/java导入为模块，并让thrift-oom模块依赖
     * 3.进入TServiceClient类的receiveBase()方法，把result.read(iprot_)修改为
     *    try {
     *        result.read(iprot_);
     *    } finally {
     *        TTransport transport = iprot_.getTransport();
     *        if (transport instanceof TSocket) {
     *            ((TSocket) transport).safeClearInputstream();
     *        }
     *    }
     * 4. 进入TSocket类，添加方法
     *    public void safeClearInputstream() {
     *        try {
     *            int len = this.inputStream_.available();
     *            this.inputStream_.skip(len);
     *        } catch (IOException e) {
     *            e.printStackTrace();
     *        }
     *    }
     */

    @Test
    public void oldclient_should_not_oom_if_clear_inputstream_at_concurrency_10() {
        int concurrency = 10;
        int requestPerWorker = 50;
        new SampleWorkers<Sample.Client>("127.0.0.1", port, concurrency, requestPerWorker) {
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
