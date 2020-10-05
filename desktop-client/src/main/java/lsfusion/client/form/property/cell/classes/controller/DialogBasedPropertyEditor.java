package lsfusion.client.form.property.cell.classes.controller;

import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.EventObject;

public abstract class DialogBasedPropertyEditor implements PropertyEditor {

    protected PropertyTableCellEditor tableEditor;

    protected EditorStub editorStub;

    private boolean editingStopped = false;

    public DialogBasedPropertyEditor() {
        editorStub = new EditorStub();
        editorStub.setOpaque(true);
    }

    public final Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        Point desiredLocation = null;
        if (tableLocation != null && cellRectangle != null) {
            desiredLocation = new Point((int) (tableLocation.getX() + cellRectangle.getX()), (int) (tableLocation.getY() + cellRectangle.getMaxY()));
        }
        showDialog(desiredLocation);
        return editorStub;
    }

    public void setTableEditor(PropertyTableCellEditor tableEditor) {
        this.tableEditor = tableEditor;
    }

    public abstract void showDialog(Point desiredLocation);

    public abstract boolean valueChanged();

    public abstract Object getCellEditorValue();

    @Override
    public boolean stopCellEditing() {
        editingStopped = true;
        return true;
    }

    @Override
    public void cancelCellEditing() { }

    protected class EditorStub extends JLabel {
        public EditorStub() {
            addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    stopEditing();
                }
            });
        }

        private void stopEditing() {
            if (!editingStopped) {
                editingStopped = true;
                if (valueChanged()) {
                    tableEditor.stopCellEditingLater();
                } else {
                    tableEditor.cancelCellEditing();
                }
            }
        }
    }
}
