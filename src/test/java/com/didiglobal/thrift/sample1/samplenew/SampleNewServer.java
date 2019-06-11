package com.didiglobal.thrift.sample1.samplenew;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SampleNewServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleNewServer.class);

    private TServer server;

    public void start(int port) {
        try {
            TServerSocket serverTransport = new TServerSocket(port);
            Sample.Processor processor = new Sample.Processor(new SampleService());
            TBinaryProtocol.Factory protFactory = new TBinaryProtocol.Factory();
            TThreadPoolServer.Args args = new TThreadPoolServer.Args(serverTransport);
            args.processor(processor);
            args.protocolFactory(protFactory);

            server = new TThreadPoolServer(args);
            LOGGER.info("Starting server on port " + port + " ...");
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

    static class SampleService implements Sample.Iface {
        int index = 0;

        @Override
        public Items getItems() {
            index++;
            LOGGER.info("server response {}", index);
            Items items = new Items();
            for (int i = 0; i < 5; i++) {
                Item item = new Item();
                item.name = "name " + i;
                item.image = "image " + i;
                item.contents = new ArrayList<>();
                for (int j = 0; j < i + 1; j++) {
                    item.contents.add("content " + i + " " + j);
                }
                items.addToItems(item);
            }

            return items;
        }
    }
}