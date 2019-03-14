package lsfusion.server.physics.dev.id.resolve;

import lsfusion.server.logics.LogicsModule;
import lsfusion.server.logics.form.struct.group.AbstractGroup;

public class ModuleGroupFinder extends ModuleSingleElementFinder<AbstractGroup, Object> {
    @Override
    protected AbstractGroup getElement(LogicsModule module, String simpleName, Object param) {
        return module.getGroup(simpleName);
    }
}
