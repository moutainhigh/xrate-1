package com.xerecter.xrate.xrate_dubbo.route_factory;

import com.xerecter.xrate.xrate_dubbo.router.XrateDubboRouter;
import org.apache.dubbo.common.URL;
import org.apache.dubbo.common.constants.CommonConstants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.configcenter.DynamicConfiguration;
import org.apache.dubbo.rpc.cluster.Router;
import org.apache.dubbo.rpc.cluster.RouterFactory;
import org.apache.dubbo.rpc.cluster.router.condition.config.AppRouter;
import org.apache.dubbo.rpc.cluster.router.condition.config.AppRouterFactory;

@Activate(group = {CommonConstants.CONSUMER_SIDE}, order = 0)
public class XrateDubboRouterFactory extends AppRouterFactory {

    private volatile Router router;

    @Override
    public Router getRouter(URL url) {
        if (this.router != null) {
            return this.router;
        }
        synchronized (this) {
            if (this.router == null) {
                this.router = createRouter(url);
            }
        }
        return this.router;
    }

    private Router createRouter(URL url) {
        return new XrateDubboRouter(DynamicConfiguration.getDynamicConfiguration(), url);
    }
}
