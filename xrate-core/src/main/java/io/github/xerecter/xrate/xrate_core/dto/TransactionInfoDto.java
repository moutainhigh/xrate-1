package io.github.xerecter.xrate.xrate_core.dto;

import io.github.xerecter.xrate.xrate_core.entity.TransactionInfo;
import lombok.Data;

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
