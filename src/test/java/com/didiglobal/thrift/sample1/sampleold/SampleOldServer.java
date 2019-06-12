package com.didiglobal.thrift.sample1.sampleold;

import com.didiglobal.thrift.sample1.SampleServer;
import org.apache.thrift.TBaseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

public class SampleOldServer extends SampleServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(SampleOldServer.class);

    public SampleOldServer(int port) {
        super(port);
    }

    public static void main(String[] args) {
        new SampleOldServer(8111).start();
    }

    @Override
    protected TBaseProcessor createProcessor() {
        return new Sample.Processor(id -> {
            LOGGER.info("server receives {}", id);
            Items items = new Items();
            items.setId(id);
            for (int i = 0; i < 5; i++) {
                Item item = new Item();
                item.name = "name " + i;
                item.contents = new ArrayList<>();
                for (int j = 0; j < 5; j++) {
                    item.contents.add("content " + i + " " + j);
                }
                items.addToItems(item);
            }

            return items;
        });
    }
}
