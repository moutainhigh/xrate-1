package com.xerecter.xrate.xrate_spring_cloud.config;

import com.xerecter.xrate.xrate_spring_cloud.interceptor.XrateSpringCloudInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * springcloud 环境配置
 *
 * @author xdd
 */
@Configuration
public class XrateSpringCloudConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new XrateSpringCloudInterceptor())
                .addPathPatterns("/**")
                .order(Integer.MIN_VALUE);
    }

}
