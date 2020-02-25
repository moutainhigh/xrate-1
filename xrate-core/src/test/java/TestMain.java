
import com.google.common.collect.Lists;
import com.xerecter.xrate.xrate_core.entity.*;
import com.xerecter.xrate.xrate_core.service.impl.MySQLTransactionInfoServiceImpl;
import com.xerecter.xrate.xrate_core.service.impl.TransactionExecuterServiceImpl;
import com.xerecter.xrate.xrate_core.util.SnowflakeKeyGenerator;

public class TestMain extends TestClass implements TestInterface {

    private String name;

    public static void main(String[] args) {
        XrateConfig xrateConfig = new XrateConfig();
        xrateConfig.setServiceName("test82");
        MySQLConfig mySQLConfig = new MySQLConfig();
        mySQLConfig.setUsername("root");
        mySQLConfig.setPassword("admin");
        mySQLConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        mySQLConfig.setUrl("jdbc:mysql://127.0.0.1:3306/xrate_2?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=UTC&autoReconnect=true&failOverReadOnly=false");
        MySQLTransactionInfoServiceImpl mySQLTransactionInfoService = new MySQLTransactionInfoServiceImpl(xrateConfig, mySQLConfig);
        TransactionExecuterServiceImpl transactionExecuteService = new TransactionExecuterServiceImpl();
        transactionExecuteService.setTransactionInfoService(mySQLTransactionInfoService);
        for (int i = 0; i < 8; i++) {
            new Thread(() -> {
                TransactionInfo transactionInfo = new TransactionInfo();
                String transId = String.valueOf(SnowflakeKeyGenerator.getInstance().generateKey().longValue());
                transactionInfo.setTransId(transId);
                transactionInfo.setIsStart(false);
                transactionInfo.setHoldServiceId("");
                transactionInfo.setTransStatus(0);
                transactionInfo.setNeedCancel(false);
                transactionInfo.setNeedSuccess(false);
                transactionInfo.setTryName("");
                transactionInfo.setCancelName("");
                transactionInfo.setBeanClassName("");
                transactionInfo.setParamClassNames(Lists.newArrayList());
                transactionInfo.setParams(new byte[0]);
                transactionInfo.setResult(new byte[0]);
                transactionInfo.setTransactionMembers(Lists.newArrayList());
                TransactionMember transactionMember = new TransactionMember();
                transactionMember.setParentTransId(transId);
                transactionMember.setTransId(transId + "-1");
                transactionMember.setAddress("");
                transactionMember.setTryName("");
                transactionMember.setMemberClassName("");
                transactionMember.setParamClassNames(Lists.newArrayList());
                transactionMember.setParams(new byte[0]);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mySQLTransactionInfoService.addTransactionInfo(transactionInfo);
                mySQLTransactionInfoService.addTransactionMember(transactionMember);
                mySQLTransactionInfoService.getSimpleTransactionInfo(transId, "");
                mySQLTransactionInfoService.getSimpleTransactionMembers(transId);
                mySQLTransactionInfoService.removeTransactionInfo(transId);
                mySQLTransactionInfoService.removeTransactionMembers(transId);
                transactionExecuteService.removeTransactionAndMembers(transactionInfo);
                System.out.println("-->");
            }).start();
        }
        System.out.println("11");
    }

}
