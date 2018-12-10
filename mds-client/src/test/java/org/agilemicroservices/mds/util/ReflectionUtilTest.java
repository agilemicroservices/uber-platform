package org.agilemicroservices.mds.util;

import org.agilemicroservices.mds.annotation.JmsInbound;
import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;


public class ReflectionUtilTest
{

    @Test
    public void test()
    {
        Map<MethodSignature, ServiceMethod> signatureToServiceMethodMap = new HashMap<>();
        ReflectionUtil.forEachServiceMethod(DerivedClass.class, signatureToServiceMethodMap);

        // defined by class
        MethodSignature signature = new MethodSignature("derivedMethod", new Class<?>[0]);
        ServiceMethod serviceMethod = signatureToServiceMethodMap.get(signature);
        assertEquals(DerivedClass.class, serviceMethod.getMethod().getDeclaringClass());

        // defined by interface, not redefined
        signature = new MethodSignature("derivedIfaceMethod", new Class<?>[0]);
        serviceMethod = signatureToServiceMethodMap.get(signature);
        assertEquals(DerivedInterface.class, serviceMethod.getMethod().getDeclaringClass());

        // defined by transitive interface, redefined
        signature = new MethodSignature("baseIfaceMethod", new Class<?>[0]);
        serviceMethod = signatureToServiceMethodMap.get(signature);
        assertEquals(DerivedClass.class, serviceMethod.getMethod().getDeclaringClass());

        // inherited method, not overridden
        signature = new MethodSignature("baseMethod", new Class<?>[0]);
        serviceMethod = signatureToServiceMethodMap.get(signature);
        assertEquals(BaseClass.class, serviceMethod.getMethod().getDeclaringClass());

        // overridden method, not redefined
        signature = new MethodSignature("overriddenBaseMethod", new Class<?>[0]);
        serviceMethod = signatureToServiceMethodMap.get(signature);
        assertEquals(BaseClass.class, serviceMethod.getMethod().getDeclaringClass());

        // inherited method transitively inheriting annotation
        signature = new MethodSignature("baseOnlyIfaceMethod", new Class<?>[0]);
        serviceMethod = signatureToServiceMethodMap.get(signature);
        assertEquals(BaseOnly.class, serviceMethod.getMethod().getDeclaringClass());
    }


    private static class BaseClass implements BaseOnly
    {

        @JmsInbound("queue:baseMethod")
        public void baseMethod()
        {
        }

        @JmsInbound("queue:overriddenBaseMethod")
        public void overriddenBaseMethod()
        {
        }

        @Override
        public void baseOnlyIfaceMethod()
        {
        }
    }

    private static class DerivedClass extends BaseClass implements DerivedInterface
    {

        @JmsInbound("queue:derivedMethod")
        public void derivedMethod()
        {
        }

        @Override
        public void overriddenBaseMethod()
        {
        }

        @Override
        public void derivedIfaceMethod()
        {
        }

        @Override
        @JmsInbound("queue:baseIfaceMethod")
        public void baseIfaceMethod()
        {
        }
    }

    private interface BaseOnly
    {
        @JmsInbound("queue:baseOnlyIfaceMethod")
        void baseOnlyIfaceMethod();
    }

    private interface BaseInterface
    {
        @JmsInbound("queue:baseIfaceMethod")
        void baseIfaceMethod();
    }

    private interface DerivedInterface extends BaseInterface
    {
        @JmsInbound("queue:derivedIfaceMethod")
        void derivedIfaceMethod();
    }
}
