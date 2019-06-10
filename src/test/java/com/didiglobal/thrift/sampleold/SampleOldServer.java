package com.didiglobal.thrift.sampleold;

import com.didiglobal.thrift.samplenew.SampleNewServer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SampleOldServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleNewServer.class);

    private TServer server;

    public void start(int port) {
        try {
            TServerSocket serverTransport = new TServerSocket(port);
            Sample.Processor processor = new Sample.Processor(new SampleService());
            TBinaryProtocol.Factory protFactory = new TBinaryProtocol.Factory(true, true);
            TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverTransport);
            args.processor(processor);
            args.protocolFactory(protFactory);

            server = new TThreadPoolServer(args);
            System.out.println("Starting server on port " + port + " ...");
            server.serve();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop();
        }
    }

    public static void main(String[] args) {
        new SampleOldServer().start(8111);
    }

    static class SampleService implements com.didiglobal.thrift.sampleold.Sample.Iface {
        @Override
        public Item getItem() {
            LOGGER.info("request received");
            Item item = new Item();
            item.name = "name";
            item.contents = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                item.contents.add("content " + j);
            }

            return item;
        }
    }
}