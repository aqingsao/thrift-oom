package com.didiglobal.thrift.sample1.sampleold;

import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.PooledObjectFactory;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.thrift.transport.TTransport;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class TransportFactory<T extends TTransport> implements PooledObjectFactory<T> {

    private final AtomicInteger num = new AtomicInteger(0);

    private final List<String> hosts;

    private final int port;

    private final int timeout;

    private final Generator<T> generator;

    public TransportFactory(List<String> hosts, int port, int timeout, Generator<T> generator) {
        if (hosts == null) {
            throw new NullPointerException("hosts is null");
        }
        if (hosts.isEmpty()) {
            throw new IllegalArgumentException("hosts is empty");
        }
        if (generator == null) {
            throw new NullPointerException("generator is null");
        }
        this.hosts = hosts;
        this.port = port;
        this.timeout = timeout;
        this.generator = generator;
    }

    @Override
    public PooledObject<T> makeObject() throws Exception {
        int size = this.hosts.size();
        String host = (size == 1) ? this.hosts.get(0) : this.hosts.get(this.num.getAndIncrement() % size);
        T transport = this.generator.gen(host, this.port, this.timeout);
        transport.open();
        return new DefaultPooledObject<>(transport);
    }

    @Override
    public void destroyObject(PooledObject<T> pooledObject) throws Exception {
        pooledObject.getObject().close();
    }

    @Override
    public boolean validateObject(PooledObject<T> pooledObject) {
        return pooledObject.getObject().isOpen();
    }

    @Override
    public void activateObject(PooledObject<T> pooledObject) throws Exception {
    }

    @Override
    public void passivateObject(PooledObject<T> pooledObject) throws Exception {
    }

    public interface Generator<T extends TTransport> {
        T gen(String host, int port, int timeout);
    }
}
