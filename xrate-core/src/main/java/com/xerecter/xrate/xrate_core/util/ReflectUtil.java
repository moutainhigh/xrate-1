package com.xerecter.xrate.xrate_core.util;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class ReflectUtil {

    /**
     * 获取类上面的注解
     *
     * @param clazz           类
     * @param annotationClass 注解class
     * @param <T>             泛型
     * @return 注解
     */
    public static <T extends Annotation> T getAnnotation(Class clazz, Class<T> annotationClass) {
        if (Annotation.class.isAssignableFrom(annotationClass)) {
            Annotation annotation = getAnnotationByClass(clazz, annotationClass);
            if (annotation == null) {
                return (T) getAnnotationByInterface(clazz, annotationClass);
            }
            return (T) annotation;
        } else {
            throw new IllegalArgumentException("class is not annotation class");
        }
    }

    /**
     * 只检查类上面的注解
     *
     * @param clazz           class
     * @param annotationClass 注解class
     * @return 注解
     */
    public static Annotation getAnnotationByClass(Class clazz, Class<?> annotationClass) {
        Annotation annotation = clazz.getAnnotation(annotationClass);
        if (annotation == null) {
            if (clazz.getSuperclass() == null) {
                return null;
            } else {
                return getAnnotationByClass(clazz.getSuperclass(), annotationClass);
            }
        } else {
            return annotation;
        }
    }

    /**
     * 只检测类接口上面的注解
     *
     * @param clazz           class
     * @param annotationClass 注解class
     * @return 注解
     */
    public static Annotation getAnnotationByInterface(Class clazz, Class<?> annotationClass) {
        Annotation annotation = clazz.getAnnotation(annotationClass);
        if (annotation == null) {
            Class[] interfaces = clazz.getInterfaces();
            for (Class anInterface : interfaces) {
                return getAnnotationByInterface(anInterface, annotationClass);
            }
            return null;
        } else {
            return annotation;
        }
    }

    /**
     * 获取类的所有属性
     *
     * @param clazz class
     * @return 属性
     */
    public static Field[] getObjectAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        while (clazz != null && !(clazz.equals(Object.class))) {
            Field[] declaredFields = clazz.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                fields.add(declaredField);
            }
            clazz = clazz.getSuperclass();
        }
        return CommonUtil.getArrayByList(Field.class, fields);
    }

    /**
     * 获取属性值
     *
     * @param fieldNameExpression 熟悉表达式
     * @param obj                 对象
     * @return 值
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
    public static <T> T getFieldValue(
            String fieldNameExpression,
            Object obj
    ) throws NoSuchFieldException, IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        String[] splits = fieldNameExpression.split("\\.");
        if (splits.length == 0) {
            throw new IllegalArgumentException("field expression error");
        }
        for (String split : splits) {
            Field field = getFieldByName(obj.getClass(), split);
            field.setAccessible(true);
            obj = field.get(obj);
        }
        return (T) obj;
    }

    /**
     * 设置属性值
     *
     * @param instance        对象
     * @param value           值
     * @param fieldExpression 属性表达式
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    public static void setFieldValue(Object instance, Object value, String fieldExpression) throws NoSuchFieldException, IllegalAccessException {
        String[] split = fieldExpression.split("\\.");
        for (int i = 0; i < split.length - 1; i++) {
            Field field = getFieldByName(instance.getClass(), split[i]);
            field.setAccessible(true);
            instance = field.get(instance);
        }
        Field field = getFieldByName(instance.getClass(), split[split.length - 1]);
        field.setAccessible(true);
        field.set(instance, value);
    }

    /**
     * 获取类型默认值
     *
     * @param clazz 类型
     * @return 值
     */
    public static Object getClassDefaultValue(Class<?> clazz) {
        if (byte.class.equals(clazz)) {
            return ((byte) 0);
        } else if (Byte.class.equals(clazz)) {
            return Byte.valueOf((byte) 0);
        } else if (short.class.equals(clazz)) {
            return ((short) 0);
        } else if (Short.class.equals(clazz)) {
            return Short.valueOf((short) 0);
        } else if (int.class.equals(clazz)) {
            return (0);
        } else if (Integer.class.equals(clazz)) {
            return Integer.valueOf(0);
        } else if (long.class.equals(clazz)) {
            return (0L);
        } else if (Long.class.equals(clazz)) {
            return Long.valueOf(0L);
        } else if (float.class.equals(clazz)) {
            return (0f);
        } else if (Float.class.equals(clazz)) {
            return Float.valueOf(0f);
        } else if (double.class.equals(clazz)) {
            return (0d);
        } else if (Double.class.equals(clazz)) {
            return Double.valueOf(0d);
        } else if (boolean.class.equals(clazz)) {
            return (false);
        } else if (Boolean.class.equals(clazz)) {
            return Boolean.valueOf(false);
        } else if (char.class.equals(clazz)) {
            return '\u0000';
        } else if (Character.class.equals(clazz)) {
            return Character.valueOf('\u0000');
        } else {
            return null;
        }
    }

    /**
     * 判断类型是否为基本数据类型或者字符串
     *
     * @param clazz          类型
     * @param canBeReference 是否运行引用类型
     * @param canBeString    是否运行引用类型
     * @return 是否为基本数据类型或者字符串
     */
    public static Boolean isBasicTypeOrStringType(
            Class clazz,
            boolean canBeReference,
            boolean canBeString
    ) {
        if (clazz.equals(int.class) || clazz.equals(byte.class) || clazz.equals(long.class) || clazz.equals(double.class) || clazz.equals(float.class) || clazz.equals(char.class) || clazz.equals(short.class) || clazz.equals(boolean.class)) {
            return true;
        }
        if (canBeString && String.class.equals(clazz)) {
            return true;
        }
        if (canBeReference) {
            return clazz.equals(Integer.class) || clazz.equals(Byte.class) || clazz.equals(Long.class) || clazz.equals(Double.class) || clazz.equals(Float.class) || clazz.equals(Character.class) || clazz.equals(Short.class) || clazz.equals(Boolean.class);
        }
        return false;
    }

    /**
     * 通过名称获取属性
     *
     * @param clazz               class
     * @param fieldNameExpression 属性名字表达式
     * @return 属性
     * @throws NoSuchFieldException
     */
    public static Field getFieldByName(Class<?> clazz, String fieldNameExpression) throws NoSuchFieldException {
        Field finalField = null;
        String[] names = fieldNameExpression.split("\\.");
        if (names.length == 0) {
            throw new IllegalArgumentException("field expression error");
        }
        for (String name : names) {
            Field[] objectAllFields = getObjectAllFields(clazz);
            finalField = null;
            for (Field field : objectAllFields) {
                if (name.equals(field.getName())) {
                    clazz = field.getType();
                    finalField = field;
                    break;
                }
            }
            if (finalField == null) {
                throw new NoSuchFieldException();
            }
        }
        return finalField;
    }

    /**
     * 获取对象的class
     *
     * @param objects 对象
     * @return classes
     */
    public static Class<?>[] getObjectClass(Object... objects) {
        Class<?>[] classes = new Class[objects.length];
        for (int i = 0; i < objects.length; i++) {
            classes[i] = objects[i].getClass();
        }
        return classes;
    }

    /**
     * 根据系统路径获取类
     *
     * @param packageName 包路径
     * @param path        路径
     * @param classes     用来返回的集合
     * @param recursive   是否递归获取
     * @return 返回获取到的类
     * @throws ClassNotFoundException 异常
     */
    public static List<Class> getAllClassesByPath(
            final String packageName,
            final String path,
            final List<Class> classes,
            final boolean recursive
    ) throws ClassNotFoundException {
        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            File[] listFiles = file.listFiles((file2) -> (file2.isDirectory() && recursive) || (file2.getName().endsWith("class")));
            for (File listFile : listFiles) {
                if (listFile.isDirectory()) {
                    getAllClassesByPath(String.join(".", packageName, listFile.getName()), listFile.getAbsolutePath(), classes, recursive);
                } else {
                    classes.add(Class.forName(String.join(".", packageName, listFile.getName().substring(0, listFile.getName().length() - 6))));
                }
            }
        }
        return classes;
    }

    /**
     * 根据包名获取下面所有的类
     *
     * @param packageName 包名
     * @param recursive   是否获取递增获取
     * @return 获取到的类型
     * @throws IOException            异常
     * @throws ClassNotFoundException 异常
     */
    public static List<Class> getAllClassesByPackageName(
            final String packageName,
            final boolean recursive
    ) throws IOException, ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        String packageNameDir = packageName.replace(".", "/");
        Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(packageNameDir);
        while (resources.hasMoreElements()) {
            URL url = resources.nextElement();
            String protocol = url.getProtocol();
            if (protocol.equals("file")) {
                String decodeUrl = URLDecoder.decode(url.getFile(), StandardCharsets.UTF_8.toString());
                getAllClassesByPath(packageName, decodeUrl, classes, recursive);
            } else if (protocol.equals("jar")) {
                JarFile jarFile = ((JarURLConnection) url.openConnection()).getJarFile();
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry jarEntry = entries.nextElement();
                    String name = jarEntry.getName();
                    if (name.startsWith(packageNameDir)) {
                        if (name.endsWith("class")) {
                            if (recursive) {
                                name = name.replace("/", ".").substring(0, name.lastIndexOf("."));
                                classes.add(Class.forName(name));
                            } else {
                                if (name.substring(0, name.lastIndexOf("/")).equals(packageNameDir)) {
                                    name = name.replace("/", ".").substring(0, name.lastIndexOf("."));
                                    classes.add(Class.forName(name));
                                }
                            }
                        }
                    }
                }
            }
        }
        return classes;
    }

    /**
     * 通过类型名称获取类型
     *
     * @param names 名称
     * @return 类型
     * @throws ClassNotFoundException
     */
    public static Class<?>[] getClassesByNames(String... names) throws ClassNotFoundException {
        Class<?>[] classes = new Class[names.length];
        for (int i = 0; i < classes.length; i++) {
            classes[i] = Class.forName(names[i]);
        }
        return classes;
    }

    /**
     * 通过类型获取名称
     *
     * @param classes 类型
     * @return 名称
     */
    public static String[] getClassNamesByClasses(Class<?>... classes) {
        String[] names = new String[classes.length];
        for (int i = 0; i < classes.length; i++) {
            names[i] = classes[i].getName();
        }
        return names;
    }

}
