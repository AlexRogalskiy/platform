package lsfusion.client.form.renderer;

import lsfusion.base.FileData;
import lsfusion.client.SwingUtils;
import lsfusion.client.logics.ClientPropertyDraw;

public class DynamicFormatFileRenderer extends FilePropertyRenderer {

    public DynamicFormatFileRenderer(ClientPropertyDraw property) {
        super(property);
    }

    public void setValue(Object value) {
        super.setValue(value);
        
        if (value != null) {
            FileData fileData = (FileData) value;
            getComponent().setIcon(SwingUtils.getSystemIcon(fileData.getExtension()));
        }
    }
}