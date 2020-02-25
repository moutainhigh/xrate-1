package io.github.xerecter.xrate.xrate_core.service.impl;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.PaginationInterceptor;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import io.github.xerecter.xrate.xrate_core.constants.CommonConstants;
import io.github.xerecter.xrate.xrate_core.entity.MySQLConfig;
import io.github.xerecter.xrate.xrate_core.entity.TransactionInfo;
import io.github.xerecter.xrate.xrate_core.entity.TransactionMember;
import io.github.xerecter.xrate.xrate_core.entity.XrateConfig;
import io.github.xerecter.xrate.xrate_core.mapper.TransactionInfoMapper;
import io.github.xerecter.xrate.xrate_core.mapper.TransactionMemberMapper;
import io.github.xerecter.xrate.xrate_core.service.ITransactionInfoService;
import com.zaxxer.hikari.HikariDataSource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.springframework.util.Assert;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.sql.*;
import java.util.List;
import java.util.Map;

@Slf4j
public class MySQLTransactionInfoServiceImpl implements ITransactionInfoService {

    private XrateConfig xrateConfig;

    private SqlSessionFactory sqlSessionFactory;

    private String transTableName;

    private String transMbTableName;

    @SneakyThrows
    public MySQLTransactionInfoServiceImpl(XrateConfig xrateConfig, Object persistenceConfig) {
        this.xrateConfig = xrateConfig;
        transTableName = xrateConfig.getServiceName() + "_";
        transMbTableName = xrateConfig.getServiceName() + "_mb" + "_";
        changeAnnotationValue(TransactionInfo.class.getAnnotation(TableName.class), "value", transTableName);
        changeAnnotationValue(TransactionMember.class.getAnnotation(TableName.class), "value", transMbTableName);
        MySQLConfig mySQLConfig = (MySQLConfig) persistenceConfig;
        DataSource dataSource = getDataSource(mySQLConfig);
        PaginationInterceptor paginationInterceptor = new PaginationInterceptor();
        MybatisConfiguration mybatisConfiguration = new MybatisConfiguration();
        mybatisConfiguration.addInterceptor(paginationInterceptor);
        mybatisConfiguration.addMapper(TransactionInfoMapper.class);
        mybatisConfiguration.addMapper(TransactionMemberMapper.class);
        MybatisSqlSessionFactoryBean mybatisSqlSessionFactory = new MybatisSqlSessionFactoryBean();
        mybatisSqlSessionFactory.setDataSource(dataSource);
        mybatisSqlSessionFactory.setConfiguration(mybatisConfiguration);
        mybatisSqlSessionFactory.setTypeAliasesPackage("com.xerecter.xrate.xrate_core.entity");
        this.sqlSessionFactory = mybatisSqlSessionFactory.getObject();
        initTransTable(dataSource);
        initTransMbTable(dataSource);
    }

    public static void changeAnnotationValue(Annotation annotation, String key, Object newValue) throws Exception {
        InvocationHandler handler = Proxy.getInvocationHandler(annotation);
        Field f;
        try {
            f = handler.getClass().getDeclaredField("memberValues");
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalStateException(e);
        }
        f.setAccessible(true);
        Map<String, Object> memberValues;
        memberValues = (Map<String, Object>) f.get(handler);
        Object oldValue = memberValues.get(key);
        if (oldValue == null || oldValue.getClass() != newValue.getClass()) {
            throw new IllegalArgumentException();
        }
        memberValues.put(key, newValue);
    }

    private DataSource getDataSource(MySQLConfig mySQLConfig) {
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setDriverClassName(mySQLConfig.getDriverClassName());
        hikariDataSource.setUsername(mySQLConfig.getUsername());
        hikariDataSource.setPassword(mySQLConfig.getPassword());
        hikariDataSource.setJdbcUrl(mySQLConfig.getUrl());
        hikariDataSource.setAutoCommit(true);
        hikariDataSource.setPoolName(CommonConstants.HIKARICP_POOL_NAME);
        return hikariDataSource;
    }

    @SneakyThrows
    private void initTransTable(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            String createSql = String.format(
                    "create table if not exists `%s`" +
                            "(" +
                            "    trans_id          char(32)    default 'null' not null," +
                            "    is_start          bit         default 0      not null," +
                            "    hold_service_id   varchar(64) default 'null' null," +
                            "    trans_status      int         default 0      not null," +
                            "    need_cancel       bit         default 0      not null," +
                            "    need_success      bit         default 0      not null," +
                            "    try_name          text                       null," +
                            "    cancel_name       text                       null," +
                            "    bean_class_name   text                       null," +
                            "    param_class_names text                       null," +
                            "    params            longblob                   null," +
                            "    result            longblob                   null," +
                            "    constraint trans_pk" +
                            "        primary key (trans_id)" +
                            ");", transTableName
            );
            try (PreparedStatement preparedStatement = connection.prepareStatement(createSql)) {
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        try (Connection connection = dataSource.getConnection()) {
            if (!existsTableIndex(dataSource, transTableName, "hold_service_id_index")) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                        "create index `hold_service_id_index`" +
                                "    on `%s` (hold_service_id);", transTableName
                ))) {
                    preparedStatement.executeUpdate();
                }
            }
        }
    }

    @SneakyThrows
    private void initTransMbTable(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                    "create table if not exists `%s`" +
                            "(" +
                            "    trans_id          char(32) default 'null' not null," +
                            "    parent_trans_id   char(32) default 'null' null," +
                            "    address           text                    null," +
                            "    try_name          text                    null," +
                            "    member_class_name text                    null," +
                            "    param_class_names text                    null," +
                            "    params            longblob                null," +
                            "    constraint trans_mb_pk" +
                            "        primary key (trans_id)" +
                            ");", transMbTableName
            ))) {
                preparedStatement.executeUpdate();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
        try (Connection connection = dataSource.getConnection()) {
            if (!existsTableIndex(dataSource, transMbTableName, "parent_trans_id_index")) {
                try (PreparedStatement preparedStatement = connection.prepareStatement(String.format(
                        "create index `parent_trans_id_index`" +
                                "    on `%s` (parent_trans_id);",
                        transMbTableName
                ))) {
                    preparedStatement.executeUpdate();
                }
            }
        }
    }

    /**
     * 是否存在对应索引
     *
     * @param dataSource 数据源
     * @param indexName  索引名称
     * @return 是否存在
     */
    @SneakyThrows
    private boolean existsTableIndex(DataSource dataSource, String tableName, String indexName) {
        boolean res = false;
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("show indexes from `" + tableName + "`")) {
                try (ResultSet resultSet = preparedStatement.executeQuery()) {
                    while (resultSet.next()) {
                        String index = resultSet.getString("Key_name");
                        if (indexName.equals(index)) {
                            res = true;
                            break;
                        }
                    }
                }
            }
        }
        return res;
    }

    @Override
    public TransactionInfo addTransactionInfo(TransactionInfo transactionInfo) {
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            Assert.isTrue(sqlSession.getMapper(TransactionInfoMapper.class).insert(transactionInfo) > 0, "save trans error");
            return transactionInfo;
        }
    }

    @Override
    public boolean updateTransactionStatus(String transId, int status) {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTransId(transId);
        transactionInfo.setTransStatus(status);
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return sqlSession.getMapper(TransactionInfoMapper.class).updateById(transactionInfo) > 0;
        }
    }

    @Override
    public boolean updateTransactionResult(String transId, byte[] result) {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTransId(transId);
        transactionInfo.setResult(result);
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return sqlSession.getMapper(TransactionInfoMapper.class).updateById(transactionInfo) > 0;
        }
    }

    @Override
    public boolean updateTransactionStatusAndResult(String transId, int status, byte[] result) {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTransId(transId);
        transactionInfo.setTransStatus(status);
        transactionInfo.setResult(result);
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return sqlSession.getMapper(TransactionInfoMapper.class).updateById(transactionInfo) > 0;
        }
    }

    @Override
    public TransactionInfo getTransactionInfo(String transId, String serviceId) {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTransId(transId);
        transactionInfo.setHoldServiceId(serviceId);
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return sqlSession.getMapper(TransactionInfoMapper.class).selectOne(new QueryWrapper<>(transactionInfo));
        }
    }

    @Override
    public TransactionInfo getSimpleTransactionInfo(String transId, String serviceId) {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setTransId(transId);
        transactionInfo.setHoldServiceId(serviceId);
        QueryWrapper<TransactionInfo> queryWrapper = new QueryWrapper<>(transactionInfo);
        queryWrapper.select(
                "trans_id",
                "is_start",
                "hold_service_id",
                "trans_status",
                "need_cancel",
                "need_success",
                "try_name",
                "cancel_name",
                "hold_service_id",
                "bean_class_name",
                "param_class_names"
        );
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            transactionInfo = sqlSession.getMapper(TransactionInfoMapper.class).selectOne(queryWrapper);
            if (transactionInfo != null) {
                transactionInfo.setTransactionMembers(this.getSimpleTransactionMembers(transactionInfo.getTransId()));
                return transactionInfo;
            }
            return null;
        }
    }

    @Override
    public List<TransactionMember> getSimpleTransactionMembers(String parentTransId) {
        TransactionMember transactionMember = new TransactionMember();
        transactionMember.setParentTransId(parentTransId);
        QueryWrapper<TransactionMember> queryWrapper = new QueryWrapper<>(transactionMember);
        queryWrapper.select(
                "trans_id",
                "parent_trans_id",
                "address",
                "try_name",
                "member_class_name"
        );
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return sqlSession.getMapper(TransactionMemberMapper.class).selectList(queryWrapper);
        }
    }

    @Override
    public List<TransactionInfo> getTransactionInfos(String serviceId) {
        TransactionInfo transactionInfo = new TransactionInfo();
        transactionInfo.setHoldServiceId(serviceId);
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true);) {
            return sqlSession.getMapper(TransactionInfoMapper.class).selectList(new QueryWrapper<>(transactionInfo));
        }
    }

    @Override
    public TransactionMember addTransactionMember(TransactionMember transactionMember) {
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            Assert.isTrue(sqlSession.getMapper(TransactionMemberMapper.class).insert(transactionMember) > 0, "save trans mb error");
            return transactionMember;
        }
    }

    @Override
    public List<TransactionMember> getTransactionMembers(String parentTransId) {
        TransactionMember transactionMember = new TransactionMember();
        transactionMember.setParentTransId(parentTransId);
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true);) {
            return sqlSession.getMapper(TransactionMemberMapper.class).selectList(new QueryWrapper<>(transactionMember));
        }
    }

    @Override
    public boolean removeTransactionInfo(String transId) {
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return sqlSession.getMapper(TransactionInfoMapper.class).deleteById(transId) > 0;
        }
    }

    @Override
    public boolean removeTransactionMembers(String parentTransId) {
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return sqlSession.getMapper(TransactionMemberMapper.class).delete(
                    new QueryWrapper<>(
                            new TransactionMember()
                                    .setParentTransId(parentTransId)
                    )
            ) > 0;
        }
    }

    @Override
    public boolean updateTransactionNeedCancel(String transId, boolean needCancel) {
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return sqlSession.getMapper(TransactionInfoMapper.class).updateById(
                    new TransactionInfo()
                            .setTransId(transId).
                            setNeedCancel(needCancel)
            ) > 0;
        }
    }

    @Override
    public boolean updateTransactionNeedCancelAndStatus(String transId, boolean needCancel, int status) {
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return sqlSession.getMapper(TransactionInfoMapper.class).updateById(
                    new TransactionInfo()
                            .setTransId(transId)
                            .setNeedCancel(needCancel)
                            .setTransStatus(status)
            ) > 0;
        }
    }

    @Override
    public boolean updateTransactionNeedSuccess(String transId, boolean needSuccess) {
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return sqlSession.getMapper(TransactionInfoMapper.class).updateById(
                    new TransactionInfo()
                            .setTransId(transId)
                            .setNeedSuccess(needSuccess)
            ) > 0;
        }
    }

    @Override
    public boolean updateTransactionNeedSuccessAndStatus(String transId, boolean needSuccess, int status) {
        try (SqlSession sqlSession = this.sqlSessionFactory.openSession(true)) {
            return sqlSession.getMapper(TransactionInfoMapper.class).updateById(
                    new TransactionInfo()
                            .setTransId(transId)
                            .setNeedSuccess(needSuccess)
                            .setTransStatus(status)
            ) > 0;
        }
    }
}
