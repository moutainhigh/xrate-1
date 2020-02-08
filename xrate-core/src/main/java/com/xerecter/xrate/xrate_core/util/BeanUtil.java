package com.xerecter.xrate.xrate_core.util;

import org.springframework.context.ApplicationContext;

public class BeanUtil {

    private static ApplicationContext applicationContext;

    public static void setSpringCtx(ApplicationContext applicationContext) {
        BeanUtil.applicationContext = applicationContext;
    }

    public static ApplicationContext getSpringCtx() {
        return applicationContext;
    }

}
