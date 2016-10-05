package org.agilemicroservices.uber;

import org.jboss.as.server.deployment.*;
import org.jboss.as.server.deployment.module.ModuleDependency;
import org.jboss.as.server.deployment.module.ModuleSpecification;
import org.jboss.modules.Module;

import java.util.List;


public class GlobalModuleDependencyProcessor implements DeploymentUnitProcessor {
    public static final Phase PHASE = Phase.STRUCTURE;
    public static final int PRIORITY = Phase.STRUCTURE_GLOBAL_MODULES;
    private List<GlobalModule> globalModules;


    @Override
    public void deploy(DeploymentPhaseContext phaseContext) throws DeploymentUnitProcessingException {
        DeploymentUnit unit = phaseContext.getDeploymentUnit();
        ModuleSpecification specification = unit.getAttachment(Attachments.MODULE_SPECIFICATION);

        for (GlobalModule o : globalModules) {
            ModuleDependency dependency = new ModuleDependency(Module.getBootModuleLoader(), o.getModuleIdentifier(),
                    false, o.isExport(), o.isImportServices(), true);
            specification.addSystemDependency(dependency);
        }
    }

    @Override
    public void undeploy(DeploymentUnit context) {
    }


    public void setGlobalModules(List<GlobalModule> globalModules) {
        this.globalModules = globalModules;
    }
}
