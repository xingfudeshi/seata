package io.seata.discovery.registery.etcd;


import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KV;
import io.etcd.jetcd.Lease;
import io.etcd.jetcd.Watch;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.kv.PutResponse;
import io.etcd.jetcd.lease.LeaseGrantResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.options.WatchOption;
import io.etcd.jetcd.watch.WatchResponse;
import io.netty.util.CharsetUtil;
import io.seata.common.util.NetUtil;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.discovery.registry.RegistryService;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static io.netty.util.CharsetUtil.UTF_8;

/**
 * @author xingfudeshi@gmail.com
 * @date 2019/04/18
 */
public class EtcdRegistryServiceImpl implements RegistryService {
    private static volatile EtcdRegistryServiceImpl instance;
    private static volatile Client client;

    private static final Configuration FILE_CONFIG = ConfigurationFactory.FILE_INSTANCE;
    private static final String FILE_ROOT_REGISTRY = "registry";
    private static final String FILE_CONFIG_SPLIT_CHAR = ".";
    private static final String REGISTRY_TYPE = "etcd3";
    private static final String SERVER_ADDR_KEY = "serverAddr";
    private static final String REGISTRY_CLUSTER = "cluster";
    private static final String DEFAULT_CLUSTER_NAME = "default";
    private static final String REGISTRY_KEY_PREFIX = "registry-seata-";
    private static final String FILE_CONFIG_KEY_PREFIX = FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR;

    private static ConcurrentMap<String, List<InetSocketAddress>> clusterAddressMap = null;
    private static final int MAP_INITIAL_CAPACITY = 8;

    private static final long TTL = 60;
    private static long leaseId = 0;

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
                    clusterAddressMap = new ConcurrentHashMap<>(MAP_INITIAL_CAPACITY);
                    instance = new EtcdRegistryServiceImpl();
                }
            }
        }
        return instance;
    }


    @Override
    public void register(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);
        doRegister(address);
    }

    /**
     * do registry
     *
     * @param address
     */
    private void doRegister(InetSocketAddress address) throws Exception {
        KV kvClient = getClient().getKVClient();
        PutOption putOption = PutOption.newBuilder().withLeaseId(getLeaseId()).build();
        kvClient.put(buildRegestryKey(address), buildRegistryValue(address), putOption);
    }

    /**
     * create a new lease id or refresh a existing lease id
     */
    private long getLeaseId() throws Exception {
        Lease leaseClient = getClient().getLeaseClient();
        if (0 == leaseId) {
            //create a new lease
            leaseId = leaseClient.grant(TTL).get().getID();
        } else {
            //refresh a existing lease
            leaseClient.keepAliveOnce(leaseId);
        }
        // TODO 需要一个线程定时刷新lease id
        return leaseId;
    }

    /**
     * build registry key
     *
     * @return registry key
     */
    private ByteSequence buildRegestryKey(InetSocketAddress address) {
        return ByteSequence.from(REGISTRY_KEY_PREFIX + getClusterName() + "-" + NetUtil.toStringAddress(address), UTF_8);
    }

    /**
     * build registry value
     * @param address
     * @return registry value
     */
    private ByteSequence buildRegistryValue(InetSocketAddress address) {
        return ByteSequence.from(NetUtil.toStringAddress(address), UTF_8);
    }

    @Override
    public void unregister(InetSocketAddress address) throws Exception {
        NetUtil.validAddress(address);

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
    private Client getClient() {
        if (null == client) {
            synchronized (EtcdRegistryServiceImpl.class) {
                if (null == client) {
                    client = Client.builder().endpoints("http://localhost:2379").build();
                }
            }

        }
        return client;

    }

    /**
     * get service group
     *
     * @param key
     * @return clusterNameKey
     */
    private String getServiceGroup(String key) {
        String clusterNameKey = PREFIX_SERVICE_ROOT + CONFIG_SPLIT_CHAR + PREFIX_SERVICE_MAPPING + key;
        return ConfigurationFactory.getInstance().getConfig(clusterNameKey);
    }

    /**
     * get cluster name
     *
     * @return
     */
    private String getClusterName() {
        String clusterConfigName = FILE_ROOT_REGISTRY + FILE_CONFIG_SPLIT_CHAR + REGISTRY_TYPE + FILE_CONFIG_SPLIT_CHAR
            + REGISTRY_CLUSTER;
        return FILE_CONFIG.getConfig(clusterConfigName, DEFAULT_CLUSTER_NAME);
    }
    /*
    public static void main(String[] args) {
        Client client = Client.builder().endpoints("http://localhost:2379").build();
        KV kvClient = client.getKVClient();
        Lease leaseClient = client.getLeaseClient();
        try {
            new Thread(() -> {
                Watch watchClient = client.getWatchClient();
                WatchOption watchOption = WatchOption.newBuilder().withPrefix(ByteSequence.from("name", CharsetUtil.UTF_8)).build();
                watchClient.watch(ByteSequence.from("name", CharsetUtil.UTF_8), watchOption, new Watch.Listener() {

                    @Override
                    public void onNext(WatchResponse watchResponse) {
                        System.out.println("watch response:" + watchResponse);
                        System.out.println("-------------------");

                    }

                    @Override
                    public void onError(Throwable throwable) {

                    }

                    @Override
                    public void onCompleted() {
                        System.out.println("watch completed");
                    }
                });

            }).start();
            String serviceNamePrefix = "seata-registry-";
            String cluster = "-default";
            String finalKey = serviceNamePrefix + cluster;
            LeaseGrantResponse leaseGrantResponse = leaseClient.grant(15).get();
            System.out.println("====>" + leaseGrantResponse.getID());


            for (int i = 0; i < 3; i++) {
                //创建三个seata server实例(模拟三个实例)
                ByteSequence key = ByteSequence.from("name_" + i, CharsetUtil.UTF_8);
                ByteSequence value = ByteSequence.from("test", CharsetUtil.UTF_8);
                PutOption option = PutOption.newBuilder().withLeaseId(leaseGrantResponse.getID()).build();
                PutResponse response = kvClient.put(key, value, option).get();
                //System.out.println(response.toString());
            }

            GetOption getOption = GetOption.newBuilder().withPrefix(ByteSequence.from("name", CharsetUtil.UTF_8)).build();
            GetResponse getResponse = kvClient.get(ByteSequence.from("name", CharsetUtil.UTF_8), getOption).get();
            //System.out.println(getResponse.toString());


            //EtcdResponsePromise<EtcdKeysResponse> response1 = etcdClient.getAll().recursive().send();
            //System.out.println(response1.get().getNode());

        } catch (Exception e) {
            e.printStackTrace();
        }


    }

     */
}
