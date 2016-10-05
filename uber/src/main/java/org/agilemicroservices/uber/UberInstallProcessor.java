package org.agilemicroservices.uber;

import org.agilemicroservices.uber.util.UberActivator;
import org.jboss.as.server.Services;
import org.jboss.as.server.deployment.*;
import org.jboss.as.server.deployment.module.ResourceRoot;
import org.jboss.modules.Module;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.vfs.VirtualFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import static org.agilemicroservices.uber.UberLogger.ROOT_LOGGER;


public class UberInstallProcessor implements DeploymentUnitProcessor {
    private static final String SERVICE_FILE_NAME = "META-INF/services/org.agilemicroservices.uber.util.UberActivator";
    static final Phase PHASE = Phase.INSTALL;
    static final int PRIORITY = Phase.INSTALL_EE_COMPONENT;

    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        Module module = deploymentUnit.getAttachment(Attachments.MODULE);
        ROOT_LOGGER.debug("Evaluating {}.", module);

        ResourceRoot root = deploymentUnit.getAttachment(Attachments.DEPLOYMENT_ROOT);
        VirtualFile serviceFile = root.getRoot().getChild(SERVICE_FILE_NAME);
        if (serviceFile.exists()) {
            deployService(phaseContext);
//            ROOT_LOGGER.info("Deploying module {}.", phaseContext.getDeploymentUnit().getName());
//
//            String mainClassName = readFile(serviceFile);
//            try {
//                UberClassLoader cl = new UberClassLoader(module);
//                Class<?> clazz = cl.loadClass(mainClassName);
//                UberActivator activator = (UberActivator) clazz.newInstance();
//                activator.start();
//            } catch (Throwable e) {
//                ROOT_LOGGER.info("Module deployment failed.", e);
//            }
//
//            ServiceTarget serviceTarget = phaseContext.getServiceTarget();
//
//            UberCreateService createService = new UberCreateService();
//            ServiceName createServiceName = ServiceName.of("testcreate", mainClassName);
//            ServiceBuilder<Component> createBuilder = serviceTarget.addService(createServiceName, createService);
//            createBuilder.addDependency(deploymentUnit.getServiceName(), DeploymentUnit.class, createService.getDeploymentUnitInjector());
//
//            UberStartService startService = new UberStartService();
//            ServiceName startServiceName = ServiceName.of("teststart", mainClassName);
//            ServiceBuilder<Component> startBuilder = serviceTarget.addService(startServiceName, startService);
//            deploymentUnit.addToAttachmentList(Attachments.DEPLOYMENT_COMPLETE_SERVICES, startServiceName);
//
//            startBuilder.addDependency(createServiceName, Component.class, startService.getComponentInjector());
//            Services.addServerExecutorDependency(startBuilder, startService.getExecutorInjector(), false);
//
//            createBuilder.install();
//            startBuilder.install();
        } else {
            ROOT_LOGGER.info("No Uber deployment descriptor for for {}.", phaseContext.getDeploymentUnit().getName());
        }
    }

    private void deployService(DeploymentPhaseContext phaseContext) {
        DeploymentUnit deploymentUnit = phaseContext.getDeploymentUnit();
        ROOT_LOGGER.info("Deploying module {}.", deploymentUnit.getName());

        ServiceTarget serviceTarget = phaseContext.getServiceTarget();

        UberCreateService createService = new UberCreateService();
        ServiceName createServiceName = ServiceName.of("testcreate", deploymentUnit.getName());
        ServiceBuilder<Component> createBuilder = serviceTarget.addService(createServiceName, createService);
        createBuilder.addDependency(deploymentUnit.getServiceName(), DeploymentUnit.class, createService.getDeploymentUnitInjector());

        UberStartService startService = new UberStartService();
        ServiceName startServiceName = ServiceName.of("teststart", deploymentUnit.getName());
        ServiceBuilder<Component> startBuilder = serviceTarget.addService(startServiceName, startService);
        deploymentUnit.addToAttachmentList(Attachments.DEPLOYMENT_COMPLETE_SERVICES, startServiceName);

        startBuilder.addDependency(createServiceName, Component.class, startService.getComponentInjector());
        Services.addServerExecutorDependency(startBuilder, startService.getExecutorInjector(), false);

        createBuilder.install();
        startBuilder.install();
    }


    @Override
    public void undeploy(DeploymentUnit context) {
    }
}