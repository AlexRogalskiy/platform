package lsfusion.server.logics.form.interactive.action.input;

import lsfusion.base.col.interfaces.immutable.ImOrderSet;
import lsfusion.base.col.interfaces.immutable.ImRevMap;
import lsfusion.server.data.SQLCallable;
import lsfusion.server.data.SQLHandledException;
import lsfusion.server.physics.dev.i18n.LocalizedString;
import lsfusion.server.logics.action.implement.ActionPropertyMapImplement;
import lsfusion.server.logics.property.implement.PropertyInterfaceImplement;
import lsfusion.server.logics.action.ExecutionContext;
import lsfusion.server.logics.property.oraction.PropertyInterface;
import lsfusion.server.logics.action.flow.AroundAspectAction;
import lsfusion.server.logics.action.flow.FlowResult;

import java.sql.SQLException;

public class PushUserInputActionProperty extends AroundAspectAction {

    private final PropertyInterfaceImplement<PropertyInterface> push;

    // по аналогии с If
    public <I extends PropertyInterface> PushUserInputActionProperty(LocalizedString caption, ImOrderSet<I> innerInterfaces, PropertyInterfaceImplement<I> push, ActionPropertyMapImplement<?, I> action) {
        super(caption, innerInterfaces, action);

        ImRevMap<I, PropertyInterface> mapInterfaces = getMapInterfaces(innerInterfaces).reverse();
        this.push = push.map(mapInterfaces);
        
        finalizeInit();
    }

    @Override
    protected FlowResult aroundAspect(final ExecutionContext<PropertyInterface> context) throws SQLException, SQLHandledException {
        return context.pushRequestedValue(push.readClasses(context, context.getKeys()), null, new SQLCallable<FlowResult>() {
            public FlowResult call() throws SQLException, SQLHandledException {
                return proceed(context);
            }
        });
    }
}
