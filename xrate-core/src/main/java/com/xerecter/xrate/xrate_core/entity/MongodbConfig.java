package com.xerecter.xrate.xrate_core.entity;

import lombok.Data;

/**
 * mongodb配置文件
 *
 * @author xdd
 */
@Data
public class MongodbConfig {

    private String username = "";

    private String password = "";

    private String database = "";

    private String host = "";

    private int port;

    private String connectString = "";

    private String options = "";

}
