package org.agilemicroservices.uber;

import org.jboss.as.controller.AbstractBoottimeAddStepHandler;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.server.AbstractDeploymentChainStep;
import org.jboss.as.server.DeploymentProcessorTarget;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.ModuleIdentifier;

import java.util.Arrays;

import static org.agilemicroservices.uber.UberLogger.ROOT_LOGGER;


class SubsystemAdd extends AbstractBoottimeAddStepHandler {
    static final SubsystemAdd INSTANCE = new SubsystemAdd();

    @Override
    protected void performBoottime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        ROOT_LOGGER.info("Activating Uber Subsystem.");

        context.addStep(new AbstractDeploymentChainStep() {

            @Override
            protected void execute(DeploymentProcessorTarget processorTarget) {
                GlobalModuleDependencyProcessor globalModuleDependencyProcessor = new GlobalModuleDependencyProcessor();
                globalModuleDependencyProcessor.setGlobalModules(Arrays.asList(
                        new GlobalModule(ModuleIdentifier.create("org.agilemicroservices.uber"), false, false),
                        new GlobalModule(ModuleIdentifier.create("org.jboss.logging"), false, false),
                        new GlobalModule(ModuleIdentifier.create("javax.api"), true, true)));

                processorTarget.addDeploymentProcessor(
                        UberExtension.SUBSYSTEM_NAME,
                        GlobalModuleDependencyProcessor.PHASE,
                        GlobalModuleDependencyProcessor.PRIORITY,
                        globalModuleDependencyProcessor);

                processorTarget.addDeploymentProcessor(
                        UberExtension.SUBSYSTEM_NAME,
                        UberInstallProcessor.PHASE,
                        UberInstallProcessor.PRIORITY,
                        new UberInstallProcessor());
            }
        }, OperationContext.Stage.RUNTIME);
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
    }
}
