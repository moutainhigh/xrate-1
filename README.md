# Xrate分布式事务
Xrate是一个最终一致性分布式事务,整个事务分为两个阶段

下面用一个订单示例来说明整个事务过程，整个过程分为增加订单、增加商家余额、减少用户余额  

1.先进行增加订单操作,这时会开启本地事务,然后远程调用增加商家余额和减少用户余额操作  
2.如果整个过程未出现任何错误,整个事务进行完毕,异步删除对应的事务信息  
3.如果其中某个环节出现错误,直接回滚本地事务,并异步执行已经操作完毕的服务的cancel方法  

## Quick Start
1.下载源码进行安装  
2.引入对应的starter  
3.编写对应的配置
```properties
#基本配置
#序列化方式,默认kyro,有kyro和java两种
io.github.xerecter.xrate.config.serializer-way=kyro
#是否异步调用,默认true,不推荐为false,为false可能会造成大量的线程进入阻塞状态
io.github.xerecter.xrate.config.async-invoke=true
#持久化方式,默认mongodb,暂时只支持mongodb,以后会加入mysql、mysql、redis、file
io.github.xerecter.xrate.config.persistence-way=mongodb
#服务名称,默认为spring.application.name属性值
io.github.xerecter.xrate.config.service-name=xrate
#服务id,默认为当前ip地址加端口号
io.github.xerecter.xrate.config.service-id=xrate-1
#异步缓冲大小,默认为cpu核数*2,框架异步调用采用的disruptor,这里配置的缓冲大小等于disruptor的ring buffer大小
io.github.xerecter.xrate.config.async-buffer-size=8
#当cancel失败时,重试的次数,默认为-1,-1代表一直重试下去,直到成功
io.github.xerecter.xrate.config.retry-times=-1
#当cancel失败时,重试的间隔,单位秒,默认为5,重试时间根据重试次数依次递增
io.github.xerecter.xrate.config.retry-interval=5
#是否需要初始化检测,默认true,项目启动完成后会有一个初始化检测,主要用于执行未完成的事务或者cancel操作
io.github.xerecter.xrate.config.need-init-check=true
#初始化检测间隔,默认0,主要用于解决各项目间的依赖
io.github.xerecter.xrate.config.init-check-interval=0
#是否开启调试模式，开启以后会打印出大量执行过程信息，如非必要不建议开启，默认false
io.github.xerecter.xrate.config.debug-mode=false
#mongodb配置
#mongodb用户名
io.github.xerecter.xrate.mongodb-config.username=admin
#mongodb密码
io.github.xerecter.xrate.mongodb-config.password=admin
#mongodb数据库
io.github.xerecter.xrate.mongodb-config.database=xrate
#mongodb地址
io.github.xerecter.xrate.mongodb-config.host=127.0.0.1
#mongodb端口
io.github.xerecter.xrate.mongodb-config.port=27017
#mongodb连接选项
io.github.xerecter.xrate.mongodb-config.options=
#mongodb连接字符串,如果使用连接字符串会覆盖其他配置
io.github.xerecter.xrate.mongodb-config.connect-string=
#mysql配置
#连接驱动名称
io.github.xerecter.xrate.my-s-q-l-config.driver-class-name=com.mysql.cj.jdbc.Driver
#用户名
io.github.xerecter.xrate.my-s-q-l-config.username=root
#密码
io.github.xerecter.xrate.my-s-q-l-config.password=root
#连接字符串
io.github.xerecter.xrate.my-s-q-l-config.url=jdbc:mysql://127.0.0.1:3306/xrate
```  
4.在对应的方法上面加上注解  
```java
@XrateTransaction("methodCancel")
public Object method(Object arg){
    return null;
}

public void methodCancel(Object arg){
}
```  
```text
@XrateTransaction 各个属性作用解释

1. cancelMethod
cancel方法名称，用于在try方法抛出异常时执行事务补偿操作，value与cancelMethod作用一致，两者互通

2. asyncInvoke
当前方法的回调方式，也就是如何执行cancel方法或删除事务信息，默认采用当前事务的值，当前事务的值来自于事务开启方的值，事务开启方如果未在局部进
行设置则会采用全局配置的值，如果在局部进行了设置，在执行时则会优先采用局部的值，retryTimes和retryInterval采用同样的机制，NONE 代表未进行
设置(默认值)，SYNC 代表同步执行，ASYNC 代表异步执行

3. retryTimes
执行cancel方法或者删除事务信息的重试次数，默认 -1

4. retryInterval
执行cancel方法或者删除事务信息的重试间隔，默认 -1

```
5.在对应的远程调用的方法加上@XrateTransaction注解(主要是为了标注此方法开启分布式事务,否则将不会开启分布式事务)
 + dubbo加在service方法上
 + spring-cloud加在feignclient方法上

### Dubbo 依赖
```xml
<dependency>
    <groupId>io.github.xerecter</groupId>
    <artifactId>xrate-spring-boot-starter-dubbo</artifactId>
    <version>1.0.0.RELEASE</version>
</dependency>
```

### Spring Cloud 依赖
```xml
<dependency>
    <groupId>io.github.xerecter</groupId>
    <artifactId>xrate-spring-boot-starter-spring-cloud</artifactId>
    <version>1.0.0.RELEASE</version>
</dependency>
```  

### Demo
#### Dubbo
[DubboDemo](https://github.com/xerecter/xrate/tree/master/xrate-dubbo-demo)
#### Spring Cloud
[SpringCloudDemo](https://github.com/xerecter/xrate/tree/master/xrate-spring-cloud-demo)
