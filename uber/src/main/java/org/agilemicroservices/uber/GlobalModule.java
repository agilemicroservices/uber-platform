package org.agilemicroservices.uber;

import org.jboss.modules.ModuleIdentifier;


public class GlobalModule {
    private ModuleIdentifier moduleIdentifier;
    private boolean importServices;
    private boolean export;


    public GlobalModule(ModuleIdentifier moduleIdentifier, boolean importServices, boolean export) {
        this.moduleIdentifier = moduleIdentifier;
        this.importServices = importServices;
        this.export = export;
    }


    public ModuleIdentifier getModuleIdentifier() {
        return moduleIdentifier;
    }

    public boolean isImportServices() {
        return importServices;
    }

    public boolean isExport() {
        return export;
    }
}
