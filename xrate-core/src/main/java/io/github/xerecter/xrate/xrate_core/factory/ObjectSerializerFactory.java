package io.github.xerecter.xrate.xrate_core.factory;

import io.github.xerecter.xrate.xrate_core.constants.CommonConstants;
import io.github.xerecter.xrate.xrate_core.service.impl.JavaObjectSerializerImpl;
import io.github.xerecter.xrate.xrate_core.service.impl.KryoObjectSerializerImpl;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * 对象对序列化服务工厂
 *
 * @author xdd
 */
public class ObjectSerializerFactory {

    /**
     * 根据序列化方式生成对应的BeanDefinition
     *
     * @param way 方式
     * @return BeanDefinition
     */
    public static AbstractBeanDefinition getObjectSerializerServiceDefinition(String way) {
        if (CommonConstants.KYRO_SERIALIZER_WAY.equalsIgnoreCase(way)) {
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(KryoObjectSerializerImpl.class);
            return beanDefinitionBuilder.getBeanDefinition();
        } else if (CommonConstants.JAVA_SERIALIZER_WAY.equalsIgnoreCase(way)) {
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(JavaObjectSerializerImpl.class);
            return beanDefinitionBuilder.getBeanDefinition();
        } else {
            BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(KryoObjectSerializerImpl.class);
            return beanDefinitionBuilder.getBeanDefinition();
        }
    }

}
