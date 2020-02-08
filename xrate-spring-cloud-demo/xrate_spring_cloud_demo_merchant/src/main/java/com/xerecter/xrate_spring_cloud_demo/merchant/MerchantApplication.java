package com.xerecter.xrate_spring_cloud_demo.merchant;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.hystrix.EnableHystrix;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportResource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication()
@EnableTransactionManagement
@EnableCaching
@MapperScans(
        value = {@MapperScan("com.xerecter.xrate_spring_cloud_demo.merchant.mapper")}
)
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableHystrix
@EnableFeignClients
public class MerchantApplication {

    public static void main(String[] args) {
        SpringApplication.run(MerchantApplication.class, args);
    }

}
