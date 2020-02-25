package io.github.xerecter.xrate.xrate_core.util;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class FormatUtil {

    public static ThreadLocal<DateTimeFormatter> dateTimeFormatter = ThreadLocal.withInitial(() -> DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

    public static ThreadLocal<DateTimeFormatter> dateTimeFormatterSimple = ThreadLocal.withInitial(() -> DateTimeFormatter.ofPattern("yyyy-MM-dd"));

    public static ThreadLocal<DecimalFormat> decimalFormat = ThreadLocal.withInitial(() -> new DecimalFormat("######0.00"));

    /**
     * double保留两位小数
     *
     * @param d 数字
     * @return 保留两位小数后的数字
     */
    public static double formatDouble(double d) {
        String format = decimalFormat.get().format(d);
        return Double.parseDouble(format);
    }

    /**
     * 格式化时间
     *
     * @param date 时间
     * @return 格式化后的字符串
     */
    public static String formatDate(Date date) {
        LocalDateTime localDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return dateTimeFormatter.get().format(localDateTime);
    }

}
