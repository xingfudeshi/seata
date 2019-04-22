package io.seata.discovery.registery.etcd;

import io.seata.discovery.registry.RegistryProvider;
import io.seata.discovery.registry.RegistryService;

/**
 * @author xingfudeshi@gmail.com
 * @date 2019/04/18
 */
public class EtcdRegistryProvider implements RegistryProvider {
    @Override
    public RegistryService provide() {
        return EtcdRegistryServiceImpl.getInstance();
    }
}
