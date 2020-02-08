import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.PackageConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;

public class TestMain {

    public static void main(String[] args) {
// 代码生成器
        AutoGenerator mpg = new AutoGenerator();

        // 全局配置
        GlobalConfig gc = new GlobalConfig();
        gc.setOutputDir("/home/xdd/code_dev/idea/xrate/xrate-dubbo-demo/merchant/src/main/java");
        gc.setAuthor("xdd");
        gc.setOpen(false);
        // gc.setSwagger2(true); 实体属性 Swagger2 注解
        mpg.setGlobalConfig(gc);

        // 数据源配置
        DataSourceConfig dsc = new DataSourceConfig();
        dsc.setUrl("jdbc:mysql://localhost:3306/xrate?useUnicode=true&useSSL=false&characterEncoding=utf8");
        // dsc.setSchemaName("public");
//        dsc.setDriverName("com.mysql.jdbc.Driver");
        dsc.setUsername("root");
        dsc.setPassword("admin");
        dsc.setDriverName("com.mysql.cj.jdbc.Driver");
        dsc.setDbType(DbType.MYSQL);
        dsc.setTypeConvert((globalConfig, fieldType) -> {
            String t = fieldType.toLowerCase();
            if (t.contains("char")) {
                return DbColumnType.STRING;
            } else if (t.contains("bigint")) {
                return DbColumnType.BASE_LONG;
            } else if (t.contains("tinyint(1)")) {
                return DbColumnType.BASE_BYTE;
            } else if (t.contains("int")) {
                return DbColumnType.BASE_INT;
            } else if (t.contains("text")) {
                return DbColumnType.STRING;
            } else if (t.contains("bit")) {
                return DbColumnType.BOOLEAN;
            } else if (t.contains("decimal")) {
                return DbColumnType.BASE_DOUBLE;
            } else if (t.contains("clob")) {
                return DbColumnType.CLOB;
            } else if (t.contains("blob")) {
                return DbColumnType.BLOB;
            } else if (t.contains("binary")) {
                return DbColumnType.BYTE_ARRAY;
            } else if (t.contains("float")) {
                return DbColumnType.BASE_FLOAT;
            } else if (t.contains("double")) {
                return DbColumnType.BASE_DOUBLE;
            } else if (t.contains("json") || t.contains("enum")) {
                return DbColumnType.STRING;
            } else if (t.contains("date") || t.contains("time") || t.contains("year")) {
                switch (globalConfig.getDateType()) {
                    case ONLY_DATE:
                        return DbColumnType.DATE;
                    case SQL_PACK:
                        switch (t) {
                            case "date":
                                return DbColumnType.DATE_SQL;
                            case "time":
                                return DbColumnType.TIME;
                            case "year":
                                return DbColumnType.DATE_SQL;
                            default:
                                return DbColumnType.TIMESTAMP;
                        }
                    case TIME_PACK:
                        switch (t) {
                            case "date":
                                return DbColumnType.LOCAL_DATE;
                            case "time":
                                return DbColumnType.LOCAL_TIME;
                            case "year":
                                return DbColumnType.YEAR;
                            default:
                                return DbColumnType.LOCAL_DATE_TIME;
                        }
                }
            }
            return DbColumnType.STRING;
        });
        mpg.setDataSource(dsc);

        // 包配置
        PackageConfig pc = new PackageConfig();
        pc.setParent("com.xerecter.xrate_dubbo_demo.merchant");
        pc.setEntity("entity");
//        pc.setEntity("pojo");
        mpg.setPackageInfo(pc);

        // 策略配置
        StrategyConfig strategy = new StrategyConfig();
        strategy.setRestControllerStyle(true);
        strategy.setSkipView(true);
        strategy.setEntityTableFieldAnnotationEnable(true);
        strategy.setNaming(NamingStrategy.underline_to_camel);
        strategy.setColumnNaming(NamingStrategy.underline_to_camel);
        strategy.setEntityBooleanColumnRemoveIsPrefix(false);
        //需要包含那些表，也就是说只有这些表生成service mapper controller
//        strategy.setInclude("",
//                "",
//                "",
//                "",
//                "",
//                ""
//        );
        mpg.setStrategy(strategy);

        //生成
        mpg.execute();
    }

}
