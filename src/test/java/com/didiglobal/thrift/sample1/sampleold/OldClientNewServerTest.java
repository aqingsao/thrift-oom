package com.didiglobal.thrift.sample1.sampleold;

import com.didiglobal.thrift.sample1.SampleServer;
import com.didiglobal.thrift.sample1.SampleWorkers;
import com.didiglobal.thrift.sample1.samplenew.SampleNewServer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TType;
import org.apache.thrift.transport.TTransport;
import org.junit.AfterClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OldClientNewServerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(OldClientNewServerTest.class);

    private static SampleServer sampleServer;
    private static int port = 8111;

    @AfterClass
    public static void afterAll() {
        if (sampleServer != null) {
            sampleServer.stop();
        }
        LOGGER.info("After all: close thrift server");
    }

    @Test
    public void oldclient_should_oom_at_concurrency_10() {
        sampleServer = new SampleNewServer(port).start();
        sleepInSeconds(3);

        int concurrency = 1;
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
     * try {
     * result.read(iprot_);
     * } finally {
     * TTransport transport = iprot_.getTransport();
     * if (transport instanceof TSocket) {
     * ((TSocket) transport).safeClearInputstream();
     * }
     * }
     * 4. 进入TSocket类，添加方法
     * public void safeClearInputstream() {
     * try {
     * int len = this.inputStream_.available();
     * this.inputStream_.skip(len);
     * } catch (IOException e) {
     * e.printStackTrace();
     * }
     * }
     */

    @Test
    public void oldclient_should_oom_if_use_TFramedTransport_at_concurrency_10() {
        sampleServer = new SampleNewServer(port).withTFramedTransport(true).start();
        sleepInSeconds(3);

        int concurrency = 1;
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
        }.withTFramedTransport(true).startAll();
    }

    private void sleepInSeconds(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        byte type = TType.STRING; // 字节0：Item第一个字段(name)的类型，String，值为11
        short id = 1;             // 字节1和2：Item第一个字段(name)的位置，值为1
        int size = 6;             // 字节3：Item第一个字段(name)的尺寸，值为6
        byte[] bytes = new byte[4];
        bytes[0] = type;
        bytes[1] = (byte) ((id >> 8) & 0xff); // big endian
        bytes[2] = (byte) ((id >> 0) & 0xff);
        bytes[3] = (byte) ((size >> 24) & 0xff);

        int value = (((bytes[0] & 0xff) << 24)
                | ((bytes[1] & 0xff) << 16)
                | ((bytes[2] & 0xff) << 8) | ((bytes[3] & 0xff) << 0));// big endian
        System.out.println(value);  // print 184549632
    }
}
