package com.xerecter.xrate.xrate_core.dto;

import com.xerecter.xrate.xrate_core.entity.TransactionInfo;
import com.xerecter.xrate.xrate_core.entity.TransactionMember;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class TransactionInfoDto {

    private TransactionInfo transactionInfo;

    private Executor executor;

    private int retryTimes = 0;

    private boolean mainCancel = false;

    private boolean memberCancel = false;

    public static interface Executor {
        public void execute(TransactionInfoDto transactionInfoDto);
    }

}
