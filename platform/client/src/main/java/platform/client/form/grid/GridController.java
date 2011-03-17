package platform.client.form.grid;

import platform.client.form.ClientFormController;
import platform.client.form.ClientFormLayout;
import platform.client.form.GroupObjectController;
import platform.client.form.queries.CalculateSumButton;
import platform.client.form.queries.CountQuantityButton;
import platform.client.form.queries.FilterController;
import platform.client.form.queries.FindController;
import platform.client.logics.ClientGrid;
import platform.client.logics.ClientGroupObjectValue;
import platform.client.logics.ClientPropertyDraw;
import platform.interop.Order;
import platform.interop.form.screen.ExternalScreenComponent;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GridController {

    private final ClientGrid key;

    public ClientGrid getKey() {
        return key;
    }

    private final GridView view;

    public GridView getView() {
        return view;
    }

    private final GridTable table;

    private final ClientFormController form;

    private final GroupObjectController groupObjectController;

    public GridController(ClientGrid key, GroupObjectController igroupObjectController, ClientFormController iform) {

        this.key = key;
        groupObjectController = igroupObjectController;
        form = iform;

        FindController findController = new FindController(groupObjectController) {

            protected boolean queryChanged() {

                form.changeFind(getConditions());

                table.requestFocusInWindow();
                return true;
            }
        };

        FilterController filterController = new FilterController(groupObjectController) {

            protected boolean queryChanged() {

                try {
                    form.changeFilter(groupObjectController.getGroupObject(), getConditions());
                } catch (IOException e) {
                    throw new RuntimeException("Ошибка при применении фильтра", e);
                }

                table.requestFocusInWindow();
                return true;
            }
        };

        CountQuantityButton countQuantity = new CountQuantityButton() {
            public void addListener() {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            showPopupMenu(form.countRecords(groupObjectController.getGroupObject().getID()));
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
            }
        };

        CalculateSumButton calculateSum = new CalculateSumButton() {
            public void addListener() {
                addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        try {
                            ClientPropertyDraw property = getCurrentProperty();
                            Object sum = form.calculateSum(groupObjectController.getGroupObject().getID(), property.getID());
                            showPopupMenu(property.getCaption(), sum);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                });
            }
        };

        view = new GridView(groupObjectController,
                            form,
                            key.showFind ? findController : null,
                            key.showFilter ? filterController : null,
                            key.showCountQuantity ? countQuantity : null,
                            key.showCalculateSum ? calculateSum : null,
                            key.tabVertical,
                            key.groupObject.needVerticalScroll) {
            protected void needToBeShown() {
                if (!hidden && !view.isVisible()) {
                    view.setVisible(true);
                }
            }

            protected void needToBeHidden() {
                view.setVisible(false);
            }
        };
        table = view.getTable();

        if (this.key.minRowCount > 0) { // вообще говоря, так делать неправильно, посколько и HeaderHeight и RowHeight могут изменяться во времени
            Dimension minSize = table.getMinimumSize();
            minSize.height = Math.max(minSize.height, (int) table.getTableHeader().getPreferredSize().getHeight() + this.key.minRowCount * table.getRowHeight());
            view.setMinimumSize(minSize);
        }
    }

    public void addView(ClientFormLayout formLayout) {
        formLayout.add(key, view);
        for (Map.Entry<ClientPropertyDraw, ExternalScreenComponent> entry : extViews.entrySet()) {
            entry.getKey().externalScreen.add(form.getID(), entry.getValue(), entry.getKey().externalScreenConstraints);
        }
    }

    private Map<ClientPropertyDraw, ExternalScreenComponent> extViews = new HashMap<ClientPropertyDraw, ExternalScreenComponent>();

    private void addExternalScreenComponent(ClientPropertyDraw key) {
        if (!extViews.containsKey(key)) {
            ExternalScreenComponent extView = new ExternalScreenComponent();
            extViews.put(key, extView);
        }
    }

    public void addProperty(ClientPropertyDraw property) {
        table.addProperty(property);

        if (property.externalScreen != null) {
            addExternalScreenComponent(property);
        }
    }

    public void removeProperty(ClientPropertyDraw property) {
        table.removeProperty(property);
    }

    public void setGridObjects(List<ClientGroupObjectValue> gridObjects) {
        table.setRowKeys(gridObjects);
    }

    public void updateColumnKeys(ClientPropertyDraw drawProperty, List<ClientGroupObjectValue> groupColumnKeys) {
        table.updateColumnKeys(drawProperty, groupColumnKeys);
    }

    public void updatePropertyCaptions(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> captions) {
        table.updateColumnCaptions(property, captions);
    }

    public void updateCellHighlightValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> cellHighlights) {
        table.updateCellHighlightValues(property, cellHighlights);
    }

    public void updateRowHighlightValues(Map<ClientGroupObjectValue, Object> rowHighlights) {
        table.updateRowHighlightValues(rowHighlights);
    }

    public void selectObject(ClientGroupObjectValue currentObject) {
        table.selectObject(currentObject);
    }

    public void updatePropertyValues(ClientPropertyDraw property, Map<ClientGroupObjectValue, Object> values) {
        table.setColumnValues(property, values);
        if (extViews.containsKey(property)) {
            Object value = getSelectedValue(property);
            extViews.get(property).setValue((value == null) ? "" : value.toString());
            property.externalScreen.invalidate();
        }
    }

    public void changeGridOrder(ClientPropertyDraw property, Order modiType) throws IOException {
        table.changeGridOrder(property, modiType);
    }

    public ClientPropertyDraw getCurrentProperty() {
        return table.getCurrentProperty();
    }

    public Object getSelectedValue(ClientPropertyDraw cell) {
        return table.getSelectedValue(cell);
    }

    public boolean requestFocusInWindow() {
        return table.requestFocusInWindow();
    }

    boolean hidden = false;

    public void hideViews() {
        hidden = true;
        view.setVisible(false);
    }

    public void showViews() {
        hidden = false;
        view.setVisible(true);
    }

    public void update() {
        table.updateTable();
    }
}
