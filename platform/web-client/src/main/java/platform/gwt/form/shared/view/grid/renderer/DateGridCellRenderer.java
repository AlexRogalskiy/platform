package platform.gwt.form.shared.view.grid.renderer;

import com.google.gwt.dom.client.Style;
import com.google.gwt.i18n.client.DateTimeFormat;
import platform.gwt.base.shared.GwtSharedUtils;
import platform.gwt.form.shared.view.GPropertyDraw;

import java.util.Date;

public class DateGridCellRenderer extends TextBasedGridCellRenderer<Date> {
    private final DateTimeFormat format;

    public DateGridCellRenderer(GPropertyDraw property) {
        this(property, GwtSharedUtils.getDefaultDateFormat());
    }

    public DateGridCellRenderer(GPropertyDraw property, DateTimeFormat format) {
        super(property, Style.TextAlign.RIGHT);
        this.format = format;
    }

    @Override
    protected String renderToString(Date value) {
        return format.format(value, null);
    }
}
