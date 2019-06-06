package com.didiglobal.thrift.samplenew;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TServerSocket;

import java.util.ArrayList;

public class SampleNewServer {

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
        new SampleNewServer().start(8111);
    }

    static class SampleService implements Sample.Iface {

        @Override
        public CardsRespInfo getCards(CardsReqInfo cardsReqInfo) throws TException {
            System.out.println("request received");
            CardsRespInfo cardsRespInfo = new CardsRespInfo();
            cardsRespInfo.cards = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                cardsRespInfo.cards.add(aCardItem(i));
            }

            return cardsRespInfo;
        }

        private CardItem aCardItem(long i) {
            CardItem cardItem = new CardItem();
            cardItem.name = "name " + i;
            cardItem.image = "image " + i;
            cardItem.contents = new ArrayList<>();
            for (int j = 0; j < 5; j++) {
                cardItem.contents.add("content " + j);
            }
            return cardItem;
        }
    }
}