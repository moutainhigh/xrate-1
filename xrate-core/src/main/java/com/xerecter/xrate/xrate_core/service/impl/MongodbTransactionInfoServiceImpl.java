package com.xerecter.xrate.xrate_core.service.impl;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.*;
import com.mongodb.client.model.DeleteOptions;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import com.xerecter.xrate.xrate_core.entity.MongodbConfig;
import com.xerecter.xrate.xrate_core.entity.TransactionInfo;
import com.xerecter.xrate.xrate_core.entity.TransactionMember;
import com.xerecter.xrate.xrate_core.entity.XrateConfig;
import com.xerecter.xrate.xrate_core.service.ITransactionInfoService;
import com.xerecter.xrate.xrate_core.util.CommonUtil;
import com.xerecter.xrate.xrate_core.util.ReflectUtil;
import com.xerecter.xrate.xrate_core.util.TransactionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.Consumer;

@Slf4j
public class MongodbTransactionInfoServiceImpl implements ITransactionInfoService {

    private MongoClient mongoClient;

    private MongodbConfig mongodbConfig;

    private XrateConfig xrateConfig;

    private String transCollectionName;

    private String transMbCollectionName;

    public MongodbTransactionInfoServiceImpl(XrateConfig xrateConfig, Object persistenceConfig) {
        mongodbConfig = (MongodbConfig) persistenceConfig;
        this.xrateConfig = xrateConfig;
        transCollectionName = CommonUtil.camelToUnderline(this.xrateConfig.getServiceName(), true);
        transMbCollectionName = transCollectionName + "_mb";

        ConnectionString connectionString;
        if (StringUtils.isNotBlank(mongodbConfig.getConnectString())) {
            connectionString = new ConnectionString(mongodbConfig.getConnectString());
        } else {
            connectionString = new ConnectionString(String.format("mongodb://%s:%s@%s:%s/%s?%s",
                    mongodbConfig.getUsername(),
                    mongodbConfig.getPassword(),
                    mongodbConfig.getHost(),
                    mongodbConfig.getPort(),
                    mongodbConfig.getDatabase(),
                    mongodbConfig.getOptions()));
        }
        mongoClient = MongoClients.create(MongoClientSettings.builder()
                .applyConnectionString(connectionString)
                .build());
        initCollectionTransIndex();
        initCollectionTransMbIndex();
    }

    /**
     * 初始化事务索引
     */
    private void initCollectionTransIndex() {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());

        boolean existsTransIdIndex = false;
        boolean existsTransIndex = false;
        MongoCollection<Document> transCollection = database.getCollection(transCollectionName);
        ListIndexesIterable<Document> transIndexes = transCollection.listIndexes();
        for (Document indexInfo : transIndexes) {
            String name = indexInfo.getString("name");
            if ("unique_trans_id_index".equals(name)) {
                existsTransIdIndex = true;
            } else if ("trans_id_is_start_hold_service_id_index".equals(name)) {
                existsTransIndex = true;
            }
        }

        if (!existsTransIdIndex) {
            Document uniqueTransIdIndex = new Document();
            uniqueTransIdIndex.append("trans_id", -1);
            IndexOptions indexOptions = new IndexOptions();
            indexOptions.name("unique_trans_id_index");
            indexOptions.unique(true);
            indexOptions.background(true);
            try {
                transCollection.createIndex(uniqueTransIdIndex, indexOptions);
            } catch (Exception e) {
                TransactionUtil.printDebugInfo(() -> log.info("create unique trans id index error"));
            }
        }

        if (!existsTransIndex) {
            Document transIndex = new Document();
            transIndex.append("trans_id", -1);
            transIndex.append("is_start", -1);
            transIndex.append("hold_service_id", -1);
            IndexOptions indexOptions = new IndexOptions();
            indexOptions.name("trans_id_is_start_hold_service_id_index");
            indexOptions.background(true);
            try {
                transCollection.createIndex(transIndex, indexOptions);
            } catch (Exception e) {
                TransactionUtil.printDebugInfo(() -> log.info("create trans index error"));
            }
        }
    }

    /**
     * 初始化事务成员索引
     */
    private void initCollectionTransMbIndex() {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        boolean existsTransMbIndex = false;
        MongoCollection<Document> transMbCollection = database.getCollection(transMbCollectionName);
        ListIndexesIterable<Document> transMbIndexes = transMbCollection.listIndexes();
        for (Document indexInfo : transMbIndexes) {
            String name = indexInfo.getString("name");
            if ("trans_mb_trans_id_index".equals(name)) {
                existsTransMbIndex = true;
            }
        }
        if (!existsTransMbIndex) {
            Document transMbIndex = new Document();
            transMbIndex.append("trans_id", -1);
            IndexOptions indexOptions = new IndexOptions();
            indexOptions.name("trans_mb_trans_id_index");
            indexOptions.background(true);
            try {
                transMbCollection.createIndex(transMbIndex, indexOptions);
            } catch (Exception e) {
                TransactionUtil.printDebugInfo(() -> log.info("create trans mb trans id index error"));
            }
        }
    }

    @Override
    public TransactionInfo addTransactionInfo(TransactionInfo transactionInfo) {
        MongoDatabase database = this.mongoClient.getDatabase(this.mongodbConfig.getDatabase());
        MongoCollection<Document> transCollection = database.getCollection(transCollectionName);
        Document document = getDocumentByTransactionInfo(transactionInfo, CommonUtil.<String>getListByArray("transactionMembers", "params", "result"));
        if (transactionInfo.getParams() != null) {
            document.append("params", Base64.getEncoder().encodeToString(transactionInfo.getParams()));
        } else {
            document.append("params", "");
        }
        if (transactionInfo.getResult() != null) {
            document.append("result", Base64.getEncoder().encodeToString(transactionInfo.getResult()));
        } else {
            document.append("result", "");
        }
        try {
            transCollection.insertOne(document);
        } catch (Exception e) {
            e.printStackTrace();
            throw new IllegalArgumentException("insert transactionInfo error");
        }
        return transactionInfo;
    }

    @Override
    public boolean updateTransactionStatus(String transId, int status) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transCollection = database.getCollection(transCollectionName);
        Document updateDocument = new Document();
        Document document = new Document();
        document.append("trans_status", status);
        updateDocument.append("$set", document);
        UpdateResult updateResult = transCollection.updateOne(Filters.and(Filters.eq("trans_id", transId)), updateDocument);
        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public boolean updateTransactionResult(String transId, byte[] result) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transCollection = database.getCollection(transCollectionName);
        Document updateDocument = new Document();
        Document document = new Document();
        document.append("result", Base64.getEncoder().encodeToString(result));
        updateDocument.append("$set", database);
        UpdateResult updateResult = transCollection.updateOne(Filters.and(Filters.eq("trans_id", transId)), updateDocument);
        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public boolean updateTransactionStatusAndResult(String transId, int status, byte[] result) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transCollection = database.getCollection(transCollectionName);
        Document updateDocument = new Document();
        Document document = new Document();
        document.append("trans_status", status);
        document.append("result", Base64.getEncoder().encodeToString(result));
        updateDocument.append("$set", document);
        UpdateResult updateResult = transCollection.updateOne(Filters.and(Filters.eq("trans_id", transId)), updateDocument);
        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public TransactionInfo getTransactionInfo(String transId, String serviceId) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transCollection = database.getCollection(transCollectionName);
        FindIterable<Document> documents = transCollection.find(Filters.and(Filters.eq("trans_id", transId),
                Filters.<String>eq("hold_service_id", serviceId)));
        List<TransactionInfo> transactionInfos = new ArrayList<>();
        documents.forEach((Consumer<? super Document>) (document) -> {
            TransactionInfo transactionInfo = getTransactionInfoByDocument(document, CommonUtil.<String>getListByArray("transactionMembers", "params", "result"));
            String params = document.getString("params");
            if (StringUtils.isNotBlank(params)) {
                transactionInfo.setParams(Base64.getDecoder().decode(params));
            }
            String result = document.getString("result");
            if (StringUtils.isNotBlank(result)) {
                transactionInfo.setResult(Base64.getDecoder().decode(result));
            }
            transactionInfo.setTransactionMembers(getTransactionMembers(transactionInfo.getTransId()));
            transactionInfos.add(transactionInfo);
        });
        return transactionInfos.size() > 0 ? transactionInfos.get(0) : null;
    }

    @Override
    public TransactionInfo getSimpleTransactionInfo(String transId, String serviceId) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transCollection = database.getCollection(transCollectionName);
        Document projection = new Document();
        projection.append("params", 0);
        projection.append("result", 0);
        FindIterable<Document> documents = transCollection.find(Filters.and(Filters.eq("trans_id", transId),
                Filters.<String>eq("hold_service_id", serviceId))).projection(projection);
        Document first = documents.first();
        TransactionInfo transactionInfo = getTransactionInfoByDocument(first, CommonUtil.<String>getListByArray("transactionMembers", "params", "result"));
        transactionInfo.setParams(new byte[0]);
        transactionInfo.setResult(new byte[0]);
        transactionInfo.setTransactionMembers(this.getSimpleTransactionMembers(transId));
        return transactionInfo;
    }

    @Override
    public List<TransactionMember> getSimpleTransactionMembers(String parentTransId) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transMbCollection = database.getCollection(transMbCollectionName);
        Document projection = new Document();
        projection.append("params", 0);
        FindIterable<Document> documents = transMbCollection.find(Filters.and(Filters.eq("parent_trans_id", parentTransId))).projection(projection);
        List<TransactionMember> transactionMembers = new ArrayList<>();
        documents.forEach((Consumer<? super Document>) document ->
                transactionMembers.add(getTransactionMemberByDocument(document, CommonUtil.getListByArray("params"))));
        return transactionMembers;
    }

    @Override
    public List<TransactionInfo> getTransactionInfos(String serviceId) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transCollection = database.getCollection(transCollectionName);
        FindIterable<Document> documents = transCollection.find(Filters.and(Filters.<String>eq("hold_service_id", serviceId)));
        List<TransactionInfo> transactionInfos = new ArrayList<>();
        documents.forEach((Consumer<? super Document>) (document) -> {
            TransactionInfo transactionInfo = getTransactionInfoByDocument(document, CommonUtil.<String>getListByArray("transactionMembers", "params", "result"));
            String params = document.getString("params");
            if (StringUtils.isNotBlank(params)) {
                transactionInfo.setParams(Base64.getDecoder().decode(params));
            }
            String result = document.getString("result");
            if (StringUtils.isNotBlank(result)) {
                transactionInfo.setResult(Base64.getDecoder().decode(result));
            }
            transactionInfo.setTransactionMembers(getTransactionMembers(transactionInfo.getTransId()));
            transactionInfos.add(transactionInfo);
        });
        return transactionInfos;
    }

    @Override
    public TransactionMember addTransactionMember(TransactionMember transactionMember) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transMbCollection = database.getCollection(transMbCollectionName);
        Document document = this.getDocumentByTransactionMember(transactionMember, CommonUtil.<String>getListByArray("params"));
        if (transactionMember.getParams() != null) {
            document.append("params", Base64.getEncoder().encodeToString(transactionMember.getParams()));
        } else {
            document.append("params", "");
        }
        try {
            transMbCollection.insertOne(document);
            return transactionMember;
        } catch (Exception e) {
            throw new IllegalArgumentException("insert trans member error");
        }
    }

    @Override
    public List<TransactionMember> getTransactionMembers(String parentTransId) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transMbCollection = database.getCollection(transMbCollectionName);
        FindIterable<Document> documents = transMbCollection.find(Filters.and(Filters.eq("parent_trans_id", parentTransId)));
        List<TransactionMember> transactionMembers = new ArrayList<>();
        documents.forEach((Consumer<? super Document>) (document) -> {
            TransactionMember transactionMember = getTransactionMemberByDocument(document, CommonUtil.getListByArray("params"));
            String params = document.getString("params");
            if (StringUtils.isNotBlank(params)) {
                transactionMember.setParams(Base64.getDecoder().decode(params));
            }
            transactionMembers.add(transactionMember);
        });
        return transactionMembers;
    }

    @Override
    public boolean removeTransactionInfo(String transId) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transCollection = database.getCollection(transCollectionName);
        DeleteResult deleteResult = transCollection.deleteOne(Filters.and(Filters.eq("trans_id", transId)));
        return deleteResult.getDeletedCount() > 0;
    }

    @Override
    public boolean removeTransactionMembers(String parentTransId) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transMbCollection = database.getCollection(transMbCollectionName);
        DeleteResult deleteResult = transMbCollection.deleteMany(Filters.and(Filters.eq("parent_trans_id", parentTransId)));
        return deleteResult.getDeletedCount() > 0;
    }

    @Override
    public boolean updateTransactionNeedCancel(String transId, boolean needCancel) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transCollection = database.getCollection(transCollectionName);
        Document updateDocument = new Document();
        updateDocument.append("$set", new Document().append("need_cancel", needCancel));
        UpdateResult updateResult = transCollection.updateOne(Filters.and(Filters.eq("trans_id", transId)), updateDocument);
        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public boolean updateTransactionNeedCancelAndStatus(String transId, boolean needCancel, int status) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transCollection = database.getCollection(transCollectionName);
        Document updateDocument = new Document();
        updateDocument.append("$set", new Document().append("need_cancel", needCancel).append("trans_status", status));
        UpdateResult updateResult = transCollection.updateOne(Filters.and(Filters.eq("trans_id", transId)), updateDocument);
        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public boolean updateTransactionNeedSuccess(String transId, boolean needSuccess) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transCollection = database.getCollection(transCollectionName);
        Document updateDocument = new Document();
        updateDocument.append("$set", new Document().append("need_success", needSuccess));
        UpdateResult updateResult = transCollection.updateOne(Filters.and(Filters.eq("trans_id", transId)), updateDocument);
        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public boolean updateTransactionNeedSuccessAndStatus(String transId, boolean needSuccess, int status) {
        MongoDatabase database = mongoClient.getDatabase(mongodbConfig.getDatabase());
        MongoCollection<Document> transCollection = database.getCollection(transCollectionName);
        Document updateDocument = new Document();
        updateDocument.append("$set", new Document().append("need_success", needSuccess).append("trans_status", status));
        UpdateResult updateResult = transCollection.updateOne(Filters.and(Filters.eq("trans_id", transId)), updateDocument);
        return updateResult.getModifiedCount() > 0;
    }

    private TransactionInfo getTransactionInfoByDocument(Document document, List<String> excludeFields) {
        return this.<TransactionInfo>getInstanceByDocument(TransactionInfo.class, document, excludeFields);
    }

    private TransactionMember getTransactionMemberByDocument(Document document, List<String> excludeFields) {
        return this.<TransactionMember>getInstanceByDocument(TransactionMember.class, document, excludeFields);
    }

    private Document getDocumentByTransactionInfo(TransactionInfo transactionInfo, List<String> excludeFields) {
        return this.<TransactionInfo>getDocumentByInstace(transactionInfo, excludeFields);
    }

    private Document getDocumentByTransactionMember(TransactionMember transactionMember, List<String> excludeFields) {
        return this.<TransactionMember>getDocumentByInstace(transactionMember, excludeFields);
    }

    private <T> T getInstanceByDocument(Class<?> clazz, Document document, List<String> excludeFields) {
        T instace = null;
        try {
            instace = (T) clazz.getConstructor().newInstance();
        } catch (InstantiationException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        Field[] fields = ReflectUtil.getObjectAllFields(clazz);
        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();
            if (!excludeFields.contains(fieldName)) {
                String underlineName = CommonUtil.camelToUnderline(fieldName, true);
                if (Set.class.isAssignableFrom(field.getType())) {
                    try {
                        field.set(instace, Set.copyOf((List) document.get(underlineName)));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else if (List.class.isAssignableFrom(field.getType())) {
                    try {
                        field.set(instace, (List) document.get(underlineName));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        Object value = document.get(underlineName);
                        if ((byte.class.equals(field.getType()))) {
                            field.setByte(instace, Byte.parseByte(value.toString()));
                        } else if (short.class.equals(field.getType())) {
                            field.setShort(instace, Short.parseShort(value.toString()));
                        } else if (char.class.equals(field.getType())) {
                            field.setChar(instace, value.toString().charAt(0));
                        } else if (int.class.equals(field.getType())) {
                            field.setInt(instace, Integer.parseInt(value.toString()));
                        } else if (long.class.equals(field.getType())) {
                            field.setLong(instace, Long.parseLong(value.toString()));
                        } else if (float.class.equals(field.getType())) {
                            field.setFloat(instace, Float.parseFloat(value.toString()));
                        } else if (double.class.equals(field.getType())) {
                            field.setDouble(instace, Double.parseDouble(value.toString()));
                        } else if (boolean.class.equals(field.getType())) {
                            field.setBoolean(instace, Boolean.parseBoolean(value.toString()));
                        } else {
                            field.set(instace, value);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return instace;
    }

    private <T> Document getDocumentByInstace(T instace, List<String> excludeFields) {
        Document document = new Document();
        Field[] allFields = ReflectUtil.getObjectAllFields(instace.getClass());
        for (Field field : allFields) {
            field.setAccessible(true);
            String name = field.getName();
            if (!excludeFields.contains(name)) {
                String underlineName = CommonUtil.camelToUnderline(name, true);
                if (Set.class.isAssignableFrom(field.getType())) {
                    try {
                        document.append(underlineName, List.copyOf((Set) field.get(instace)));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        document.append(underlineName, field.get(instace));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return document;
    }

}
