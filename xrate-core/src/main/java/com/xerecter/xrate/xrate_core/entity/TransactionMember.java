package com.xerecter.xrate.xrate_core.entity;

import lombok.Data;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class TransactionMember {

    private String parentTransId;

    private String transId;

    private String address;

    private String tryName;

    private String memberClassName;

    private List<String> paramClassNames;

    private byte[] params;

}
