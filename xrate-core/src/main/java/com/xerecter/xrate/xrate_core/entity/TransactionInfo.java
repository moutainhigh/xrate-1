package com.xerecter.xrate.xrate_core.entity;

import lombok.Data;

import java.util.*;

@Data
public class TransactionInfo {

    /**
     * 事务id 雪花算法 + 位置
     */
    private String transId;

    private boolean isStart;

    private String holdServiceId;

    private int transStatus;

    private boolean needCancel;

    /**
     * 是否需要执行完成操作，也就是是否删除对应的信息
     */
    private boolean needSuccess;

    private String tryName;

    private String cancelName;

    private String beanClassName;

    private List<String> paramClassNames;

    private byte[] params;

    private byte[] result;

    private List<TransactionMember> transactionMembers = new ArrayList<>();

}
