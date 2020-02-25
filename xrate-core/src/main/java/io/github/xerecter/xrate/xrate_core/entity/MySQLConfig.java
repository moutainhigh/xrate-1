package io.github.xerecter.xrate.xrate_core.entity;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class MySQLConfig {

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 驱动类型
     */
    private String driverClassName;

    /**
     * 连接字符串
     */
    private String url;

}
