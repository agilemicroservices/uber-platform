package org.agilemicroservices.mds.util;

import org.agilemicroservices.mds.annotation.JmsInbound;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.function.Consumer;


public final class ReflectionUtil {

    private ReflectionUtil() {
        // static class
    }


    public static void forEachServiceMethod(final Class<?> clazz,
                                            final Map<MethodSignature, ServiceMethod> signatureToMethodMap) {
        for (Method o : clazz.getDeclaredMethods())
        {
            if (isCandidateMethod(o))
            {
                MethodSignature signature = new MethodSignature(o);
                ServiceMethod serviceMethod = new ServiceMethod(o);
                signatureToMethodMap.putIfAbsent(signature, serviceMethod);
            }
        }

        Class<?> superclass = clazz.getSuperclass();
        if (null != superclass && Object.class != superclass)
        {
            forEachServiceMethod(clazz.getSuperclass(), signatureToMethodMap);
        }

        for (Class<?> o : clazz.getInterfaces())
        {
            forEachServiceMethod(o, signatureToMethodMap);
        }
    }

    public static void forEachServiceMethod(final Class<?> serviceInterface, final Consumer<ServiceMethod> consumer) {
        for (Method o : serviceInterface.getMethods()) {
            if (isCandidateMethod(o)) {
                ServiceMethod serviceMethod = new ServiceMethod(o);
                consumer.accept(serviceMethod);
            }
        }
    }

    public static boolean isCandidateMethod(Method method) {
        // TODO handle all cases
        return !method.isSynthetic() &&
                method.getParameterCount() <= 1 &&
                method.isAnnotationPresent(JmsInbound.class) &&
                (method.getModifiers() & Modifier.STATIC) == 0;
    }
}
