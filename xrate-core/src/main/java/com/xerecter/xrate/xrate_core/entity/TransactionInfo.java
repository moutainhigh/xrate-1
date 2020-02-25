package com.xerecter.xrate.xrate_core.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;
import java.util.*;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName(value = "transaction_info", autoResultMap = true)
public class TransactionInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 事务id 雪花算法 + 位置
     */
    @TableId("trans_id")
    private String transId;

    @TableField("is_start")
    private Boolean isStart;

    @TableField("hold_service_id")
    private String holdServiceId;

    @TableField("trans_status")
    private Integer transStatus;

    @TableField("need_cancel")
    private Boolean needCancel;

    /**
     * 是否需要执行完成操作，也就是是否删除对应的信息
     */
    @TableField("need_success")
    private Boolean needSuccess;

    @TableField("try_name")
    private String tryName;

    @TableField("cancel_name")
    private String cancelName;

    @TableField("bean_class_name")
    private String beanClassName;

    @TableField(
            value = "param_class_names",
            typeHandler = FastjsonTypeHandler.class,
            jdbcType = JdbcType.LONGVARCHAR
    )
    private List<String> paramClassNames;

    @TableField("params")
    private byte[] params;

    @TableField("result")
    private byte[] result;

    @TableField(
            exist = false,
            insertStrategy = FieldStrategy.IGNORED,
            updateStrategy = FieldStrategy.IGNORED,
            whereStrategy = FieldStrategy.IGNORED
    )
    private List<TransactionMember> transactionMembers = new ArrayList<>();

}
