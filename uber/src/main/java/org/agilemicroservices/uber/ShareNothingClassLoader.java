package org.agilemicroservices.uber;

import org.jboss.modules.ConcurrentClassLoader;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;


/**
 * <code>ShareNothingClassLoader</code> reloads all classes with the exception of specific system packages, separating
 * multiple instances of a deployment.
 */
public class ShareNothingClassLoader extends ClassLoader {
    private static final String[] SYSTEM_PACKAGES = new String[]{
            "java.",
            "javax.xml.",
            "javax.naming.",
            "sun.reflect.",
            "sun.security.",
            "__redirected.",
            "org.agilemicroservices.uber.",
            "org.xml.",
            "org.w3c.",
            "com.sun."
    };

    private ModuleClassLoader moduleClassLoader;


    private static boolean isSystemPackage(String packageName) {
        for (int i = 0; i < SYSTEM_PACKAGES.length; i++) {
            if (packageName.startsWith(SYSTEM_PACKAGES[i])) {
                return true;
            }
        }
        return false;
    }


    public ShareNothingClassLoader(ModuleClassLoader moduleClassLoader) {
        this.moduleClassLoader = moduleClassLoader;
    }


    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        Class<?> clazz = null;


        if (isSystemPackage(name)) {
            try {
                clazz = moduleClassLoader.loadClass(name, resolve);
            } catch (ClassNotFoundException e) {
                // do nothing
            }
            if (null == clazz) {
                clazz = ConcurrentClassLoader.class.getClassLoader().loadClass(name);
            }
            return clazz;
        }

        clazz = findClass(name);
        if (resolve) {
            resolveClass(clazz);
        }

        return clazz;
    }

    @Override
    protected synchronized Class<?> findClass(String name) throws ClassNotFoundException {
        Class<?> clazz = null;

        if (isSystemPackage(name)) {
            try {
                clazz = moduleClassLoader.loadClass(name, false);
            } catch (ClassNotFoundException e) {
                // do nothing
            }
            if (null == clazz) {
                clazz = ConcurrentClassLoader.class.getClassLoader().loadClass(name);
            }
            return clazz;
        }

        clazz = findLoadedClass(name);
        if (null == clazz) {
            clazz = doLoadClass(name);
            if (null == clazz) {
                clazz = moduleClassLoader.loadClass(name);
            }
        }

        return clazz;
    }

    private Class<?> doLoadClass(String name) throws ClassNotFoundException {
        String binaryName = name.replace(".", "/") + ".class";
        List<Resource> resources = moduleClassLoader.loadResourceLocal(binaryName);
        if (null == resources || resources.isEmpty()) {
            return null;
        }

        try {
            InputStream inputStream = resources.get(0).openStream();
            byte[] bytes = toByteArray(inputStream);
            Class<?> cls = defineClass(name, bytes, 0, bytes.length);

            if (cls.getPackage() == null) {
                int packageSeparator = name.lastIndexOf('.');
                if (packageSeparator != -1) {
                    String packageName = name.substring(0, packageSeparator);
                    definePackage(packageName, null, null, null, null, null, null, null);
                }
            }

            return cls;
        } catch (IOException ex) {
            throw new ClassNotFoundException("Cannot load resource for class [" + name + "]", ex);
        }
    }

    private byte[] toByteArray(InputStream inputStream) throws IOException {
        byte[] bytes = new byte[256];
        int total = 0;
        int last;
        while ((last = inputStream.read(bytes, total, bytes.length - total)) != -1) {
            total += last;
            if (total == bytes.length) {
                bytes = Arrays.copyOf(bytes, bytes.length * 2);
            }
        }

        if (total != bytes.length) {
            bytes = Arrays.copyOf(bytes, total);
        }

        return bytes;
    }


    @Override
    public URL getResource(String name) {
        return moduleClassLoader.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return moduleClassLoader.getResources(name);
    }

    @Override
    protected URL findResource(String name) {
        return moduleClassLoader.findResource(name, false);
    }

    @Override
    protected Enumeration<URL> findResources(String name) throws IOException {
        return moduleClassLoader.findResources(name, false);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return moduleClassLoader.getResourceAsStream(name);
    }
}
