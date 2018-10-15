package org.agilemicroservices.uber;

import org.jboss.as.server.deployment.Attachments;
import org.jboss.as.server.deployment.DeploymentUnit;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.Module;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;
import org.jboss.vfs.VirtualFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.agilemicroservices.uber.UberLogger.ROOT_LOGGER;


public class UberCreateService implements Service<Component> {
    private static final String SERVICE_FILE_NAME = "META-INF/services/org.agilemicroservices.uber.util.UberActivator";

    private InjectedValue<DeploymentUnit> deploymentUnit = new InjectedValue<>();
    private Component component;


    @Override
    public synchronized void start(StartContext context) throws StartException {
        ROOT_LOGGER.debug("Creating {}.", deploymentUnit.getValue().getName());

        DeploymentUnit deploymentUnit = this.deploymentUnit.getValue();
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);

        ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        VirtualFile serviceFile = root.getRoot().getChild(SERVICE_FILE_NAME);
        String mainClassName = readFile(serviceFile);

        component = new Component(mainClassName, module);
    }

    private String readFile(VirtualFile serviceFile) {
        String mainClassName;
        try {
            Reader reader = new BufferedReader(new InputStreamReader(serviceFile.openStream()));
            StringBuilder builder = new StringBuilder();
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
            reader.close();
            mainClassName = builder.toString().trim();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        return mainClassName;
    }

    @Override
    public synchronized void stop(StopContext context) {
        // TODO invoke stop
        component = null;
    }

    @Override
    public synchronized Component getValue() throws IllegalStateException, IllegalArgumentException {
        return component;
    }

    public Injector<DeploymentUnit> getDeploymentUnitInjector() {
        return deploymentUnit;
    }
}
