package com.xerecter.xrate.xrate_core.annotation;

import java.lang.annotation.*;

////////////////////////////////////////////////////////////////////
//                            _ooOoo_                                  //
//                           o8888888o                              //
//                           88" . "88                              //
//                           (| ^_^ |)                              //
//                           O\  =  /O                              //
//                        ____/`---'\____                              //
//                      .'  \\|     |//  `.                          //
//                     /  \\|||  :  |||//  \                          //
//                    /  _||||| -:- |||||-  \                          //
//                    |   | \\\  -  /// |   |                          //
//                    | \_|  ''\---/''  |   |                          //
//                    \  .-\__  `-`  ___/-. /                          //
//                  ___`. .'  /--.--\  `. . ___                      //
//                ."" '<  `.___\_<|>_/___.'  >'"".                  //
//              | | :  `- \`.;`\ _ /`;.`/ - ` : | |                  //
//              \  \ `-.   \_ __\ /__ _/   .-` /  /                 //
//        ========`-.____`-.___\_____/___.-`____.-'========          //
//                             `=---='                              //
//        ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^        //
//         佛祖保佑       永无BUG        永不修改                  //
////////////////////////////////////////////////////////////////////

/**
 * @author xdd
 */
@Retention(value = RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@Inherited
public @interface XrateTransaction {

    String value();

    /**
     * @return cancel 方法名称
     */
    String cancelMethod() default "";

    /**
     * @return 回调类型
     */
    InvokeEnum asyncInvoke() default InvokeEnum.NONE;

    /**
     * @return 重试次数
     */
    int retryTimes() default -1;

    /**
     * @return 重试间隔
     */
    int retryInterval() default -1;

    /**
     * 回调类型
     */
    public static enum InvokeEnum {
        // 异步
        ASYNC,
        // 同步
        SYNC,
        // 无
        NONE,
        ;
    }

}