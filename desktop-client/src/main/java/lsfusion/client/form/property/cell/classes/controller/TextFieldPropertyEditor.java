package lsfusion.client.form.property.cell.classes.controller;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.DefaultEventComboBoxModel;
import lsfusion.base.Pair;
import lsfusion.base.lambda.AsyncCallback;
import lsfusion.client.base.view.ClientColorUtils;
import lsfusion.client.base.view.ClientImages;
import lsfusion.client.base.view.SwingDefaults;
import lsfusion.client.form.property.ClientPropertyDraw;
import lsfusion.client.form.property.async.ClientInputList;
import lsfusion.client.form.property.cell.classes.controller.suggest.BasicComboBoxUI;
import lsfusion.client.form.property.cell.controller.PropertyTableCellEditor;
import lsfusion.client.form.property.panel.view.CaptureKeyEventsDispatcher;
import lsfusion.client.form.property.table.view.AsyncChangeInterface;
import lsfusion.client.form.property.table.view.AsyncInputComponent;
import lsfusion.interop.form.event.KeyStrokes;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import javax.swing.plaf.basic.BasicComboPopup;
import javax.swing.plaf.basic.ComboPopup;
import java.awt.*;
import java.awt.event.*;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EventObject;
import java.util.List;

import static lsfusion.client.base.view.SwingDefaults.getTableCellMargins;
import static org.apache.commons.lang3.StringUtils.trimToEmpty;

public abstract class TextFieldPropertyEditor extends JFormattedTextField implements PropertyEditor {
    private static final String CANCEL_EDIT_ACTION = "reset-field-edit";

    private PropertyTableCellEditor tableEditor;
    private AsyncChangeInterface asyncChange;
    private ClientPropertyDraw property;

    private String actionSID;
    private boolean hasList;
    private boolean strict;
    private String[] actions;

    private EventObject editEvent;

    private SuggestBox suggestBox;

    private SuggestPopupButton refreshButton;

    TextFieldPropertyEditor(ClientPropertyDraw property) {
        this(property, null, null);
    }

    TextFieldPropertyEditor(ClientPropertyDraw property, AsyncChangeInterface asyncChange, Object value) {
        super();
        this.asyncChange = asyncChange;
        this.property = property;

        editEvent = asyncChange != null ? asyncChange.getCurrentEditEvent() : null;

        this.actionSID = asyncChange != null ? asyncChange.getCurrentActionSID() : null;

        ClientInputList inputList = asyncChange != null ? asyncChange.getCurrentInputList() : null;
        this.hasList = inputList != null && !disableSuggest();
        this.strict = inputList != null && inputList.strict;
        this.actions = inputList != null ? inputList.actions : null;

        if (hasList) {
            suggestBox = new SuggestBox((String) value);
        } else {
            setBorder(this);
            setDesign(this);
            setOpaque(true);

            addActionListener(e -> {
                tableEditor.preCommit(true);
                tableEditor.stopCellEditing();
                tableEditor.postCommit();
            });

            getActionMap().put(CANCEL_EDIT_ACTION, new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    tableEditor.cancelCellEditing();
                }
            });
        }
    }

    protected boolean disableSuggest() {
        return true;
    }

    private Timer delayTimer;
    private String currentRequest; // current pending request

    private String prevSucceededEmptyQuery;

    private void requestSuggestions() {
        if(!suggestBox.disableUpdate) {
            currentRequest = suggestBox.getComboBoxEditorText();

            if(delayTimer == null)
                updateAsyncValues();
        }
    }

    private void updateAsyncValues() {
        final String query = currentRequest;
        currentRequest = null;

        if(prevSucceededEmptyQuery != null && query.startsWith(prevSucceededEmptyQuery))
            return;

        suggestBox.updateLoading(true);

        assert delayTimer == null;
        // we're sending a request, so we want to delay all others for at least 100ms
        // also we're using timer to identify the call in cancelAndFlushDelayed
        Timer execTimer = new Timer(100, e -> flushDelayed());
        execTimer.setRepeats(false);
        execTimer.start();
        delayTimer = execTimer;

        asyncChange.getForm().getAsyncValues(property, asyncChange.getColumnKey(0, 0), query, actionSID,
                new AsyncCallback<Pair<List<String>, Boolean>>() {
                    @Override
                    public void done(Pair<List<String>, Boolean> result) {
                        suggestBox.updateItems(result.first, strict && !query.isEmpty());

                        suggestBox.updateLoading(result.second);

                        if(!result.second) {
                            if (result.first.isEmpty())
                                prevSucceededEmptyQuery = query;
                            else
                                prevSucceededEmptyQuery = null;
                        }

                        cancelAndFlushDelayed(execTimer);
                    }

                    @Override
                    public void failure(Throwable t) {
                        cancelAndFlushDelayed(execTimer);
                    }
                });
    }

    private void cancelAndFlushDelayed(Timer execTimer) {
        if(delayTimer == execTimer) { // we're canceling only if the current timer has not changed
            delayTimer.stop();

            flushDelayed();
        }
    }

    private void flushDelayed() {
        // assert that delaytimer is equal to execTimer
        delayTimer = null;

        if(currentRequest != null) // there was pending request
            updateAsyncValues();
    }

    private void cancelAsyncValues() {
        if(suggestBox.isLoading)
            asyncChange.getForm().getAsyncValues(property, asyncChange.getColumnKey(0, 0), null, actionSID, new AsyncCallback<Pair<List<String>, Boolean>>() {
                @Override
                public void done(Pair<List<String>, Boolean> result) {
                    // assert that CANCELED
                }
                @Override
                public void failure(Throwable t) {
                }
            });
    }

    public void setTableEditor(PropertyTableCellEditor tableEditor) {
        this.tableEditor = tableEditor;
    }

    public Component getComponent(Point tableLocation, Rectangle cellRectangle, EventObject editEvent) {
        return hasList ? suggestBox.comboBox : this;
    }

    public boolean stopCellEditing() {
        try {
            if(hasList)
                cancelAsyncValues();
            commitEdit();
        } catch (ParseException e) {
            return false;
        }

        return true;
    }

    public void cancelCellEditing() {
        if(hasList)
            cancelAsyncValues();
    }

    @Override
    public String getText() {
        return hasList ? trimToEmpty(suggestBox.getComboBoxEditorText()) : super.getText();
    }

    public Object getCellEditorValue() {
        return this.getValue();
    }

    @Override
    public String toString() {
        if (tableEditor != null) {
            return "TextFieldEditor[" + tableEditor.getTable().getName() + "]: " + super.toString();
        } else {
            return super.toString();
        }
    }

    @Override
    public void replaceSelection(String content){
        if(content.endsWith("\n"))
            content = content.substring(0, content.length()-1);
        try {
            super.replaceSelection(content);
        } catch (IllegalArgumentException e) { // strange java bug, so just suppress that exception
        }
    }

    private void setBorder(JComponent component) {
        Insets insets = getTableCellMargins();
        component.setBorder(BorderFactory.createEmptyBorder(insets.top, insets.left, insets.bottom, insets.right - 1));
    }

    private void setDesign(JTextField component) {
        if (property != null) {
            if (property.design != null)
                ClientColorUtils.designComponent(component, property.design);

            Integer valueAlignment = property.getSwingValueAlignment();
            if (valueAlignment != null) {
                component.setHorizontalAlignment(valueAlignment);
            }
        }
    }

    private String initValue;

    //based on glazedLists AutoCompleteSupport
    class SuggestBox {
        private EventList<Object> items;
        List<String> latestSuggestions = new ArrayList<>();

        private CustomComboBox comboBox;

        private JTextField comboBoxEditorComponent;

        public SuggestBox(String value) {
            items = GlazedLists.eventListOf();

            comboBox = new CustomComboBox(new DefaultEventComboBoxModel(items));
            comboBox.setEditable(true);
            
            comboBox.setRenderer(new DefaultListCellRenderer() {
                @Override
                public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                    JComponent comp = (JComponent) super.getListCellRendererComponent(list,value, index, isSelected, cellHasFocus);
                    comp.setBorder(SwingDefaults.getTableCellBorder());
                    return comp;
                }
            });
            setBorder(comboBox);

            comboBox.setUI(new BasicComboBoxUI() {
                @Override
                protected ComboPopup createPopup() {
                    BasicComboPopup popup = new BasicComboPopup(comboBox) {
                        @Override
                        protected void configurePopup() {
                            super.configurePopup();
                            JPanel buttonsPanel = new JPanel();
                            setBackgroundColor(buttonsPanel);

                            refreshButton = new SuggestPopupButton(ClientImages.get("refresh.png"), e -> requestSuggestions());
                            buttonsPanel.add(refreshButton);

                            for (int i = 0; i < actions.length; i++) {
                                int index = i;
                                SuggestPopupButton button = new SuggestPopupButton(ClientImages.get(actions[index] + ".png"), e -> {
                                    asyncChange.setContextAction(index);
                                    tableEditor.stopCellEditing();
                                    asyncChange.setContextAction(null);
                                });
                                buttonsPanel.add(button);
                            }

                            JPanel buttonsTopPanel = new JPanel(new BorderLayout());
                            setBackgroundColor(buttonsTopPanel);
                            buttonsTopPanel.add(buttonsPanel, BorderLayout.EAST);

                            add(buttonsTopPanel);
                            setBackground(SwingDefaults.getTableCellBackground());

                            setBorder(BorderFactory.createLineBorder(SwingDefaults.getComponentBorderColor()));
                            setOpaque(true);
                        }

                        //calculate width based on width of elements
                        @Override
                        protected Rectangle computePopupBounds(int px, int py, int pw, int ph) {
                            //default size + 1px left and right for equal width of textBox and popup
                            return super.computePopupBounds(px - 1, py, Math.max(comboBox.getPreferredSize().width, pw + 2), ph);
                        }
                    };
                    popup.getAccessibleContext().setAccessibleParent(comboBox);
                    return popup;
                }

                private void setBackgroundColor(JPanel panel) {
                    panel.setBackground(SwingDefaults.getPanelBackground());
                }
            });

            addListeners(value);
        }

        public boolean isValidValue(String value) {
            return value.isEmpty() || latestSuggestions.contains(value);
        }

        private void setSelectedIndex(int index) {
            int size = comboBox.getModel().getSize();
            if (index < 0) {
                index = size - 1; //last item
            } else if (index >= size) {
                index = 0; //first item
            }
            if(index < size) {
                comboBox.setSelectedIndex(index);
            }
        }

        private class MoveAction extends AbstractAction {
            private final int offset;

            public MoveAction(int offset) {
                this.offset = offset;
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                if (comboBox.isPopupVisible()) {
                    setSelectedIndex(comboBox.getSelectedIndex() + offset);
                }
                if (!strict) {
                    setComboBoxEditorText((String) comboBox.getSelectedItem());
                }
            }
        }

        public String getComboBoxEditorText() {
            return comboBoxEditorComponent.getText();
        }

        public boolean disableUpdate;
        public void setComboBoxEditorText(String value) {
            disableUpdate = true;
            comboBoxEditorComponent.setText(value);
            disableUpdate = false;
        }

        public void updateItems(List<String> result, boolean selectFirst) {
            items.clear();
            comboBox.getModel().setSelectedItem(null);
            items.addAll(GlazedLists.eventListOf(result.toArray()));
            latestSuggestions = result;
            comboBox.setMaximumRowCount(result.size());
            //hide and show to call computePopupBounds
            suggestBox.comboBox.hidePopup();
            suggestBox.comboBox.showPopup();
            if (selectFirst) {
                setSelectedIndex(0);
            }
        }

        public boolean isLoading;
        public void updateLoading(boolean isLoading) {
            if(this.isLoading != isLoading) {
                refreshButton.setIcon(ClientImages.get(isLoading ? "loading.gif" : "refresh.png"));
                this.isLoading = isLoading;
            }
        }

        private void addListeners(String value) {
            //stop editing after item selection
            comboBox.setEditor(new BasicComboBoxEditor() {
                @Override
                public void setItem(Object anObject) {
                    super.setItem(anObject);
                    tableEditor.stopCellEditing();
                }
            });

            //cancel editing when popup is canceled
            comboBox.addPopupMenuListener(new PopupMenuListener() {
                @Override
                public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                }

                @Override
                public void popupMenuCanceled(PopupMenuEvent e) {
                    if(strict) {
                        tableEditor.cancelCellEditing();
                    } else {
                        tableEditor.stopCellEditing();
                    }
                }
            });

            comboBoxEditorComponent = (JTextField) comboBox.getEditor().getEditorComponent();
            setDesign(comboBoxEditorComponent);

            //need to catch key events
            CaptureKeyEventsDispatcher.get().setCapture(comboBoxEditorComponent);

            if (!KeyStrokes.isDeleteEvent(editEvent))
                initValue = value;
            else
                initValue = "";

            // add a FocusListener to the ComboBoxEditor which selects all text when focus is gained
            comboBoxEditorComponent.addFocusListener(new FocusAdapter() {
                @Override
                public void focusGained(FocusEvent e) {
                    if (!KeyStrokes.isSuitableStartFilteringEvent(editEvent)) {
                        comboBoxEditorComponent.selectAll();
                    }
                }
            });

            //catch enter and escape events
            comboBoxEditorComponent.addKeyListener(new KeyAdapter() {
                @Override
                public void keyPressed(KeyEvent e) {
                    if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                        //set selected value from dropdown
                        if(comboBox.getSelectedItem() != null) {
                            setComboBoxEditorText((String) comboBox.getSelectedItem());
                        }
                        if (!strict || isValidValue(getComboBoxEditorText())) {
                            tableEditor.preCommit(true);
                            tableEditor.stopCellEditing();
                            tableEditor.postCommit();
                        }
                        e.consume();
                    } else if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                        tableEditor.cancelCellEditing();
                        e.consume();
                    }
                }
            });

            //updateAsyncValues on every change
            comboBoxEditorComponent.getDocument().addDocumentListener(new DocumentListener() {
                public void changedUpdate(DocumentEvent e) {
                    run();
                }
                public void removeUpdate(DocumentEvent e) {
                    run();
                }
                public void insertUpdate(DocumentEvent e) {
                    run();
                }

                public void run() {
                    requestSuggestions();
                }
            });

            final ActionMap actionMap = comboBox.getActionMap();

            final Action upAction = new MoveAction(-1);
            final Action downAction = new MoveAction(1);

            // install custom actions for the arrow keys in all non-Apple L&Fs
            actionMap.put("selectPrevious", upAction);
            actionMap.put("selectNext", downAction);
            actionMap.put("selectPrevious2", upAction);
            actionMap.put("selectNext2", downAction);

            // install custom actions for the arrow keys in the Apple Aqua L&F
            actionMap.put("aquaSelectPrevious", upAction);
            actionMap.put("aquaSelectNext", downAction);
        }
    }

    private class SuggestPopupButton extends JButton {

        public SuggestPopupButton(Icon icon, ActionListener actionListener) {
            super(icon);
            init(actionListener);
        }

        private void init(ActionListener actionListener) {
            setPreferredSize(new Dimension(24, 24));
            setRequestFocusEnabled(false);
            addActionListener(actionListener);
        }
    }


    private class CustomComboBox extends JComboBox implements AsyncInputComponent {

        public CustomComboBox(ComboBoxModel model) {
            super(model);
        }

        @Override
        public void initEditor() {
            //need because we extend JComboBox
            suggestBox.comboBoxEditorComponent.putClientProperty("doNotCancelPopup",  new JComboBox().getClientProperty("doNotCancelPopup"));

            //show empty async popup
            suggestBox.updateItems(Collections.emptyList(), false);

            requestSuggestions();

            suggestBox.setComboBoxEditorText(initValue);
            suggestBox.comboBoxEditorComponent.selectAll();
        }
    }
}
