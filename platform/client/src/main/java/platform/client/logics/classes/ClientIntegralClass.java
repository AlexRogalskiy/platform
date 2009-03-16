package platform.client.logics.classes;

import platform.client.form.*;
import platform.client.form.renderer.IntegerPropertyRenderer;
import platform.client.form.editor.IntegerPropertyEditor;
import platform.client.logics.ClientCellView;

import java.io.DataInputStream;
import java.io.IOException;
import java.text.Format;
import java.text.NumberFormat;

abstract public class ClientIntegralClass extends ClientClass {

    ClientIntegralClass(DataInputStream inStream) throws IOException {
        super(inStream);
    }

    public int getMinimumWidth() { return 45; }
    public int getPreferredWidth() { return 80; }

    public Format getDefaultFormat() {
        return NumberFormat.getInstance();
    }

    public PropertyRendererComponent getRendererComponent(Format format) { return new IntegerPropertyRenderer(format); }
    public PropertyEditorComponent getEditorComponent(ClientForm form, ClientCellView property, Object value, Format format) { return new IntegerPropertyEditor(value, (NumberFormat)format, getJavaClass()); }

}
