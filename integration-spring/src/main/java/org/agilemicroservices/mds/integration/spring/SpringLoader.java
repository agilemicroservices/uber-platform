package org.agilemicroservices.mds.integration.spring;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class SpringLoader {
    private ApplicationContext context;


    /**
     * Creates a spring context from <code>META-INF/spring/context.xml</code>.
     */
    public SpringLoader() {
        this("META-INF/spring/context.xml");
    }

    public SpringLoader(String... configLocations) {
        context = new ClassPathXmlApplicationContext(configLocations);
    }

    public SpringLoader(Class<?>... annotatedClasses) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setClassLoader(SpringLoader.class.getClassLoader());
        context.register(annotatedClasses);
        context.refresh();

        this.context = context;
    }


    // TODO is there a common lifecycle interface?
    public void destroy() {
        if (context instanceof AnnotationConfigApplicationContext) {
            ((AnnotationConfigApplicationContext) context).close();
        } else if (context instanceof ClassPathXmlApplicationContext) {
            ((ClassPathXmlApplicationContext) context).close();
        }
    }


    public ApplicationContext getContext() {
        return context;
    }
}
