package com.xerecter.xrate.xrate_spring_cloud.config;

import com.xerecter.xrate.xrate_spring_cloud.interceptor.XrateSpringRequestInterceptor;
import feign.Feign;
import feign.Retryer;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

@Configuration
public class XrateFeignClientsConfiguration extends FeignClientsConfiguration {

    @Override
    @Scope("prototype")
    @Primary
    @Bean
    public Feign.Builder feignBuilder(Retryer retryer) {
        return Feign.builder().requestInterceptor(new XrateSpringRequestInterceptor()).retryer(retryer);
    }

}
