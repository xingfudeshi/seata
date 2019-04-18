package io.seata.discovery.registery.etcd;


import io.seata.discovery.registry.RegistryService;
import mousio.etcd4j.EtcdClient;
import mousio.etcd4j.promises.EtcdResponsePromise;
import mousio.etcd4j.requests.EtcdKeyGetRequest;
import mousio.etcd4j.responses.EtcdKeysResponse;

import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @author xingfudeshi@gmail.com
 * @date 2019/04/18
 */
public class EtcdRegistryServiceImpl implements RegistryService {
    private static volatile EtcdRegistryServiceImpl instance;
    private static volatile EtcdClient etcdClient;

    private EtcdRegistryServiceImpl() {
    }

    /**
     * get etcd registry service instance
     *
     * @return instance
     */
    static EtcdRegistryServiceImpl getInstance() {
        if (null == instance) {
            synchronized (EtcdRegistryServiceImpl.class) {
                if (null == instance) {
                    instance = new EtcdRegistryServiceImpl();
                }
            }
        }
        return instance;
    }


    @Override
    public void register(InetSocketAddress address) throws Exception {

    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {

    }

    @Override
    public void subscribe(String cluster, Object listener) throws Exception {

    }

    @Override
    public void unsubscribe(String cluster, Object listener) throws Exception {

    }

    @Override
    public List<InetSocketAddress> lookup(String key) throws Exception {
        return null;
    }

    @Override
    public void close() throws Exception {

    }

    /**
     * get client
     *
     * @return client
     */
    private EtcdClient getEtcdClient() {
        if (null == etcdClient) {
            synchronized (EtcdRegistryServiceImpl.class) {
                if (null == etcdClient) {
                    etcdClient = new EtcdClient(URI.create("http://localhost:2379"));
                }
            }

        }
        return etcdClient;

    }

    public static void main(String[] args) {
        EtcdClient etcdClient = new EtcdClient(URI.create("http://localhost:2379"));
        try {
            String serviceNamePrefix = "seata/registry/";
            String cluster = "default3";
            String finalKey = serviceNamePrefix + cluster;
            /*
            EtcdKeysResponse response = etcdClient.put("foo", "bar").send().get();
            // Prints out: bar
            System.out.println(response.node.value);

             */
            etcdClient.putDir(finalKey).ttl(10).send();


            //EtcdResponsePromise<EtcdKeysResponse> response1 = etcdClient.getAll().recursive().send();
            //System.out.println(response1.get().getNode());

        } catch (Exception e) {
            e.printStackTrace();
        }


    }
}
