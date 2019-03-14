package lsfusion.server.logics.action.flow;

import lsfusion.server.data.SQLHandledException;
import lsfusion.server.logics.property.implement.PropertyMapImplement;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.property.derived.DerivedProperty;

import java.sql.SQLException;

public class BreakActionProperty extends ChangeFlowAction {
    public BreakActionProperty() {
        super(LocalizedString.create("break"));

        finalizeInit();
    }

    public boolean hasFlow(ChangeFlowType type) {
        if(type == ChangeFlowType.BREAK)
            return true;
        return super.hasFlow(type);
    }

    public FlowResult aspectExecute(ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        return FlowResult.BREAK;
    }

    @Override
    public PropertyMapImplement<?, PropertyInterface> calcWhereProperty() {
        return DerivedProperty.createNull();
    }
}
