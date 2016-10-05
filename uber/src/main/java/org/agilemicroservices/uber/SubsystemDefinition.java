package org.agilemicroservices.uber;

import org.jboss.as.controller.SimpleResourceDefinition;
import org.jboss.as.controller.registry.ManagementResourceRegistration;


public class SubsystemDefinition extends SimpleResourceDefinition {
    static final SubsystemDefinition INSTANCE = new SubsystemDefinition();

    private SubsystemDefinition() {
        super(new Parameters(UberExtension.SUBSYSTEM_PATH,
                UberExtension.getResourceDescriptionResolver(null))
        .setAddHandler(SubsystemAdd.INSTANCE));
    }

    @Override
    public void registerAttributes(ManagementResourceRegistration resourceRegistration) {
    }

//    @Override
//    public void registerOperations(ManagementResourceRegistration resourceRegistration) {
//        super.registerOperations(resourceRegistration);
//    }
}
