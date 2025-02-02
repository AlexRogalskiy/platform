package lsfusion.gwt.client.form.property.cell.controller;

import com.google.gwt.user.client.rpc.AsyncCallback;
import lsfusion.gwt.client.base.GAsync;
import lsfusion.gwt.client.base.Pair;
import lsfusion.gwt.client.form.property.cell.view.GUserInputResult;

import java.util.ArrayList;

public interface EditManager {
    void getAsyncValues(String value, AsyncCallback<Pair<ArrayList<GAsync>, Boolean>> callback);

    void commitEditing(GUserInputResult result, CommitReason commitReason);  // assert if blurred then editor rerender dom

    void cancelEditing(CancelReason cancelReason);

    boolean isThisCellEditing(CellEditor cellEditor);
}
