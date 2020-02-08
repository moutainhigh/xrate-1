package com.xerecter.xrate_dubbo_demo.user;

import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.annotation.MapperScans;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.ImportResource;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication()
@ImportResource(locations = {"classpath:application-dubbo.xml"})
@EnableTransactionManagement
@EnableCaching
@MapperScans(
        value = {@MapperScan("com.xerecter.xrate_dubbo_demo.user.mapper")}
)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class UserApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserApplication.class, args);
    }
}
