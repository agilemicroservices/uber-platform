package org.agilemicroservices.uber;

import org.agilemicroservices.uber.util.UberActivator;
import org.jboss.modules.Module;

import static org.agilemicroservices.uber.UberLogger.ROOT_LOGGER;


public class Component {
    private Module module;
    private String mainClassName;
    private UberActivator activator;


    public Component(String mainClassName, Module module) {
        ROOT_LOGGER.debug("Creating service {} from {}.", mainClassName, module);
        this.module = module;
        this.mainClassName = mainClassName;
    }


    public void start() {
        ROOT_LOGGER.info("Starting service {}.", mainClassName);
        try {
            UberClassLoader cl = new UberClassLoader(module);
            Class<?> clazz = cl.loadClass(mainClassName);
            activator = (UberActivator) clazz.newInstance();
            activator.start();
        } catch (Throwable e) {
            ROOT_LOGGER.warn("Service deployment failed, name=" + mainClassName + ".", e);
        }
    }

    public void stop() {
        activator.stop();
    }
}
