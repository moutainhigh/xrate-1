package com.xerecter.xrate.xrate_core.util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.xerecter.xrate.xrate_core.annotation.XrateTransaction;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.util.Assert;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {

    private static Map<String, String> camelToUnderlineCache = new ConcurrentHashMap<>();

    private static Map<String, String> underlineToCamelCache = new ConcurrentHashMap<>();

    /**
     * 生成随机数 最多为9位
     *
     * @return 随机数长度
     */
    public static String generateRandomNumber(int LENGTH) {
        String no = "";
        int[] defaultNums = new int[10];
        for (int i = 0; i < defaultNums.length; i++) {
            defaultNums[i] = i;
        }

        Random random = new Random();
        int[] nums = new int[LENGTH];
        int canBeUsed = 10;
        for (int i = 0; i < nums.length; i++) {
            int index = random.nextInt(canBeUsed);
            nums[i] = defaultNums[index];
            swap(index, canBeUsed - 1, defaultNums);
            canBeUsed--;
        }
        if (nums.length > 0) {
            for (int i = 0; i < nums.length; i++) {
                no += nums[i];
            }
        }

        return no;
    }

    private static void swap(int i, int j, int[] nums) {
        int temp = nums[i];
        nums[i] = nums[j];
        nums[j] = temp;
    }

    /**
     * 获取 cr 符号的 num 倍
     *
     * @param cr
     * @param num
     * @return
     */
    public static String getCharByNum(String cr, long num) {
        String ncr = "";
        for (int i = 0; i < num; i++) {
            ncr += cr;
        }
        return ncr;
    }

    /**
     * 补齐0到数字上
     *
     * @param num    需要补零的数字
     * @param length 整体长度
     * @return 补齐后的数据
     */
    public static String supplementZeroToNum(long num, long length) {
        if (String.valueOf(num).length() < length) {
            StringBuffer stringBuffer = new StringBuffer();
            long temp = length - String.valueOf(num).length();
            for (int i = 0; i < temp; i++) {
                stringBuffer.append("0");
            }
            stringBuffer.insert(stringBuffer.length(), num);
            return stringBuffer.toString();
        } else {
            return String.valueOf(num);
        }
    }

    /**
     * 把时间格式化成springCron
     *
     * @param date
     * @return
     */
    public static String parseCron(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("ss mm HH dd MM ? yyyy");
        String formatTimeStr = "";
        if (date != null) {
            formatTimeStr = sdf.format(date);
        }
        return formatTimeStr;
    }

    /**
     * 封装的fastjson序列化方法
     *
     * @param object
     * @return
     */
    public static String parseObjectToJSON(Object object) {
        return parseObjectToJSON(object, true, false);
    }

    /**
     * 封装的fastjson序列化方法
     *
     * @param object
     * @return
     */
    public static String parseObjectToJSON(Object object, boolean dateFormat) {
        return parseObjectToJSON(object, dateFormat, false);
    }

    /**
     * 封装的fastjson序列化方法
     *
     * @param object
     * @return
     */
    public static String parseObjectToJSON(Object object, boolean dateFormat, boolean notNull) {
        if (notNull) {
            if (dateFormat) {
                return JSON.toJSONStringWithDateFormat(object, "yyyy-MM-dd HH:mm:ss"
                        , SerializerFeature.WriteNullStringAsEmpty
                        , SerializerFeature.WriteNullListAsEmpty
                        , SerializerFeature.WriteNullNumberAsZero
                        , SerializerFeature.WriteNullBooleanAsFalse
                        , SerializerFeature.WriteMapNullValue
                        , SerializerFeature.DisableCircularReferenceDetect
                );
            } else {
                return JSON.toJSONString(object,
                        SerializerFeature.WriteNullStringAsEmpty,
                        SerializerFeature.WriteNullListAsEmpty,
                        SerializerFeature.WriteNullNumberAsZero,
                        SerializerFeature.WriteNullBooleanAsFalse,
                        SerializerFeature.WriteMapNullValue,
                        SerializerFeature.DisableCircularReferenceDetect
                );
            }
        } else {
            if (dateFormat) {
                return JSON.toJSONStringWithDateFormat(object, "yyyy-MM-dd HH:mm:ss",
                        SerializerFeature.DisableCircularReferenceDetect
                );
            } else {
                return JSON.toJSONString(object,
                        SerializerFeature.DisableCircularReferenceDetect
                );
            }
        }
    }

    /**
     * 验证密码的难易程度 若不符合标准返回-1 密码长度在6-16之间 密码难度有三个等级 低中高 返回值分别为 0 1 2
     *
     * @param pwd
     * @return
     */
    public static int verifyPwdIntensity(String pwd) {
        String reg1 = "(^\\d{6,}$)|(^[a-zA-Z]{6,}$)|(^[^a-zA-Z0-9]{6,}$)"; //数字，字母或符号其中的一种
        String reg7 = "\\d*\\D*((\\d+[a-zA-Z]+[^0-9a-zA-Z]+)|(\\d+[^0-9a-zA-Z]+[a-zA-Z]+)|([a-zA-Z]+\\d+[^0-9a-zA-Z]+)|([a-zA-Z]+[^0-9a-zA-Z]+\\d+)|([^0-9a-zA-Z]+[a-zA-Z]+\\d+)|([^0-9a-zA-Z]+\\d+[a-zA-Z]+))\\d*\\D*"; //数字字母字符任意组合
        if (pwd.length() < 6 || pwd.length() > 16) {
            return -1;
        } else {
            if (pwd.matches(reg1)) {
                return 0;
            } else if (!pwd.matches(reg7)) {
                return 1;
            } else {
                return 2;
            }
        }
    }

    /**
     * 根据list转成对应的数组
     *
     * @param tClass 类型
     * @param tList  集合
     * @param <T>    泛型
     * @return 数组
     */
    public static <T> T[] getArrayByList(Class<T> tClass, List<T> tList) {
        return tList.toArray((T[]) Array.newInstance(tClass, tList.size()));
    }

    /**
     * 数组转list
     *
     * @param ts  数组
     * @param <T> 泛型
     * @return list
     */
    public static <T> List<T> getListByArray(T... ts) {
        return Arrays.asList(ts);
    }

    /**
     * 根据set集合转list
     *
     * @param tSet set
     * @param <T>  泛型
     * @return list
     */
    public static <T> List<T> getListBySet(Set<T> tSet) {
        List<T> list = new ArrayList<>();
        for (T t : tSet) {
            list.add(t);
        }
        return list;
    }

    /**
     * 将路径解析成map
     * stanUrl 为 /user/{userId} trueUrl /user/123456
     * 解析出来的map一对键值对 键userId值123456
     *
     * @param stanUrl
     * @param trueUrl
     * @return 对应的map
     */
    public static Map<String, String> getRestUrlQueryParams(String stanUrl, String trueUrl) {
        Map<String, String> queryParams = new HashMap<>();
        if (trueUrl.contains("://")) {
            trueUrl = trueUrl.substring(trueUrl.indexOf("/", (trueUrl.indexOf("://") + 3)));
        }
        if (stanUrl.contains("://")) {
            stanUrl = stanUrl.substring(stanUrl.indexOf("/", (stanUrl.indexOf("://") + 3)));
        }
        trueUrl = trueUrl.substring(0, trueUrl.contains("?") ? trueUrl.indexOf("?") : trueUrl.length());
        while (true) {
            int beginBigBra = stanUrl.indexOf("{");
            if (beginBigBra == -1) {
                break;
            }
            int endBigBra = stanUrl.indexOf("}", beginBigBra);
            String stanKey = stanUrl.substring(beginBigBra + 1, endBigBra);
            int idx1 = stanUrl.indexOf("{", endBigBra + 1);
            String stanKeyNext = stanUrl.substring(endBigBra + 1, idx1 == -1 ? stanUrl.length() : idx1);
            int idx2 = trueUrl.indexOf(stanKeyNext, beginBigBra);
            if (idx2 == -1) {
                break;
            }
            String trueVal = null;
            if (stanKeyNext.length() == 0) {
                trueVal = trueUrl.substring(beginBigBra);
            } else {
                trueVal = trueUrl.substring(beginBigBra, idx2);

            }
            queryParams.put(stanKey, trueVal);
            stanUrl = stanUrl.substring(endBigBra + 1);
            trueUrl = trueUrl.substring(idx2);
        }
        return queryParams;
    }

    /**
     * 将查询字符串解析成map
     *
     * @param url 路径
     * @return map
     */
    public static Map<String, String> getQueryParams(String url) {
        try {
            Map<String, String> finparams = new HashMap<String, String>();
            Map<String, List<String>> params = new HashMap<String, List<String>>();
            String[] urlParts = url.split("\\?");
            if (urlParts.length > 1) {
                String query = urlParts[1];
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
                    String value = "";
                    if (pair.length > 1) {
                        value = URLDecoder.decode(pair[1], StandardCharsets.UTF_8);
                    }

                    List<String> values = params.computeIfAbsent(key, k -> new ArrayList<>());
                    values.add(value);
                }
            }

            for (String vakey : params.keySet()) {
                StringBuilder sb = new StringBuilder();
                for (String vavalue : params.get(vakey)) {
                    sb.append(vavalue);
                }
                finparams.put(vakey, sb.toString());
            }

            return finparams;
        } catch (Exception ex) {
            throw new AssertionError(ex);
        }
    }

    /**
     * 下划线转驼峰
     */
    public static String underlineToCamel(String str, boolean needCache) {
        if (underlineToCamelCache.containsKey(str)) {
            return underlineToCamelCache.get(str);
        }
        StringBuilder stringBuilder = new StringBuilder();
        String[] strings = str.split("_");
        for (int i = 1; i < strings.length; i++) {
            String currStr = strings[i];
            if (currStr.length() > 0) {
                stringBuilder.append(String.valueOf(currStr.charAt(0)).toUpperCase());
                char[] chars = currStr.toCharArray();
                if (chars.length > 1) {
                    stringBuilder.append(String.valueOf(ArrayUtils.subarray(chars, 1, chars.length)));
                }
            }
        }
        String result = stringBuilder.toString();
        if (needCache) {
            underlineToCamelCache.putIfAbsent(str, result);
        }
        return result;
    }

    /**
     * 防止xss攻击
     *
     * @param value 对应的字符串
     * @return 处理以后的字符串
     */
    public static String stripXSS(String value) {
        if (value != null) {
            //You'll need to remove the spaces from the html entities below
            value = value.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
            value = value.replaceAll("\\(", "&#40;").replaceAll("\\)", "&#41;");
            value = value.replaceAll("'", "&#39;");
            value = value.replaceAll("eval\\((.*)\\)", "");
            value = value.replaceAll("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", "\"\"");
            value = value.replaceAll("script", "");
        }
        return value;
    }

    /**
     * 防止XSS攻击 轻度
     *
     * @param value 对应的字符串
     * @return 处理以后的字符串
     */
    public static String mildStripXSS(String value) {
        if (value != null) {
            //You'll need to remove the spaces from the html entities below
            value = value.replaceAll("eval\\((.*)\\)", "");
            value = value.replaceAll("[\\\"\\\'][\\s]*javascript:(.*)[\\\"\\\']", "\"\"");
            value = value.replaceAll("script", "");
        }
        return value;
    }

    /**
     * 校验手机号
     *
     * @param phone 手机号
     * @return 是否校验通过
     */
    public static boolean verifyPhone(String phone) {
//        return phone.matches("^((13[0-9])|(14[5|7])|(15([0-3]|[5-9]))|(18[0,5-9]))\\d{8}$");
        return phone.matches("^1[3-9]\\d{9}$");
    }

    /**
     * 校验身份证号
     *
     * @param idNumber 身份证号
     * @return 是否校验通过
     */
    public static boolean verifyIDNumber(String idNumber) {
        return idNumber.matches("^[1-9]\\d{5}(18|19|([23]\\d))\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$");
    }

    /**
     * 校验url
     *
     * @param url url
     * @return 是否校验通过
     */
    public static boolean verifyURL(String url) {
        return url.matches("[a-zA-z]+://[^\\s]*+");
    }

    /**
     * 校验ip v4地址
     *
     * @param ipv4 地址
     * @return 是否校验通过
     */
    public static boolean verifyIPV4(String ipv4) {
        return ipv4.matches("^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$");
    }

    /**
     * 校验是否有中文
     *
     * @param str 字符串
     * @return 是否校验通过
     */
    public static boolean verifyIsHaveChinese(String str) {
        return str.matches("[\\u4E00-\\u9FA5]|\\S*+");
    }

    /**
     * 验证邮箱
     *
     * @param email 邮箱地址
     * @return 是否校验通过
     */
    public static boolean verifyEmail(String email) {
        return email.matches("^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$") && email.length() <= 60;
    }

    /**
     * md5加密
     *
     * @param encryptStr
     * @return
     */
    public static String md5Hex(String encryptStr) {
        return DigestUtils.md5Hex(encryptStr).toLowerCase();
    }

    /**
     * 多次 md5加密
     *
     * @param encryptStr 需要加密的字符串
     * @param times      次数
     * @return 结果
     */
    public static String md5Hex(String encryptStr, int times) {
        String md5 = encryptStr;
        for (int i = 0; i < times; i++) {
            md5 = DigestUtils.md5Hex(md5).toLowerCase();
        }
        return md5;
    }


    /**
     * 生成多个返回值的str
     *
     * @param status 状态
     * @param result 结果
     * @return 对应的字符串结果
     */
    public static String returnMultiResult(int status, Object... result) {
        Map<String, Object> stringObjectMap = new HashMap<>();
        if (result.length < 1) return CommonUtil.parseObjectToJSON(stringObjectMap);
        stringObjectMap.put("msg", result[0]);
        stringObjectMap.put("status", status);
        for (int i = 1; i < result.length; i++) {
            stringObjectMap.put("val" + i, result[i]);
        }
        return CommonUtil.parseObjectToJSON(stringObjectMap);
    }

    /**
     * 生成错误返回值的str
     *
     * @return
     */
    public static String returnMultiResult() {
        return returnMultiResult(0, "error");
    }

    /**
     * 生成单一返回值的map
     *
     * @param status
     * @param result
     * @return
     */
    public static Map<String, Object> returnSingleResult(int status, Object result) {
        Map<String, Object> stringObjectMap = new HashMap<>();
        stringObjectMap.put("msg", result);
        stringObjectMap.put("status", status);
        return stringObjectMap;
    }

    /**
     * 获取本机内网ip
     *
     * @return 本机内网ip
     */
    public static String getHostIp() {
        try {
            Enumeration<NetworkInterface> allNetInterfaces = NetworkInterface.getNetworkInterfaces();
            while (allNetInterfaces.hasMoreElements()) {
                NetworkInterface netInterface = (NetworkInterface) allNetInterfaces.nextElement();
                Enumeration<InetAddress> addresses = netInterface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress ip = (InetAddress) addresses.nextElement();
                    if (ip != null
                            && ip instanceof Inet4Address
                            && !ip.isLoopbackAddress() //loopback地址即本机地址，IPv4的loopback范围是127.0.0.0 ~ 127.255.255.255
                            && ip.getHostAddress().indexOf(":") == -1) {
                        return ip.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 驼峰命名转下划线
     *
     * @param str 字符串
     * @return 结果
     */
    public static String camelToUnderline(String str, boolean needCache) {
        if (camelToUnderlineCache.containsKey(str)) {
            return camelToUnderlineCache.get(str);
        }
        str = str.replaceAll("-", "_");
        char[] chars = str.toCharArray();
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < chars.length; i++) {
            if (chars[i] == 'A') {
                stringBuilder.append('_')
                        .append('a');
            } else if (chars[i] == 'B') {
                stringBuilder.append('_')
                        .append('b');
            } else if (chars[i] == 'C') {
                stringBuilder.append('_')
                        .append('c');
            } else if (chars[i] == 'D') {
                stringBuilder.append('_')
                        .append('d');
            } else if (chars[i] == 'E') {
                stringBuilder.append('_')
                        .append('e');
            } else if (chars[i] == 'F') {
                stringBuilder.append('_')
                        .append('f');
            } else if (chars[i] == 'G') {
                stringBuilder.append('_')
                        .append('g');
            } else if (chars[i] == 'H') {
                stringBuilder.append('_')
                        .append('h');
            } else if (chars[i] == 'I') {
                stringBuilder.append('_')
                        .append('i');
            } else if (chars[i] == 'J') {
                stringBuilder.append('_')
                        .append('j');
            } else if (chars[i] == 'K') {
                stringBuilder.append('_')
                        .append('k');
            } else if (chars[i] == 'L') {
                stringBuilder.append('_')
                        .append('l');
            } else if (chars[i] == 'M') {
                stringBuilder.append('_')
                        .append('m');
            } else if (chars[i] == 'N') {
                stringBuilder.append('_')
                        .append('n');
            } else if (chars[i] == 'O') {
                stringBuilder.append('_')
                        .append('o');
            } else if (chars[i] == 'P') {
                stringBuilder.append('_')
                        .append('p');
            } else if (chars[i] == 'Q') {
                stringBuilder.append('_')
                        .append('q');
            } else if (chars[i] == 'R') {
                stringBuilder.append('_')
                        .append('r');
            } else if (chars[i] == 'S') {
                stringBuilder.append('_')
                        .append('s');
            } else if (chars[i] == 'T') {
                stringBuilder.append('_')
                        .append('t');
            } else if (chars[i] == 'U') {
                stringBuilder.append('_')
                        .append('u');
            } else if (chars[i] == 'V') {
                stringBuilder.append('_')
                        .append('v');
            } else if (chars[i] == 'W') {
                stringBuilder.append('_')
                        .append('w');
            } else if (chars[i] == 'X') {
                stringBuilder.append('_')
                        .append('x');
            } else if (chars[i] == 'Y') {
                stringBuilder.append('_')
                        .append('y');
            } else if (chars[i] == 'Z') {
                stringBuilder.append('_')
                        .append('z');
            } else {
                stringBuilder.append(chars[i]);
            }
        }
        String result = stringBuilder.toString();
        if (needCache) {
            camelToUnderlineCache.putIfAbsent(str, result);
        }
        return result;
    }

}
