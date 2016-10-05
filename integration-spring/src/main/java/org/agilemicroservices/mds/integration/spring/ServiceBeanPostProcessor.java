package org.agilemicroservices.mds.integration.spring;

import org.agilemicroservices.mds.ServiceManager;
import org.agilemicroservices.mds.annotation.MessagingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;


public class ServiceBeanPostProcessor implements BeanPostProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceBeanPostProcessor.class);

    @Autowired
    private ServiceManager serviceManager;


    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean.getClass().isAnnotationPresent(MessagingService.class)) {
            LOGGER.info("Registering message driven service;beanName={},class={}.", beanName, bean.getClass().getName());
            serviceManager.register(bean);
        }
        return bean;
    }
}