package io.github.xerecter.xrate.xrate_spring_cloud.factory;

import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.ILoadBalancer;
import io.github.xerecter.xrate.xrate_spring_cloud.load_balance.XrateLoadBalancer;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.netflix.ribbon.ServerIntrospector;
import org.springframework.cloud.netflix.ribbon.SpringClientFactory;
import org.springframework.cloud.openfeign.ribbon.CachingSpringLoadBalancerFactory;
import org.springframework.cloud.openfeign.ribbon.FeignLoadBalancer;
import org.springframework.cloud.openfeign.ribbon.RetryableFeignLoadBalancer;
import org.springframework.util.ConcurrentReferenceHashMap;

import java.util.Map;

public class XrateCachingSpringLoadBalancerFactory extends CachingSpringLoadBalancerFactory {

    private volatile Map<String, FeignLoadBalancer> cache = new ConcurrentReferenceHashMap<>();

    public XrateCachingSpringLoadBalancerFactory(SpringClientFactory factory) {
        super(factory);
    }

    public XrateCachingSpringLoadBalancerFactory(
            SpringClientFactory factory,
            LoadBalancedRetryFactory loadBalancedRetryPolicyFactory
    ) {
        super(factory, loadBalancedRetryPolicyFactory);
    }

    public XrateCachingSpringLoadBalancerFactory(
            SpringClientFactory factory,
            LoadBalancedRetryFactory loadBalancedRetryPolicyFactory,
            Map<String, FeignLoadBalancer> cache
    ) {
        super(factory, loadBalancedRetryPolicyFactory);
        this.cache = cache;
    }

    @Override
    public FeignLoadBalancer create(String clientName) {
        FeignLoadBalancer client = this.cache.get(clientName);
        if (client != null) {
            return client;
        }
        IClientConfig config = this.factory.getClientConfig(clientName);
        ILoadBalancer lb = new XrateLoadBalancer(this.factory.getLoadBalancer(clientName));
        ServerIntrospector serverIntrospector = this.factory.getInstance(clientName,
                ServerIntrospector.class);
        client = this.loadBalancedRetryFactory != null
                ? new RetryableFeignLoadBalancer(lb, config, serverIntrospector,
                this.loadBalancedRetryFactory)
                : new FeignLoadBalancer(lb, config, serverIntrospector);
        this.cache.put(clientName, client);
        return client;
    }
}
