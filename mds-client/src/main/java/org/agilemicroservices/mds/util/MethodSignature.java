package org.agilemicroservices.mds.util;

import java.lang.reflect.Method;
import java.util.Arrays;


public class MethodSignature
{
    private String methodName;
    private Class<?> parameterTypes[];


    public MethodSignature(Method method)
    {
        this(method.getName(), method.getParameterTypes());
    }

    public MethodSignature(String methodName, Class<?>[] parameterTypes)
    {
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MethodSignature signature = (MethodSignature) o;

        if (!methodName.equals(signature.methodName)) return false;
        return Arrays.equals(parameterTypes, signature.parameterTypes);
    }

    @Override
    public int hashCode()
    {
        int result = methodName.hashCode();
        result = 31 * result + Arrays.hashCode(parameterTypes);
        return result;
    }

    public String getMethodName()
    {
        return methodName;
    }

    public Class<?>[] getParameterTypes()
    {
        return parameterTypes;
    }

    @Override
    public String toString()
    {
        return "MethodSignature{" +
                "methodName='" + methodName + '\'' +
                ", parameterTypes=" + Arrays.toString(parameterTypes) +
                '}';
    }
}
