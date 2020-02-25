package com.xerecter.xrate.xrate_core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.FastjsonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.type.JdbcType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@TableName(value = "transaction_mb", autoResultMap = true)
public class TransactionMember implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableField("parent_trans_id")
    private String parentTransId;

    @TableField("trans_id")
    private String transId;

    @TableField("address")
    private String address;

    @TableField("try_name")
    private String tryName;

    @TableField("member_class_name")
    private String memberClassName;

    @TableField(
            value = "param_class_names",
            typeHandler = FastjsonTypeHandler.class,
            jdbcType = JdbcType.LONGVARCHAR
    )
    private List<String> paramClassNames;

    @TableField("params")
    private byte[] params;

}
