package com.xerecter.xrate.xrate_core.util;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoField;
import java.util.Date;

public class DateUtil {

    /**
     * 时间字符串转时间
     *
     * @param str 字符串
     * @return date
     */
    public static Date parseDate(String str) {
        Date date = null;
        try {
            date = localDateTimeToDate(LocalDateTime.parse(str, FormatUtil.dateTimeFormatter.get()));
            return date;
        } catch (Exception ignored) {
        }

        try {
            date = localDateTimeToDate(LocalDate.parse(str, FormatUtil.dateTimeFormatterSimple.get()).atStartOfDay());
            return date;
        } catch (Exception ignored) {
        }
        return date;
    }

    /**
     * LocalDateTime转Date
     *
     * @param localDateTime localDateTime
     * @return date
     */
    public static Date localDateTimeToDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Date转LocalDateTime
     *
     * @param date date
     * @return LocalDateTime
     */
    public static LocalDateTime dateToLocalDateTime(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
    }


    /**
     * 把时间格式化成springCron
     *
     * @param date 时间
     * @return springCron
     */
    public static String parseCron(Date date) {
        String formatTimeStr = "";
        if (date != null) {
            formatTimeStr = FormatUtil.formatDate(date);
        }
        return formatTimeStr;
    }

    /**
     * 获取当前时间所在星期的星期一
     *
     * @return 当前时间所在星期的星期一
     */
    public static Date getCurrentWeekFirstDate() {
        LocalDateTime nowLocalDateTime = LocalDateTime.now();
        nowLocalDateTime = nowLocalDateTime.minusDays(nowLocalDateTime.getDayOfWeek().ordinal());
        LocalDateTime atStartOfDay = nowLocalDateTime.toLocalDate().atStartOfDay();
        return Date.from(atStartOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取当前时间所在星期的星期日
     *
     * @return 当前时间所在星期的星期日
     */
    public static Date getCurrentWeekEndDate() {
        LocalDateTime nowLocalDateTime = LocalDateTime.now();
        return getLocalDateTimeWeekEndDate(nowLocalDateTime);
    }

    /**
     * 获取日期所在星期的星期日
     *
     * @param nowLocalDateTime 日期
     * @return 日期所在星期的星期日
     */
    private static Date getLocalDateTimeWeekEndDate(LocalDateTime nowLocalDateTime) {
        nowLocalDateTime = nowLocalDateTime.plusDays(7 - nowLocalDateTime.getDayOfWeek().getValue());
        LocalDateTime atStartOfDay = nowLocalDateTime.toLocalDate().atStartOfDay().plusSeconds(LocalTime.MAX.getLong(ChronoField.SECOND_OF_DAY));
        return Date.from(atStartOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取日期所在星期的星期日
     *
     * @param date 日期
     * @return 日期所在星期的星期日
     */
    public static Date getDateWeekEndDate(Date date) {
        LocalDateTime nowLocalDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        return getLocalDateTimeWeekEndDate(nowLocalDateTime);
    }

    /**
     * 获取日期所在星期的星期一
     *
     * @param date 日期
     * @return 日期所在星期的星期一
     */
    public static Date getDateWeekFirstDate(Date date) {
        LocalDateTime nowLocalDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        nowLocalDateTime = nowLocalDateTime.minusDays(nowLocalDateTime.getDayOfWeek().ordinal());
        LocalDateTime atStartOfDay = nowLocalDateTime.toLocalDate().atStartOfDay();
        return Date.from(atStartOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }


    /**
     * 获取当前日期所在星期的星期三
     *
     * @return 当前日期所在星期的星期三
     */
    public static Date getCurrentWeekWednesday() {
        LocalDateTime nowLocalDateTime = LocalDateTime.now();
        int rangeValue = 3 - nowLocalDateTime.getDayOfWeek().getValue();
        if (rangeValue < 0) {
            nowLocalDateTime = nowLocalDateTime.minusDays(-rangeValue);
        } else {
            nowLocalDateTime = nowLocalDateTime.plusDays(rangeValue);
        }
        LocalDateTime atStartOfDay = nowLocalDateTime.toLocalDate().atStartOfDay();
        return Date.from(atStartOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 获取日期所在星期的星期三
     *
     * @return 日期所在星期的星期三
     */
    public static Date getDateWeekWednesday(Date date) {
        LocalDateTime nowLocalDateTime = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
        int rangeValue = 3 - nowLocalDateTime.getDayOfWeek().getValue();
        if (rangeValue < 0) {
            nowLocalDateTime = nowLocalDateTime.minusDays(-rangeValue);
        } else {
            nowLocalDateTime = nowLocalDateTime.plusDays(rangeValue);
        }
        LocalDateTime atStartOfDay = nowLocalDateTime.toLocalDate().atStartOfDay();
        return Date.from(atStartOfDay.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * 验证时间字符串是否正确
     *
     * @param date 时间字符串
     * @return 是否正确
     */
    public static boolean verifyDate(String date) {
        try {
            LocalDateTime.parse(date, FormatUtil.dateTimeFormatter.get());
            return true;
        } catch (Exception ignored) {
        }
        try {
            LocalDate.parse(date, FormatUtil.dateTimeFormatterSimple.get());
            return true;
        } catch (Exception ignored) {
        }
        return false;
    }

}
