package org.agilemicroservices.uber;

import org.agilemicroservices.uber.util.UberActivator;
import org.agilemicroservices.uber.util.annotation.ActivatorStart;
import org.agilemicroservices.uber.util.annotation.ActivatorStop;
import org.jboss.modules.Module;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.agilemicroservices.uber.UberLogger.ROOT_LOGGER;


public class Component {
    private Module module;
    private String mainClassName;
    private Class<?> activatorClass;
    private Object activatorObj;


    public Component(String mainClassName, Module module) {
        ROOT_LOGGER.debug("Creating service {} from {}.", mainClassName, module);
        this.module = module;
        this.mainClassName = mainClassName;

        UberClassLoader cl = new UberClassLoader(module);
        try {
            activatorClass = cl.loadClass(mainClassName);
            activatorObj = activatorClass.newInstance();
        } catch (Throwable e) {
            ROOT_LOGGER.warn("Service initialization failed, class=" + mainClassName + ", module=" + module.getName() + ".", e);
        }
    }


    public void start() {
        ROOT_LOGGER.info("Starting service {}.", mainClassName);
        try {
            if (UberActivator.class.isAssignableFrom(activatorClass)) {
                ((UberActivator) activatorObj).start();
            } else {
                invokeAnnotatedStartMethod();
            }
        } catch (Throwable e) {
            ROOT_LOGGER.warn("Service start failed, class=" + mainClassName + ", module=" + module.getName() + ".", e);
        }
    }

    public void stop() {
        ROOT_LOGGER.info("Stopping service {}.", mainClassName);
        try {
            if (UberActivator.class.isAssignableFrom(activatorClass)) {
                ((UberActivator) activatorObj).stop();
            } else {
                invokeAnnotatedStopMethod();
            }
        } catch (Throwable e) {
            ROOT_LOGGER.warn("Service stop failed, class=" + mainClassName + ", module=" + module.getName() + ".", e);
        }
    }

    private void invokeAnnotatedStartMethod() {
        for (Method o : activatorClass.getDeclaredMethods()) {
            if (o.isAnnotationPresent(ActivatorStart.class)) {
                o.setAccessible(true);
                try {
                    o.invoke(activatorObj);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(
                            "Unable to invoke activator start method, class=" + mainClassName + ".", e);
                }
                break;
            }
        }
    }

    private void invokeAnnotatedStopMethod() {
        for (Method o : activatorClass.getDeclaredMethods()) {
            if (o.isAnnotationPresent(ActivatorStop.class)) {
                o.setAccessible(true);
                try {
                    o.invoke(activatorObj);
                } catch (IllegalAccessException | InvocationTargetException e) {
                    throw new IllegalStateException(
                            "Unable to invoke activator stop method, class=" + mainClassName + ".", e);
                }
                break;
            }
        }
    }
}
