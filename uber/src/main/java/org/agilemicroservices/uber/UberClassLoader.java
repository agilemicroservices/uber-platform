package org.agilemicroservices.uber;

import org.jboss.modules.ConcurrentClassLoader;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;


public class UberClassLoader extends ConcurrentClassLoader {
    // TODO load from wildfly properties file used by Module.systemPackages
    private static final String[] systemPackages = new String[]{
            "java.",
            "__redirected.",
            "sun.reflect.",
            "javax.xml.",
            "org.xml.",
            "org.w3c.",
            "org.slf4j."
    };

    private ModuleClassLoader moduleClassLoader;


    static {
        boolean parallelOk = true;
        try {
            parallelOk = ClassLoader.registerAsParallelCapable();
        } catch (Throwable ignored) {
        }
        if (!parallelOk) {
            throw new Error("Failed to register " + ModuleClassLoader.class.getName() + " as parallel-capable");
        }
    }


    public UberClassLoader(Module module) {
        moduleClassLoader = module.getClassLoader();
    }


    @Override
    protected Class<?> findClass(String className, boolean exportsOnly, boolean resolve) throws ClassNotFoundException {
        // fall through on system classes
        for (String o : systemPackages) {
            if (className.startsWith(o)) {
                //System.out.println("FINDING SYSTEM CLASS " + className);
                return moduleClassLoader.loadClass(className, resolve);
            }
        }

        // find if already loaded
        Class<?> clazz = findLoadedClass(className);
        if (null != clazz) {
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }

        // load class if contained in module
        String binaryName = className.replace(".", "/") + ".class";
        List<Resource> resources = moduleClassLoader.loadResourceLocal(binaryName);
        if (null == resources || resources.isEmpty()) {
            //System.out.println("FINDING MODULE CLASS " + className);
            clazz = moduleClassLoader.loadClass(className, resolve);
        } else {
            //System.out.println("FINDING GHOST CLASS " + className);
            try {
                InputStream inputStream = resources.get(0).openStream();
                byte[] bytes = toByteArray(inputStream);
                clazz = defineOrLoadClass(className, bytes, 0, bytes.length);

                if (clazz.getPackage() == null) {
                    int packageSeparator = className.lastIndexOf('.');
                    if (packageSeparator != -1) {
                        String packageName = className.substring(0, packageSeparator);
                        definePackage(packageName, null, null, null, null, null, null, null);
                    }
                }

//                if (resolve) {
//                    resolveClass(clazz);
//                }
            } catch (IOException e) {
                throw new ClassNotFoundException("Unable to load resource for " + className, e);
            }
        }

        return clazz;
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
    public final URL findResource(final String name, final boolean exportsOnly) {
        List<Resource> resources = moduleClassLoader.loadResourceLocal(name);
        if (null != resources && !resources.isEmpty()) {
            return resources.get(0).getURL();
        }
        return null;
    }

    // TODO optimize
    @Override
    public final Enumeration<URL> findResources(final String name, final boolean exportsOnly) throws IOException {
        List<Resource> resources = moduleClassLoader.loadResourceLocal(name);
        Vector<URL> urls = new Vector<>();
        urls.addAll(resources.stream().map(Resource::getURL).collect(Collectors.toList()));
        return urls.elements();
    }

    @Override
    public final InputStream findResourceAsStream(final String name, boolean exportsOnly) {
        List<Resource> resources = moduleClassLoader.loadResourceLocal(name);
        if (null != resources && !resources.isEmpty()) {
            try {
                return resources.get(0).openStream();
            } catch (IOException e) {
                return null;
            }
        }
        return null;
    }
}
