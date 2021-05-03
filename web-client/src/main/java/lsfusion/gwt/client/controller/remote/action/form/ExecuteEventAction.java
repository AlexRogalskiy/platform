package lsfusion.gwt.client.controller.remote.action.form;

import lsfusion.gwt.client.form.object.GGroupObjectValue;
import lsfusion.gwt.client.form.property.async.GPushAsyncResult;

public class ExecuteEventAction extends FormRequestCountingAction<ServerResponseResult> {
    public int[] propertyIds;
    public GGroupObjectValue[] fullKeys;
    public String actionSID;

    public GPushAsyncResult[] pushAsyncResults;

    public ExecuteEventAction() {
    }

    public ExecuteEventAction(int[] propertyIds, GGroupObjectValue[] fullKeys, String actionSID, GPushAsyncResult[] pushAsyncResults) {
        this.propertyIds = propertyIds;
        this.fullKeys = fullKeys;
        this.actionSID = actionSID;
        this.pushAsyncResults = pushAsyncResults;
    }
}
