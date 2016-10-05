package org.agilemicroservices.uber;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;


public class UberStartService implements Service<Component> {
    private InjectedValue<Component> component = new InjectedValue<>();
    private InjectedValue<ExecutorService> executor = new InjectedValue<>();

    @Override
    public void start(StartContext context) throws StartException {
        Runnable runnable = () -> {
            try {
                getValue().start();
                context.complete();
            } catch (Throwable t) {
                context.failed(new StartException(t));
            }
        };

        try {
            executor.getValue().submit(runnable);
        } catch (RejectedExecutionException e) {
            runnable.run();
        } finally {
            context.asynchronous();
        }
    }

    @Override
    public void stop(StopContext context) {
        // TODO should failed be set on exception?
        Runnable runnable = () -> {
            try {
                getValue().stop();
            } finally {
                context.complete();
            }
        };

        try {
            executor.getValue().submit(runnable);
        } catch (RejectedExecutionException e) {
            runnable.run();
        } finally {
            context.asynchronous();
        }
    }

    @Override
    public Component getValue() throws IllegalStateException, IllegalArgumentException {
        return component.getValue();
    }

    public Injector<Component> getComponentInjector() {
        return component;
    }

    public Injector<ExecutorService> getExecutorInjector() {
        return executor;
    }
}
