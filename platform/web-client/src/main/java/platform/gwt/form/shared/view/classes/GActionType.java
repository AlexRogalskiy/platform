package platform.gwt.form.shared.view.classes;

import platform.gwt.form.client.form.ui.GFormController;
import platform.gwt.form.shared.view.GFont;
import platform.gwt.form.shared.view.GPropertyDraw;
import platform.gwt.form.shared.view.changes.GGroupObjectValue;
import platform.gwt.form.shared.view.grid.renderer.ActionGridCellRenderer;
import platform.gwt.form.shared.view.grid.renderer.GridCellRenderer;
import platform.gwt.form.shared.view.panel.ActionPanelRenderer;
import platform.gwt.form.shared.view.panel.PanelRenderer;

public class GActionType extends GDataType {
    public static GActionType instance = new GActionType();

    @Override
    public PanelRenderer createPanelRenderer(GFormController form, GPropertyDraw property, GGroupObjectValue columnKey) {
        return new ActionPanelRenderer(form, property, columnKey);
    }

    @Override
    public GridCellRenderer createGridCellRenderer(GPropertyDraw property) {
        return new ActionGridCellRenderer(property);
    }

    @Override
    public int getMaximumPixelWidth(int maximumCharWidth, GFont font) {
        return getPreferredPixelWidth(maximumCharWidth, font);
    }

    @Override
    public int getPreferredPixelHeight(GFont font) {
        return font == null || font.size == null ? 18 : (int) (font.size * 1.4);
    }

    @Override
    public String getPreferredMask() {
        return "1234567";
    }
}
