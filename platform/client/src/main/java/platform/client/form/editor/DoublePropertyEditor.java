package platform.client.form.editor;

import platform.interop.ComponentDesign;

import javax.swing.*;
import javax.swing.text.DefaultFormatterFactory;
import javax.swing.text.NumberFormatter;
import java.text.*;

public class DoublePropertyEditor extends TextFieldPropertyEditor {

    public DoublePropertyEditor(Object value, NumberFormat format, ComponentDesign design, Class<?> valueClass) {
        super(design);
        final DecimalFormat df = (DecimalFormat) format;
        final boolean isGroupSeparatorDot = df.getDecimalFormatSymbols().getGroupingSeparator() == '.';

        NumberFormatter formatter = new NullNumberFormatter(format, isGroupSeparatorDot ? 0 : 0.0) {
            private final char separator = df.getDecimalFormatSymbols().getDecimalSeparator();

            public boolean lastTextEndsWithSeparator;
            public int lastZero;

            @Override
            public String valueToString(Object value) throws ParseException {
                String result = super.valueToString(value);
                if (lastTextEndsWithSeparator && result != null && result.indexOf(separator) == -1) {
                    result += separator;
                    lastTextEndsWithSeparator = false;
                }
                for (int i = 0; i < lastZero; i++)
                    result += '0';
                return result;
            }

            @Override
            public Object stringToValue(String text) throws ParseException {
                lastZero = 0;
                if (text != null && text.length() > 0) {
                    if (!isGroupSeparatorDot)
                        text = text.replace(',', separator).replace('.', separator);
                    if (text.indexOf(separator) != -1) {
                        while (lastZero < text.length() - 1 && text.charAt(text.length() - 1 - lastZero) == '0') {
                            lastZero++;
                        }
                    }
                    lastTextEndsWithSeparator = text.indexOf(separator) == text.length() - 1 - lastZero;
                } else {
                    lastTextEndsWithSeparator = false;
                }
                return super.stringToValue(text);
            }
        };

        formatter.setValueClass(valueClass);
        formatter.setAllowsInvalid(false);

        this.setHorizontalAlignment(JTextField.RIGHT);
        setFormatterFactory(new DefaultFormatterFactory(formatter));

        if (value != null) {
            try {
                setText(formatter.valueToString(value));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }


    public Object getCellEditorValue() {

        try {
            commitEdit();
        } catch (ParseException e) {
            return null;
        }

        return this.getValue();
    }

    @Override
    public String checkValue(Object value) {
        return null;
    }
}
