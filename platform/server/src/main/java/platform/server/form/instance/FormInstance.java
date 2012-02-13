package platform.server.form.instance;

import platform.base.*;
import platform.interop.ClassViewType;
import platform.interop.Compare;
import platform.interop.FormEventType;
import platform.interop.Scroll;
import platform.interop.action.ClientAction;
import platform.interop.action.ContinueAutoActionsClientAction;
import platform.interop.action.ResultClientAction;
import platform.interop.action.StopAutoActionsClientAction;
import platform.interop.form.FormUserPreferences;
import platform.server.Message;
import platform.server.ParamMessage;
import platform.server.auth.SecurityPolicy;
import platform.server.caches.ManualLazy;
import platform.server.classes.*;
import platform.server.data.expr.Expr;
import platform.server.data.expr.KeyExpr;
import platform.server.data.expr.ValueExpr;
import platform.server.data.expr.query.GroupExpr;
import platform.server.data.expr.query.GroupType;
import platform.server.data.query.Query;
import platform.server.data.type.ObjectType;
import platform.server.data.type.TypeSerializer;
import platform.server.form.entity.*;
import platform.server.form.entity.filter.FilterEntity;
import platform.server.form.entity.filter.NotFilterEntity;
import platform.server.form.entity.filter.NotNullFilterEntity;
import platform.server.form.entity.filter.RegularFilterGroupEntity;
import platform.server.form.instance.filter.CompareValue;
import platform.server.form.instance.filter.FilterInstance;
import platform.server.form.instance.filter.RegularFilterGroupInstance;
import platform.server.form.instance.filter.RegularFilterInstance;
import platform.server.form.instance.listener.CustomClassListener;
import platform.server.form.instance.listener.FocusListener;
import platform.server.form.instance.listener.FormEventListener;
import platform.server.form.instance.remote.RemoteForm;
import platform.server.logics.BusinessLogics;
import platform.server.logics.DataObject;
import platform.server.logics.ObjectValue;
import platform.server.logics.ServerResourceBundle;
import platform.server.logics.linear.LP;
import platform.server.logics.property.*;
import platform.server.logics.property.derived.MaxChangeProperty;
import platform.server.logics.property.derived.OnChangeProperty;
import platform.server.session.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.*;
import java.util.Map.Entry;

import static platform.base.BaseUtils.mergeSet;
import static platform.interop.ClassViewType.*;
import static platform.interop.Order.ADD;
import static platform.interop.Order.DIR;
import static platform.interop.Order.REPLACE;
import static platform.server.form.instance.GroupObjectInstance.*;
import static platform.server.logics.ServerResourceBundle.getString;

// класс в котором лежит какие изменения произошли

// нужен какой-то объект который
//  разделит клиента и серверную часть кинув каждому свои данные
// так клиента волнуют панели на форме, список гридов в привязке, дизайн и порядок представлений
// сервера колышет дерево и св-ва предст. с привязкой к объектам

public class FormInstance<T extends BusinessLogics<T>> extends OverrideModifier {

    public final T BL;

    SecurityPolicy securityPolicy;

    public CustomClass getCustomClass(int classID) {
        return BL.LM.baseClass.findClassID(classID);
    }

    private Set<Property> hintsIncrementTable;
    private List<Property> hintsIncrementList = null;
    private List<Property> getHintsIncrementList() {
        if(hintsIncrementList==null) {
            hintsIncrementList = new ArrayList<Property>();
            for(Property property : BL.getPropertyList(false))
                if(hintsIncrementTable.contains(property)) // чтобы в лексикографике был список
                    hintsIncrementList.add(property);
        }
        return hintsIncrementList;
    }
    
    Set<Property> hintsNoUpdate = new HashSet<Property>();

    public final DataSession session;
    public final NoUpdate noUpdate = new NoUpdate();
    public final IncrementProps increment = new IncrementProps();
    
    public void addHintNoUpdate(Property property) {
        hintsNoUpdate.add(property);
        noUpdate.add(property);
    }

    public boolean isHintIncrement(Property property) {
        return hintsIncrementTable.contains(property);
    }
    public boolean allowHintIncrement(Property property) {
        return true;
    }
    public void addHintIncrement(Property property) {
        hintsIncrementList = null;
        usedProperties = null;
        boolean noHint = hintsIncrementTable.add(property);
        assert noHint;
        try {
            readIncrement(property);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private <P extends PropertyInterface> void rereadIncrement(Property<P> property) throws SQLException {
        increment.remove(property, session.sql);
        readIncrement(property);
    }

    private <P extends PropertyInterface> void readIncrement(Property<P> property) throws SQLException {
        if(property.hasChanges(this))
            increment.add(property, property.readChangeTable(session.sql, this, BL.LM.baseClass, session.env));
    }

    public Set<Property> getUpdateProperties(PropertyChanges propChanges) {
        return Property.hasChanges(getUsedProperties(), noUpdate.getPropertyChanges().add(propChanges));
    }

    public Set<Property> getUpdateProperties() {
        return Property.hasChanges(getUsedProperties(), getPropertyChanges());
    }

    private final WeakReference<FocusListener<T>> weakFocusListener;
    public FocusListener<T> getFocusListener() {
        return weakFocusListener.get();
    }

    private final WeakReference<CustomClassListener> weakClassListener;
    public CustomClassListener getClassListener() {
        return weakClassListener.get();
    }

    public final FormEntity<T> entity;

    public final InstanceFactory instanceFactory;

    // для импорта конструктор, объекты пустые
    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer) throws SQLException {
        this(entity, BL, session, securityPolicy, focusListener, classListener, computer, new HashMap<ObjectEntity, DataObject>(), false);
    }

    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer, boolean interactive) throws SQLException {
        this(entity, BL, session, securityPolicy, focusListener, classListener, computer, new HashMap<ObjectEntity, DataObject>(), interactive);
    }

    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer, Map<ObjectEntity, ? extends ObjectValue> mapObjects, boolean interactive) throws SQLException {
        this(entity, BL, session, securityPolicy, focusListener, classListener, computer, mapObjects, interactive, null);
    }

    public FormInstance(FormEntity<T> entity, T BL, DataSession session, SecurityPolicy securityPolicy, FocusListener<T> focusListener, CustomClassListener classListener, PropertyObjectInterfaceInstance computer, Map<ObjectEntity, ? extends ObjectValue> mapObjects, boolean interactive, Set<FilterEntity> additionalFixedFilters) throws SQLException {
        lateInit(noUpdate, increment, session);
        this.session = session;
        this.entity = entity;
        this.BL = BL;
        this.securityPolicy = securityPolicy;

        instanceFactory = new InstanceFactory(computer);

        this.weakFocusListener = new WeakReference<FocusListener<T>>(focusListener);
        this.weakClassListener = new WeakReference<CustomClassListener>(classListener);

        fillHints(false);

        for (int i = 0; i < entity.groups.size(); i++) {
            GroupObjectInstance groupObject = instanceFactory.getInstance(entity.groups.get(i));
            groupObject.order = i;
            groupObject.setClassListener(classListener);
            groups.add(groupObject);
        }

        for (TreeGroupEntity treeGroup : entity.treeGroups) {
            treeGroups.add(instanceFactory.getInstance(treeGroup));
        }

        for (PropertyDrawEntity<?> propertyDrawEntity : entity.propertyDraws)
            if (this.securityPolicy.property.view.checkPermission(propertyDrawEntity.propertyObject.property)) {
                PropertyDrawInstance propertyDrawInstance = instanceFactory.getInstance(propertyDrawEntity);
                if (propertyDrawInstance.toDraw == null) // для Instance'ов проставляем не null, так как в runtime'е порядок меняться не будет
                    propertyDrawInstance.toDraw = instanceFactory.getInstance(propertyDrawEntity.getToDraw(entity));
                properties.add(propertyDrawInstance);
            }

        Set<FilterEntity> allFixedFilters = additionalFixedFilters == null
                                            ? entity.fixedFilters
                                            : mergeSet(entity.fixedFilters, additionalFixedFilters);
        for (FilterEntity filterEntity : allFixedFilters) {
            FilterInstance filter = filterEntity.getInstance(instanceFactory);
            filter.getApplyObject().fixedFilters.add(filter);
        }

        for (RegularFilterGroupEntity filterGroupEntity : entity.regularFilterGroups) {
            regularFilterGroups.add(instanceFactory.getInstance(filterGroupEntity));
        }

        for (Entry<OrderEntity<?>, Boolean> orderEntity : entity.fixedOrders.entrySet()) {
            OrderInstance orderInstance = orderEntity.getKey().getInstance(instanceFactory);
            orderInstance.getApplyObject().fixedOrders.put(orderInstance, orderEntity.getValue());
        }

        // в первую очередь ставим на объекты из cache'а
        if (classListener != null) {
            for (GroupObjectInstance groupObject : groups) {
                for (ObjectInstance object : groupObject.objects)
                    if (object.getBaseClass() instanceof CustomClass) {
                        Integer objectID = classListener.getObject((CustomClass) object.getBaseClass());
                        if (objectID != null)
                            groupObject.addSeek(object, session.getDataObject(objectID, ObjectType.instance), false);
                    }
            }
        }

        for (Entry<ObjectEntity, ? extends ObjectValue> mapObject : mapObjects.entrySet()) {
            ObjectInstance instance = instanceFactory.getInstance(mapObject.getKey());
            instance.groupTo.addSeek(instance, mapObject.getValue(), false);
        }

        addObjectOnTransaction(FormEventType.INIT);

        //устанавливаем фильтры и порядки по умолчанию...
        for (RegularFilterGroupInstance filterGroup : regularFilterGroups) {
            int defaultInd = filterGroup.entity.defaultFilter;
            if (defaultInd >= 0 && defaultInd < filterGroup.filters.size()) {
                setRegularFilter(filterGroup, filterGroup.filters.get(defaultInd));
            }
        }

        Set<GroupObjectInstance> wasOrder = new HashSet<GroupObjectInstance>();
        for (Entry<PropertyDrawEntity<?>, Boolean> entry : entity.defaultOrders.entrySet()) {
            PropertyDrawInstance property = instanceFactory.getInstance(entry.getKey());
            GroupObjectInstance toDraw = property.toDraw;
            Boolean ascending = entry.getValue();

            toDraw.changeOrder(property.propertyObject, wasOrder.contains(toDraw) ? ADD : REPLACE);
            if (!ascending) {
                toDraw.changeOrder(property.propertyObject, DIR);
            }
            wasOrder.add(toDraw);
        }

        if(!interactive) {
            endApply();
            this.mapObjects = mapObjects;
        }
        this.interactive = interactive;
    }

    public Map<String, FormUserPreferences> loadUserPreferences() {
        Map<String, FormUserPreferences> preferences = new HashMap<String, FormUserPreferences>();
        try {
            KeyExpr propertyDrawExpr = new KeyExpr("propertyDraw");

            Integer userId = (Integer) BL.LM.currentUser.read(session);
            DataObject currentUser = session.getDataObject(userId, ObjectType.instance);

            Expr customUserExpr = currentUser.getExpr();

            Map<Object, KeyExpr> newKeys = new HashMap<Object, KeyExpr>();
            newKeys.put("propertyDraw", propertyDrawExpr);

            Query<Object, Object> query = new Query<Object, Object>(newKeys);
            query.properties.put("propertyDrawSID", BL.LM.propertyDrawSID.getExpr(propertyDrawExpr));
            query.properties.put("nameShowPropertyDrawCustomUser", BL.LM.nameShowPropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            query.properties.put("columnWidthOverridePropertyDrawCustomUser", BL.LM.columnWidthOverridePropertyDrawCustomUser.getExpr(propertyDrawExpr, customUserExpr));
            
            DataObject formObject = (DataObject) BL.LM.SIDToForm.readClasses(session, new DataObject(entity.getSID(), StringClass.get(50)));
            if (formObject != null)
                query.and(BL.LM.formPropertyDraw.getExpr(propertyDrawExpr).compare(formObject.getExpr(), Compare.EQUALS));

            OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);

            for (Map<Object, Object> values : result.values()) {
                String propertyDrawSID = values.get("propertyDrawSID").toString().trim();
                Boolean needToHide = null;
                Object hide = values.get("nameShowPropertyDrawCustomUser");
                if (hide != null) {
                    if (getString("logics.property.draw.hide").equals(hide.toString().trim()))
                        needToHide = true;
                    else if (getString("logics.property.draw.show").equals(hide.toString().trim()))
                        needToHide = false;
                }
                Integer width = (Integer) values.get("columnWidthOverridePropertyDrawCustomUser");
                preferences.put(propertyDrawSID, new FormUserPreferences(needToHide, width));
            }
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        return preferences;
    }

    public void saveUserPreferences(Map<String, FormUserPreferences> preferences, Boolean forAllUsers) {
        try {
            DataSession dataSession = session.createSession();
            for (Map.Entry<String, FormUserPreferences> entry : preferences.entrySet()) {
                DataObject userObject = dataSession.getDataObject(BL.LM.currentUser.read(dataSession), ObjectType.instance);
                Integer id = (Integer) BL.LM.SIDFormSIDPropertyDrawToPropertyDraw.read(dataSession, new DataObject(entity.getSID(), StringClass.get(50)), new DataObject(entry.getKey(), StringClass.get(50)));
                DataObject propertyDrawObject = dataSession.getDataObject(id, ObjectType.instance);
                if (entry.getValue().isNeedToHide()!=null) {
                    if (!entry.getValue().isNeedToHide())
                        BL.LM.showPropertyDrawCustomUser.execute(BL.LM.propertyDrawShowStatus.getID("Hide"), dataSession, propertyDrawObject, userObject);
                    else
                        BL.LM.showPropertyDrawCustomUser.execute(BL.LM.propertyDrawShowStatus.getID("Show"), dataSession, propertyDrawObject, userObject);
                }
                if (forAllUsers)
                    BL.LM.columnWidthPropertyDraw.execute(entry.getValue().getWidthUser(), dataSession, propertyDrawObject);
                else
                    BL.LM.columnWidthPropertyDrawCustomUser.execute(entry.getValue().getWidthUser(), dataSession, propertyDrawObject, userObject);
            }
            dataSession.apply(BL);
        } catch (SQLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

    private Map<ObjectEntity, ? extends ObjectValue> mapObjects = null;

    public boolean areObjectsFounded() {
        assert !interactive;
        for(Entry<ObjectEntity, ? extends ObjectValue> mapObjectInstance : mapObjects.entrySet())
            if(!instanceFactory.getInstance(mapObjectInstance.getKey()).getObjectValue().equals(mapObjectInstance.getValue()))
                return false;
        return true;
    }

    private boolean interactive = true;

    public List<GroupObjectInstance> groups = new ArrayList<GroupObjectInstance>();
    public List<TreeGroupInstance> treeGroups = new ArrayList<TreeGroupInstance>();

    // собсно этот объект порядок колышет столько же сколько и дизайн представлений
    public List<PropertyDrawInstance> properties = new ArrayList<PropertyDrawInstance>();

    private Collection<ObjectInstance> objects;

    @ManualLazy
    public Collection<ObjectInstance> getObjects() {
        if (objects == null) {
            objects = new ArrayList<ObjectInstance>();
            for (GroupObjectInstance group : groups)
                for (ObjectInstance object : group.objects)
                    objects.add(object);
        }
        return objects;
    }

    public void addFixedFilter(FilterEntity newFilter) {
        FilterInstance newFilterInstance = newFilter.getInstance(instanceFactory);
        newFilterInstance.getApplyObject().fixedFilters.add(newFilterInstance);
    }

    // ----------------------------------- Поиск объектов по ID ------------------------------ //
    public GroupObjectInstance getGroupObjectInstance(int groupID) {
        for (GroupObjectInstance groupObject : groups)
            if (groupObject.getID() == groupID)
                return groupObject;
        return null;
    }

    public ObjectInstance getObjectInstance(int objectID) {
        for (ObjectInstance object : getObjects())
            if (object.getID() == objectID)
                return object;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(int propertyID) {
        for (PropertyDrawInstance property : properties)
            if (property.getID() == propertyID)
                return property;
        return null;
    }

    public RegularFilterGroupInstance getRegularFilterGroup(int groupID) {
        for (RegularFilterGroupInstance filterGroup : regularFilterGroups)
            if (filterGroup.getID() == groupID)
                return filterGroup;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(Property<?> property, ObjectInstance object) {
        for (PropertyDrawInstance propertyDraw : properties)
            if (property.equals(propertyDraw.propertyObject.property) && propertyDraw.propertyObject.mapping.containsValue(object))
                return propertyDraw;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(Property<?> property, GroupObjectInstance group) {
        for (PropertyDrawInstance propertyDraw : properties)
            if (property.equals(propertyDraw.propertyObject.property) && (group==null || group.equals(propertyDraw.toDraw)))
                return propertyDraw;
        return null;
    }

    public PropertyDrawInstance getPropertyDraw(Property<?> property) {
        return getPropertyDraw(property, (GroupObjectInstance)null);
    }

    public PropertyDrawInstance getPropertyDraw(LP<?> property) {
        return getPropertyDraw(property.property);
    }

    public PropertyDrawInstance getPropertyDraw(LP<?> property, ObjectInstance object) {
        return getPropertyDraw(property.property, object);
    }

    public PropertyDrawInstance getPropertyDraw(LP<?> property, GroupObjectInstance group) {
        return getPropertyDraw(property.property, group);
    }



    public void serializePropertyEditorType(DataOutputStream outStream, PropertyDrawInstance<?> propertyDraw, Map<ObjectInstance, DataObject> keys, boolean aggValue) throws SQLException, IOException {
        PropertyObjectInstance<?> change = propertyDraw.getChangeInstance(aggValue, BL, keys, this);
        boolean isReadOnly = propertyDraw.isReadOnly() || (propertyDraw.propertyReadOnly!=null && propertyDraw.propertyReadOnly.read(session, this)!=null);
        if (!isReadOnly && securityPolicy.property.change.checkPermission(change.property) &&
                (entity.isActionOnChange(change.property) || change.getValueImplement().canBeChanged(this))) {
            outStream.writeBoolean(false);
            TypeSerializer.serializeType(outStream, change.getEditorType());
        } else {
            outStream.writeBoolean(true);
        }
    }

    // ----------------------------------- Навигация ----------------------------------------- //

    public void changeGroupObject(GroupObjectInstance group, Scroll changeType) throws SQLException {
        switch (changeType) {
            case HOME:
                group.seek(false);
                break;
            case END:
                group.seek(true);
                break;
        }
    }

    public void expandGroupObject(GroupObjectInstance group, Map<ObjectInstance, DataObject> value) throws SQLException {
        if(group.expandTable==null)
            group.expandTable = group.createKeyTable();
        group.expandTable.insertRecord(session.sql, value, true, true);
        group.updated |= UPDATED_EXPANDS;
    }

    public void switchClassView(GroupObjectInstance group) {
        ClassViewType newClassView = switchView(group.curClassView);
        if (group.entity.isAllowedClassView(newClassView)) {
            changeClassView(group, newClassView);
        }
    }

    public void changeClassView(GroupObjectInstance group, ClassViewType show) {

        group.curClassView = show;
        group.updated = group.updated | UPDATED_CLASSVIEW;
    }

    // сстандартные фильтры
    public List<RegularFilterGroupInstance> regularFilterGroups = new ArrayList<RegularFilterGroupInstance>();
    private Map<RegularFilterGroupInstance, RegularFilterInstance> regularFilterValues = new HashMap<RegularFilterGroupInstance, RegularFilterInstance>();

    public void setRegularFilter(RegularFilterGroupInstance filterGroup, int filterId) {
        setRegularFilter(filterGroup, filterGroup.getFilter(filterId));
    }

    public void setRegularFilter(RegularFilterGroupInstance filterGroup, RegularFilterInstance filter) {

        RegularFilterInstance prevFilter = regularFilterValues.get(filterGroup);
        if (prevFilter != null)
            prevFilter.filter.getApplyObject().removeRegularFilter(prevFilter.filter);

        if (filter == null) {
            regularFilterValues.remove(filterGroup);
        } else {
            regularFilterValues.put(filterGroup, filter);
            filter.filter.getApplyObject().addRegularFilter(filter.filter);
        }
    }

    // -------------------------------------- Изменение данных ----------------------------------- //

    // пометка что изменились данные
    public boolean dataChanged = true;

    private DataObject createObject(ConcreteCustomClass cls) throws SQLException {

        if (!securityPolicy.cls.edit.add.checkPermission(cls)) return null;

        return session.addObject(cls, this);
    }

    private void resolveAdd(CustomObjectInstance object, ConcreteCustomClass cls, DataObject addObject) throws SQLException {

        // резолвим все фильтры
        for (FilterInstance filter : object.groupTo.getSetFilters())
            if (!FilterInstance.ignoreInInterface || filter.isInInterface(object.groupTo)) // если ignoreInInterface проверить что в интерфейсе
                filter.resolveAdd(session, this, object, addObject);

        // todo : теоретически надо переделывать
        // нужно менять текущий объект, иначе не будет работать ImportFromExcelActionProperty
        object.changeValue(session, addObject);

        object.groupTo.addSeek(object, addObject, false);

        // меняем вид, если при добавлении может получиться, что фильтр не выполнится, нужно как-то проверить в общем случае
//      changeClassView(object.groupTo, ClassViewType.PANEL);

        dataChanged = true;
    }

    // добавляет во все
    public DataObject addObject(ConcreteCustomClass cls) throws SQLException {

        DataObject addObject = createObject(cls);
        if (addObject == null) return addObject;

        for (ObjectInstance object : getObjects()) {
            if (object instanceof CustomObjectInstance && cls.isChild(((CustomObjectInstance) object).baseClass)) {
                resolveAdd((CustomObjectInstance) object, cls, addObject);
            }
        }

        return addObject;
    }

    public DataObject addObject(CustomObjectInstance object, ConcreteCustomClass cls) throws SQLException {
        // пока тупо в базу

        DataObject addObject = createObject(cls);
        if (addObject == null) return addObject;

        resolveAdd(object, cls, addObject);

        return addObject;
    }

    public void changeClass(CustomObjectInstance object, DataObject change, int classID) throws SQLException {
        if (securityPolicy.cls.edit.change.checkPermission(object.currentClass)) {
            object.changeClass(session, change, classID);
            dataChanged = true;
        }
    }

    public boolean canChangeClass(CustomObjectInstance object) {
        return securityPolicy.cls.edit.change.checkPermission(object.currentClass);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Object value, boolean aggValue) throws SQLException {
        return changeProperty(property, value, null, false, aggValue);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Object value, RemoteForm executeForm, boolean all, boolean aggValue) throws SQLException {
        return changeProperty(property, new HashMap<ObjectInstance, DataObject>(), value, executeForm, all, aggValue);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Map<ObjectInstance, DataObject> mapDataValues,
                                             PropertyDrawInstance<?> value, Map<ObjectInstance, DataObject> valueColumnKeys, RemoteForm executeForm, boolean all, boolean aggValue) throws SQLException {
        return changeProperty(property, mapDataValues, value.getChangeInstance(aggValue, BL, valueColumnKeys, this), executeForm, all, aggValue);
    }

    public List<ClientAction> changeProperty(PropertyDrawInstance<?> property, Map<ObjectInstance, DataObject> mapDataValues, Object value, RemoteForm executeForm, boolean all, boolean aggValue) throws SQLException {
        assert !property.isReadOnly();
        return changeProperty(property.getChangeInstance(aggValue, BL, mapDataValues, this), value, executeForm, all ? property.toDraw : null);
    }

    @Message("message.form.change.property")
    public List<ClientAction> changeProperty(@ParamMessage PropertyObjectInstance<?> property, Object value, RemoteForm executeForm, GroupObjectInstance groupObject) throws SQLException {
        if (securityPolicy.property.change.checkPermission(property.property)) {
            dataChanged = true;
            return property.execute(session, value instanceof CompareValue? (CompareValue) value : session.getObjectValue(value, property.getType()), this, executeForm, groupObject);
        } else {
            return null;
        }
    }

    public void pasteExternalTable(List<Integer> propertyIDs, List<List<Object>> table) throws SQLException {
        List<PropertyDrawInstance> properties = new ArrayList<PropertyDrawInstance>();
        for (Integer id : propertyIDs) {
            properties.add(getPropertyDraw(id));
        }
        GroupObjectInstance groupObject = properties.get(0).toDraw;
        OrderedMap<Map<ObjectInstance, DataObject>, Map<OrderInstance, ObjectValue>> executeList = groupObject.seekObjects(session.sql, session.env, this, BL.LM.baseClass, table.size());

        //создание объектов
        int availableQuantity = executeList.size();
        if (availableQuantity < table.size()) {
            executeList.putAll(groupObject.createObjects(session, this, table.size() - availableQuantity));
        }

        for (Map<ObjectInstance, DataObject> key : executeList.keySet()) {
            List<Object> row = table.get(executeList.indexOf(key));
            for (PropertyDrawInstance property : properties) {
                PropertyObjectInstance propertyObjectInstance = property.getPropertyObjectInstance();

                for (ObjectInstance groupKey : (Collection<ObjectInstance>) propertyObjectInstance.mapping.values()) {
                    if (!key.containsKey(groupKey)) {
                        key.put(groupKey, groupKey.getDataObject());
                    }
                }

                int propertyIndex = properties.indexOf(property);
                if (propertyIndex < row.size() //если вдруг копировали не таблицу - может быть разное кол-во значений в строках
                        && !(propertyObjectInstance.getType() instanceof ActionClass)
                        && !property.isReadOnly() && securityPolicy.property.change.checkPermission(property.getPropertyObjectInstance().property)) {
                    dataChanged = true;
                    Object value = row.get(propertyIndex);
                    propertyObjectInstance.property.execute(BaseUtils.join(propertyObjectInstance.mapping, key), session, value, this);
                }
            }
        }
    }

    public void pasteMulticellValue(Map<Integer, List<Map<Integer, Object>>> cells, Object value) throws SQLException {
        for (Integer propertyId : cells.keySet()) {
            PropertyDrawInstance property = getPropertyDraw(propertyId);
            PropertyObjectInstance propertyObjectInstance = property.getPropertyObjectInstance();
            for (Map<Integer, Object> keyIds : cells.get(propertyId)) {
                Map<ObjectInstance, DataObject> key = new HashMap<ObjectInstance, DataObject>();
                for (Integer objectId : keyIds.keySet()) {
                    ObjectInstance objectInstance = getObjectInstance(objectId);
                    key.put(objectInstance, session.getDataObject(keyIds.get(objectId), objectInstance.getType()));
                }
                for (ObjectInstance groupKey : (Collection<ObjectInstance>) propertyObjectInstance.mapping.values()) {
                    if (!key.containsKey(groupKey)) {
                        key.put(groupKey, groupKey.getDataObject());
                    }
                }
                if (!(propertyObjectInstance.getType() instanceof ActionClass)
                        && !property.isReadOnly() && securityPolicy.property.change.checkPermission(property.getPropertyObjectInstance().property)) {
                    propertyObjectInstance.property.execute(BaseUtils.join(propertyObjectInstance.mapping, key), session, value, this);
                    dataChanged = true;
                }
            }
        }
    }

    public int countRecords(int groupObjectID) throws SQLException {
        GroupObjectInstance group = getGroupObjectInstance(groupObjectID);
        Expr expr = GroupExpr.create(new HashMap(), new ValueExpr(1, IntegerClass.instance), group.getWhere(group.getMapKeys(), this), GroupType.SUM, new HashMap());
        Query<Object, Object> query = new Query<Object, Object>(new HashMap<Object, KeyExpr>());
        query.properties.put("quant", expr);
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql, session.env);
        Integer quantity = (Integer) result.getValue(0).get("quant");
        if (quantity != null) {
            return quantity;
        } else {
            return 0;
        }
    }

    public Object calculateSum(PropertyDrawInstance propertyDraw, Map<ObjectInstance, DataObject> columnKeys) throws SQLException {
        GroupObjectInstance groupObject = propertyDraw.toDraw;

        Map<ObjectInstance, KeyExpr> mapKeys = groupObject.getMapKeys();
        Map<ObjectInstance, Expr> keys = new HashMap<ObjectInstance, Expr>(mapKeys);

        for (ObjectInstance object : columnKeys.keySet()) {
            keys.put(object, columnKeys.get(object).getExpr());
        }
        Expr expr = GroupExpr.create(new HashMap(), propertyDraw.propertyObject.getExpr(keys, this), groupObject.getWhere(mapKeys, this), GroupType.SUM, new HashMap());

        Query<Object, Object> query = new Query<Object, Object>(new HashMap<Object, KeyExpr>());
        query.properties.put("sum", expr);
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql);
        return result.getValue(0).get("sum");
    }

    public Map<List<Object>, List<Object>> groupData(Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> toGroup,
                                                     Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> toSum,
                                                     Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> toMax, boolean onlyNotNull) throws SQLException {
        GroupObjectInstance groupObject = ((PropertyDrawInstance) toGroup.keySet().toArray()[0]).toDraw;
        Map<ObjectInstance, KeyExpr> mapKeys = groupObject.getMapKeys();

        Map<Object, KeyExpr> keyExprMap = new HashMap<Object, KeyExpr>();
        Map<Object, Expr> exprMap = new HashMap<Object, Expr>();
        for (PropertyDrawInstance property : toGroup.keySet()) {
            int i = 0;
            for (Map<ObjectInstance, DataObject> columnKeys : toGroup.get(property)) {
                i++;
                Map<ObjectInstance, Expr> keys = new HashMap<ObjectInstance, Expr>(mapKeys);
                for (ObjectInstance object : columnKeys.keySet()) {
                    keys.put(object, columnKeys.get(object).getExpr());
                }
                keyExprMap.put(property.getsID() + i, new KeyExpr("expr"));
                exprMap.put(property.getsID() + i, property.propertyObject.getExpr(keys, this));
            }
        }

        Query<Object, Object> query = new Query<Object, Object>(keyExprMap);
        Expr exprQuant = GroupExpr.create(exprMap, new ValueExpr(1, IntegerClass.instance), groupObject.getWhere(mapKeys, this), GroupType.SUM, keyExprMap);
        query.and(exprQuant.getWhere());

        int separator = toSum.size();
        int idIndex = 0;
        for (int i = 0; i < toSum.size() + toMax.size(); i++) {
            Map<PropertyDrawInstance, List<Map<ObjectInstance, DataObject>>> currentMap;
            int index;
            GroupType groupType;
            if (i < separator) {
                currentMap = toSum;
                groupType = GroupType.SUM;
                index = i;
            } else {
                currentMap = toMax;
                groupType = GroupType.MAX;
                index = i - separator;
            }
            PropertyDrawInstance property = (PropertyDrawInstance) currentMap.keySet().toArray()[index];
            if (property == null) {
                query.properties.put("quant", exprQuant);
                continue;
            }
            for (Map<ObjectInstance, DataObject> columnKeys : currentMap.get(property)) {
                idIndex++;
                Map<ObjectInstance, Expr> keys = new HashMap<ObjectInstance, Expr>(mapKeys);
                for (ObjectInstance object : columnKeys.keySet()) {
                    keys.put(object, columnKeys.get(object).getExpr());
                }
                Expr expr = GroupExpr.create(exprMap, property.propertyObject.getExpr(keys, this), groupObject.getWhere(mapKeys, this), groupType, keyExprMap);
                query.properties.put(property.getsID() + idIndex, expr);
                if (onlyNotNull) {
                    query.and(expr.getWhere());
                }
            }
        }

        Map<List<Object>, List<Object>> resultMap = new OrderedMap<List<Object>, List<Object>>();
        OrderedMap<Map<Object, Object>, Map<Object, Object>> result = query.execute(session.sql, session.env);
        for (Map<Object, Object> one : result.keyList()) {
            List<Object> groupList = new ArrayList<Object>();
            List<Object> sumList = new ArrayList<Object>();

            for (PropertyDrawInstance propertyDraw : toGroup.keySet()) {
                for (int i = 1; i <= toGroup.get(propertyDraw).size(); i++) {
                    groupList.add(one.get(propertyDraw.getsID() + i));
                }
            }
            int index = 1;
            for (PropertyDrawInstance propertyDraw : toSum.keySet()) {
                if (propertyDraw == null) {
                    sumList.add(result.get(one).get("quant"));
                    continue;
                }
                for (int i = 1; i <= toSum.get(propertyDraw).size(); i++) {
                    sumList.add(result.get(one).get(propertyDraw.getsID() + index));
                    index++;
                }
            }
            for (PropertyDrawInstance propertyDraw : toMax.keySet()) {
                for (int i = 1; i <= toMax.get(propertyDraw).size(); i++) {
                    sumList.add(result.get(one).get(propertyDraw.getsID() + index));
                    index++;
                }
            }
            resultMap.put(groupList, sumList);
        }
        return resultMap;
    }

    // Обновление данных
    public void refreshData() throws SQLException {

        for (ObjectInstance object : getObjects())
            if (object instanceof CustomObjectInstance)
                ((CustomObjectInstance) object).refreshValueClass(session);
        refresh = true;
        dataChanged = session.hasChanges();
    }

    void addObjectOnTransaction(FormEventType event) throws SQLException {
        for (ObjectInstance object : getObjects()) {
            if (object instanceof CustomObjectInstance) {
                CustomObjectInstance customObject = (CustomObjectInstance) object;
                if (customObject.isAddOnEvent(event)) {
                    addObject(customObject, (ConcreteCustomClass) customObject.gridClass);
                }
            }
            if (object.isResetOnApply())
                object.groupTo.dropSeek(object);
        }
    }

    public String checkApply() throws SQLException {
        return session.check(BL);
    }

    public void synchronizedApplyChanges(String clientResult, List<ClientAction> actions) throws SQLException {
        if (entity.isSynchronizedApply)
            synchronized (entity) {
                applyChanges(clientResult, actions);
            }
        else
            applyChanges(clientResult, actions);
    }

    private void fillHints(boolean restart) throws SQLException {
        if(restart) {
            increment.cleanIncrementTables(session.sql);
            noUpdate.clear();
            
            hintsIncrementList = null;
            usedProperties = null;
        }

        hintsIncrementTable = new HashSet<Property>(entity.hintsIncrementTable);
        hintsNoUpdate = new HashSet<Property>(entity.hintsNoUpdate);
        noUpdate.addAll(hintsNoUpdate);
    }
    
    public void applyChanges(String checkResult, List<ClientAction> actions) throws SQLException {
        if(checkResult == null)
            checkResult = session.apply(BL, actions);

        if (checkResult != null) {
            actions.add(new ResultClientAction(checkResult, true));
            actions.add(new StopAutoActionsClientAction());
            return;
        }

        fillHints(true);

        refreshData();
        addObjectOnTransaction(FormEventType.APPLY);

        dataChanged = true; // временно пока applyChanges синхронен, для того чтобы пересылался факт изменения данных

        actions.add(new ResultClientAction(ServerResourceBundle.getString("form.instance.changes.saved"), false));
    }

    public void cancelChanges() throws SQLException {
        session.restart(true);

        fillHints(true);

        // пробежим по всем объектам
        for (ObjectInstance object : getObjects())
            if (object instanceof CustomObjectInstance)
                ((CustomObjectInstance) object).updateCurrentClass(session);
        addObjectOnTransaction(FormEventType.CANCEL);

        dataChanged = true;
    }

    // ------------------ Через эти методы сообщает верхним объектам об изменениях ------------------- //

    // В дальнейшем наверное надо будет переделать на Listener'ы...
    protected void objectChanged(ConcreteCustomClass cls, Integer objectID) {
    }

    public void changePageSize(GroupObjectInstance groupObject, Integer pageSize) {
        groupObject.setPageSize(pageSize);
    }

    public void gainedFocus() {
        dataChanged = true;
        FocusListener<T> focusListener = getFocusListener();
        if(focusListener!=null)
            focusListener.gainedFocus(this);
    }

    void close() throws SQLException {

        session.incrementChanges.remove(this);
        for (GroupObjectInstance group : groups) {
            if(group.keyTable!=null)
                group.keyTable.drop(session.sql);
            if(group.expandTable!=null)
                group.expandTable.drop(session.sql);
        }
    }

    // --------------------------------------------------------------------------------------- //
    // --------------------- Общение в обратную сторону с ClientForm ------------------------- //
    // --------------------------------------------------------------------------------------- //

    public ConcreteCustomClass getObjectClass(ObjectInstance object) {

        if (!(object instanceof CustomObjectInstance))
            return null;

        return ((CustomObjectInstance) object).currentClass;
    }

    private Collection<Property> usedProperties;
    public Collection<Property> getUsedProperties() {
        if(usedProperties == null) {
            usedProperties = new HashSet<Property>();
            for (PropertyDrawInstance<?> propertyDraw : properties) {
                usedProperties.add(propertyDraw.propertyObject.property);
                if (propertyDraw.propertyCaption != null) {
                    usedProperties.add(propertyDraw.propertyCaption.property);
                }
                if (propertyDraw.propertyReadOnly != null) {
                    usedProperties.add(propertyDraw.propertyReadOnly.property);
                }
                if (propertyDraw.propertyFooter != null) {
                    usedProperties.add(propertyDraw.propertyFooter.property);
                }
                if (propertyDraw.propertyHighlight != null) {
                    usedProperties.add(propertyDraw.propertyHighlight.property);
                }
            }
            for (GroupObjectInstance group : groups) {
                if (group.propertyHighlight != null) {
                    usedProperties.add(group.propertyHighlight.property);
                }
                group.fillUpdateProperties((Set<Property>) usedProperties);
            }
            usedProperties.addAll(hintsIncrementTable); // собственно пока только hintsIncrementTable не позволяет сделать usedProperties просто IdentityLazy
        }
        return usedProperties;
    }
    public FormInstance<T> createForm(FormEntity<T> form, Map<ObjectEntity, DataObject> mapObjects, boolean newSession, boolean interactive) throws SQLException {
        return new FormInstance<T>(form, BL, newSession ? session.createSession() : session, securityPolicy, getFocusListener(), getClassListener(), instanceFactory.computer, mapObjects, interactive);
    }

    public void forceChangeObject(ObjectInstance object, ObjectValue value) throws SQLException {

        if(object instanceof DataObjectInstance && !(value instanceof DataObject))
            object.changeValue(session, ((DataObjectInstance)object).getBaseClass().getDefaultObjectValue());
        else
            object.changeValue(session, value);

        object.groupTo.addSeek(object, value, false);
    }


    public void seekObject(ValueClass cls, ObjectValue value) throws SQLException {

        for (ObjectInstance object : getObjects()) {
            if (object.getBaseClass().isCompatibleParent(cls))
                seekObject(object, value);
        }
    }

    // todo : временная затычка
    public void seekObject(ObjectInstance object, ObjectValue value) throws SQLException {

        if(entity.eventActions.size() > 0) { // дебилизм конечно но пока так
            forceChangeObject(object, value);
        } else {
            object.groupTo.addSeek(object, value, false);
        }
    }

    public List<ClientAction> changeObject(ObjectInstance object, ObjectValue value, RemoteForm form) throws SQLException {
        seekObject(object, value);
        // запускаем все Action'ы, которые следят за этим объектом
        return fireObjectChanged(object, form);
    }

    public void fullRefresh() {
        try {
            refreshData();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        dataChanged = session.hasChanges();
    }

    // "закэшированная" проверка присутствия в интерфейсе, отличается от кэша тем что по сути функция от mutable объекта
    protected Map<PropertyDrawInstance, Boolean> isDrawed = new HashMap<PropertyDrawInstance, Boolean>();

    boolean refresh = true;

    private boolean classUpdated(Updated updated, GroupObjectInstance groupObject) {
        return updated.classUpdated(Collections.singleton(groupObject));
    }

    private boolean objectUpdated(Updated updated, GroupObjectInstance groupObject) {
        return updated.objectUpdated(Collections.singleton(groupObject));
    }

    private boolean objectUpdated(Updated updated, Set<GroupObjectInstance> groupObjects) {
        return updated.objectUpdated(groupObjects);
    }

    private boolean propertyUpdated(PropertyObjectInstance updated, Set<GroupObjectInstance> groupObjects, Collection<Property> changedProps) {
        return dataUpdated(updated, changedProps)
                || groupUpdated(groupObjects, UPDATED_KEYS)
                || objectUpdated(updated, groupObjects);
    }

    private boolean groupUpdated(Collection<GroupObjectInstance> groupObjects, int flags) {
        for (GroupObjectInstance groupObject : groupObjects)
            if ((groupObject.updated & flags) != 0)
                return true;
        return false;
    }

    private boolean dataUpdated(Updated updated, Collection<Property> changedProps) {
        return updated.dataUpdated(changedProps);
    }

    void applyFilters() {
        for (GroupObjectInstance group : groups)
            group.filters = group.getSetFilters();
    }

    void applyOrders() {
        for (GroupObjectInstance group : groups)
            group.orders = group.getSetOrders();
    }

    private static class GroupObjectValue {
        private GroupObjectInstance group;
        private Map<ObjectInstance, DataObject> value;

        private GroupObjectValue(GroupObjectInstance group, Map<ObjectInstance, DataObject> value) {
            this.group = group;
            this.value = value;
        }
    }

    @Message("message.form.increment.read.properties")
    private <P extends PropertyInterface> void updateIncrementTableProps(Collection<Property> changedProps) throws SQLException {
        for(Property<P> property : getHintsIncrementList()) // в changedProps могут быть и cancel'ы и новые изменения
            if(refresh || changedProps.contains(property))
                rereadIncrement(property);
    }

    @Message("message.form.update.props")
    private void updateDrawProps(FormChanges result, Set<GroupObjectInstance> keyGroupObjects, @ParamMessage Set<PropertyReaderInstance> propertyList) throws SQLException {
        Query<ObjectInstance, PropertyReaderInstance> selectProps = new Query<ObjectInstance, PropertyReaderInstance>(GroupObjectInstance.getObjects(getUpTreeGroups(keyGroupObjects)));
        for (GroupObjectInstance keyGroup : keyGroupObjects) {
            NoPropertyTableUsage<ObjectInstance> groupTable = keyGroup.keyTable;
            selectProps.and(groupTable.getWhere(selectProps.mapKeys));
        }

        for (PropertyReaderInstance propertyReader : propertyList) {
            selectProps.properties.put(propertyReader, propertyReader.getPropertyObjectInstance().getExpr(selectProps.mapKeys, this));
        }

        OrderedMap<Map<ObjectInstance, DataObject>, Map<PropertyReaderInstance, ObjectValue>> queryResult = selectProps.executeClasses(session.sql, session.env, BL.LM.baseClass);
        for (PropertyReaderInstance propertyReader : propertyList) {
            Map<Map<ObjectInstance, DataObject>, ObjectValue> propertyValues = new HashMap<Map<ObjectInstance, DataObject>, ObjectValue>();
            for (Entry<Map<ObjectInstance, DataObject>, Map<PropertyReaderInstance, ObjectValue>> resultRow : queryResult.entrySet())
                propertyValues.put(resultRow.getKey(), resultRow.getValue().get(propertyReader));
            result.properties.put(propertyReader, propertyValues);
        }
    }

    @Message("message.form.end.apply")
    public FormChanges endApply() throws SQLException {

        assert interactive;

        final FormChanges result = new FormChanges();

        // если изменились данные, применяем изменения
        Collection<Property> changedProps;
        Collection<CustomClass> changedClasses = new HashSet<CustomClass>();
        if (dataChanged) {
            changedProps = session.update(this, changedClasses);

            updateIncrementTableProps(changedProps);
        } else
            changedProps = new ArrayList<Property>();

        GroupObjectValue updateGroupObject = null; // так как текущий groupObject идет относительно treeGroup, а не group
        for (GroupObjectInstance group : groups) {
            if (refresh) {
                //обновляем classViews при refresh
                result.classViews.put(group, group.curClassView);
            }

            Map<ObjectInstance, DataObject> selectObjects = group.updateKeys(session.sql, session.env, this, BL.LM.baseClass, refresh, result, changedProps, changedClasses);
            if(selectObjects!=null) // то есть нужно изменять объект
                updateGroupObject = new GroupObjectValue(group, selectObjects);

            if (group.getDownTreeGroups().size() == 0 && updateGroupObject != null) { // так как в tree группе currentObject друг на друга никак не влияют, то можно и нужно делать updateGroupObject в конце
                updateGroupObject.group.update(session, result, updateGroupObject.value);
                updateGroupObject = null;
            }
        }

        for (Entry<Set<GroupObjectInstance>, Set<PropertyReaderInstance>> entry : BaseUtils.groupSet(getChangedDrawProps(result, changedProps)).entrySet())
            updateDrawProps(result, entry.getKey(), entry.getValue());

        if (dataChanged)
            result.dataChanged = session.hasStoredChanges();

        // сбрасываем все пометки
        for (GroupObjectInstance group : groups) {
            group.userSeeks = null;

            for (ObjectInstance object : group.objects)
                object.updated = 0;
            group.updated = 0;
        }
        refresh = false;
        dataChanged = false;

//        result.out(this);

        return result;
    }

    private Map<PropertyReaderInstance, Set<GroupObjectInstance>> getChangedDrawProps(FormChanges result, Collection<Property> changedProps) {
        final Map<PropertyReaderInstance, Set<GroupObjectInstance>> readProperties = new HashMap<PropertyReaderInstance, Set<GroupObjectInstance>>();

        for (PropertyDrawInstance<?> drawProperty : properties) {
            if (drawProperty.toDraw != null && drawProperty.toDraw.curClassView == HIDE) continue;

            ClassViewType forceViewType = drawProperty.getForceViewType();
            if (forceViewType != null && forceViewType == HIDE) continue;

            Set<GroupObjectInstance> columnGroupGrids = new HashSet<GroupObjectInstance>();
            for (GroupObjectInstance columnGroup : drawProperty.columnGroupObjects)
                if (columnGroup.curClassView == GRID)
                    columnGroupGrids.add(columnGroup);

            Boolean inInterface = null; Set<GroupObjectInstance> drawGridObjects = null;
            if (drawProperty.toDraw != null && drawProperty.toDraw.curClassView == GRID && (forceViewType == null || forceViewType == GRID) &&
                    drawProperty.propertyObject.isInInterface(drawGridObjects = BaseUtils.addSet(columnGroupGrids, drawProperty.toDraw), forceViewType != null)) // в grid'е
                inInterface = true;
            else if (drawProperty.propertyObject.isInInterface(drawGridObjects = columnGroupGrids, false)) // в панели
                inInterface = false;

            Boolean previous = isDrawed.put(drawProperty, inInterface);
            if(inInterface!=null) {
                boolean read = refresh || !inInterface.equals(previous) // если изменилось представление
                        || groupUpdated(drawProperty.columnGroupObjects, UPDATED_CLASSVIEW); // изменились группы в колонки (так как отбираются только GRID)
                if(read || propertyUpdated(drawProperty.propertyObject, drawGridObjects, changedProps)) {
                    readProperties.put(drawProperty, drawGridObjects);
                    if(!inInterface) // говорим клиенту что свойство в панели
                        result.panelProperties.add(drawProperty);
                }

                if (drawProperty.propertyCaption != null && (read || propertyUpdated(drawProperty.propertyCaption, columnGroupGrids, changedProps)))
                    readProperties.put(drawProperty.captionReader, columnGroupGrids);
                if (drawProperty.propertyFooter != null && (read || propertyUpdated(drawProperty.propertyFooter, columnGroupGrids, changedProps)))
                    readProperties.put(drawProperty.footerReader, columnGroupGrids);
                if (drawProperty.propertyHighlight != null && (read || propertyUpdated(drawProperty.propertyHighlight, drawGridObjects, changedProps))) {
                    readProperties.put(drawProperty.highlightReader, drawGridObjects);
                    if (!inInterface) {
                        result.panelProperties.add(drawProperty.highlightReader);
                    }
                }
            } else if (previous!=null) // говорим клиенту что свойство надо удалить
                result.dropProperties.add(drawProperty);
        }

        for (GroupObjectInstance group : groups) // читаем highlight'ы
            if (group.propertyHighlight != null) {
                Set<GroupObjectInstance> gridGroups = (group.curClassView == GRID ? Collections.singleton(group) : new HashSet<GroupObjectInstance>());
                if (refresh || (group.updated & UPDATED_CLASSVIEW) != 0 || propertyUpdated(group.propertyHighlight, gridGroups, changedProps))
                    readProperties.put(group, gridGroups);
            }

        return readProperties;
    }

    // возвращает какие объекты на форме показываются
    private Set<GroupObjectInstance> getPropertyGroups() {

        Set<GroupObjectInstance> reportObjects = new HashSet<GroupObjectInstance>();
        for (GroupObjectInstance group : groups)
            if (group.curClassView != HIDE)
                reportObjects.add(group);

        return reportObjects;
    }

    // возвращает какие объекты на форме не фиксируются
    private Set<GroupObjectInstance> getClassGroups() {

        Set<GroupObjectInstance> reportObjects = new HashSet<GroupObjectInstance>();
        for (GroupObjectInstance group : groups)
            if (group.curClassView == GRID)
                reportObjects.add(group);

        return reportObjects;
    }

    // считывает все данные с формы
    public FormData getFormData(Collection<PropertyDrawInstance> propertyDraws, Set<GroupObjectInstance> classGroups) throws SQLException {

        applyFilters();
        applyOrders();

        // пока сделаем тупо получаем один большой запрос

        Query<ObjectInstance, Object> query = new Query<ObjectInstance, Object>(GroupObjectInstance.getObjects(classGroups));
        OrderedMap<Object, Boolean> queryOrders = new OrderedMap<Object, Boolean>();

        for (GroupObjectInstance group : groups) {

            if (classGroups.contains(group)) {

                // не фиксированные ключи
                query.and(group.getWhere(query.mapKeys, this));

                // закинем Order'ы
                for (Entry<OrderInstance, Boolean> order : group.orders.entrySet()) {
                    query.properties.put(order.getKey(), order.getKey().getExpr(query.mapKeys, this));
                    queryOrders.put(order.getKey(), order.getValue());
                }

                for (ObjectInstance object : group.objects) {
                    query.properties.put(object, object.getExpr(query.mapKeys, this));
                    queryOrders.put(object, false);
                }
            }
        }

        FormData result = new FormData();

        for (PropertyDrawInstance<?> property : propertyDraws)
            query.properties.put(property, property.propertyObject.getExpr(query.mapKeys, this));

        OrderedMap<Map<ObjectInstance, Object>, Map<Object, Object>> resultSelect = query.execute(session.sql, queryOrders, 0, session.env);
        for (Entry<Map<ObjectInstance, Object>, Map<Object, Object>> row : resultSelect.entrySet()) {
            Map<ObjectInstance, Object> groupValue = new HashMap<ObjectInstance, Object>();
            for (GroupObjectInstance group : groups)
                for (ObjectInstance object : group.objects)
                    if (classGroups.contains(group))
                        groupValue.put(object, row.getKey().get(object));
                    else
                        groupValue.put(object, object.getObjectValue().getValue());

            Map<PropertyDrawInstance, Object> propertyValues = new HashMap<PropertyDrawInstance, Object>();
            for (PropertyDrawInstance property : propertyDraws)
                propertyValues.put(property, row.getValue().get(property));

            result.add(groupValue, propertyValues);
        }

        return result;
    }

    // pullProps чтобы запретить hint'ить
    public <P extends PropertyInterface, F extends PropertyInterface> Set<FilterEntity> getEditFixedFilters(ClassFormEntity<T> editForm, PropertyObjectInstance<P> changeProperty, GroupObjectInstance selectionGroupObject, Collection<PullChangeProperty> pullProps) {
        Set<FilterEntity> fixedFilters = new HashSet<FilterEntity>();

        PropertyValueImplement<P> implement = changeProperty.getValueImplement();

        for (MaxChangeProperty<?, P> constrainedProperty : implement.property.getMaxChangeProperties(BL.getCheckConstrainedProperties())) {
            pullProps.add(constrainedProperty);
            fixedFilters.add(new NotFilterEntity(new NotNullFilterEntity<MaxChangeProperty.Interface<P>>(
                            constrainedProperty.getPropertyObjectEntity(implement.mapping, editForm.object))));
        }

        ObjectEntity object = editForm.object;
        for (FilterEntity filterEntity : entity.fixedFilters) {
            FilterInstance filter = filterEntity.getInstance(instanceFactory);
            if (filter.getApplyObject() == selectionGroupObject) {
                for (ObjectEntity filterObject : filterEntity.getObjects()) {
                    //добавляем фильтр только, если есть хотя бы один объект который не будет заменён на константу
                    if (filterObject.baseClass == object.baseClass) {
                        fixedFilters.add(filterEntity.getRemappedFilter(filterObject, object, instanceFactory));
                        break;
                    }
                }
                for(PropertyValueImplement<?> filterImplement : filter.getResolveChangeProperties(implement.property)) {
                    OnChangeProperty<F, P> onChangeProperty = (OnChangeProperty<F, P>) filterImplement.property.getOnChangeProperty(implement.property);
                    pullProps.add(onChangeProperty);
                    fixedFilters.add(new NotNullFilterEntity<OnChangeProperty.Interface<F, P>>(
                                    onChangeProperty.getPropertyObjectEntity((Map<F,DataObject>) filterImplement.mapping, implement.mapping, editForm.object)));
                }
            }
        }
        return fixedFilters;
    }

    public DialogInstance<T> createClassPropertyDialog(int viewID, int value) throws RemoteException, SQLException {
        ClassFormEntity<T> classForm = getPropertyDraw(viewID).propertyObject.getDialogClass().getDialogForm(BL.LM);
        return new DialogInstance<T>(classForm.form, BL, session, securityPolicy, getFocusListener(), getClassListener(), classForm.object, value, instanceFactory.computer);
    }

    public Object read(PropertyObjectInstance<?> property) throws SQLException {
        return property.read(session, this);
    }

    public DialogInstance<T> createObjectEditorDialog(int viewID) throws RemoteException, SQLException {
        PropertyDrawInstance propertyDraw = getPropertyDraw(viewID);
        PropertyObjectInstance<?> changeProperty = propertyDraw.getChangeInstance(BL, this);

        CustomClass objectClass = changeProperty.getDialogClass();
        ClassFormEntity<T> classForm = objectClass.getEditForm(BL.LM);

        Object currentObject = read(changeProperty);
        if (currentObject == null && objectClass instanceof ConcreteCustomClass) {
            currentObject = addObject((ConcreteCustomClass)objectClass).object;
        }

        return currentObject == null
               ? null
               : new DialogInstance<T>(classForm.form, BL, session, securityPolicy, getFocusListener(), getClassListener(), classForm.object, currentObject, instanceFactory.computer);
    }

    public DialogInstance<T> createEditorPropertyDialog(int viewID) throws SQLException {
        PropertyDrawInstance propertyDraw = getPropertyDraw(viewID);

        Result<Property> aggProp = new Result<Property>();
        PropertyObjectInstance<?> changeProperty = propertyDraw.getChangeInstance(aggProp, BL, this);

        ClassFormEntity<T> formEntity = changeProperty.getDialogClass().getDialogForm(BL.LM);
        Set<PullChangeProperty> pullProps = new HashSet<PullChangeProperty>();
        Set<FilterEntity> additionalFilters = getEditFixedFilters(formEntity, changeProperty, propertyDraw.toDraw, pullProps);

        ObjectEntity dialogObject = formEntity.object;
        DialogInstance<T> dialog = new DialogInstance<T>(formEntity.form, BL, session, securityPolicy, getFocusListener(), getClassListener(), dialogObject, read(changeProperty), instanceFactory.computer, additionalFilters, pullProps);

        Property<PropertyInterface> filterProperty = aggProp.result;
        if (filterProperty != null) {
            PropertyDrawEntity filterPropertyDraw = formEntity.form.getPropertyDraw(filterProperty, dialogObject);
            if (filterPropertyDraw == null)
                filterPropertyDraw = formEntity.form.addPropertyDraw(filterProperty,
                        Collections.singletonMap(BaseUtils.single(filterProperty.interfaces), (PropertyObjectInterfaceEntity) dialogObject));
            dialog.initFilterPropertyDraw = filterPropertyDraw;
        }

        dialog.undecorated = BL.isDialogUndecorated();

        return dialog;
    }

    // ---------------------------------------- Events ----------------------------------------

    private class AutoActionsRunner {
        private final RemoteForm form;
        private Iterator<PropertyObjectEntity> autoActionsIt;
        private Iterator<ClientAction> actionsIt;

        public AutoActionsRunner(RemoteForm form, List<PropertyObjectEntity> autoActions) {
            this.form = form;
            autoActionsIt = autoActions.iterator();
            actionsIt = new EmptyIterator<ClientAction>();
        }

        private void prepareNext() throws SQLException {
            while (autoActionsIt.hasNext() && !actionsIt.hasNext()) {
                PropertyObjectEntity autoAction = autoActionsIt.next();
                PropertyObjectInstance action = instanceFactory.getInstance(autoAction);
                if (action.isInInterface(null)) {
                    List<ClientAction> change
                            = changeProperty(action,
                                             read(action) == null ? true : null,
                                             form, null);
                    actionsIt = change.iterator();
                }
            }
        }

        private boolean hasNext() throws SQLException {
            prepareNext();
            return actionsIt.hasNext();
        }

        private ClientAction next() throws SQLException {
            if (hasNext()) {
                return actionsIt.next();
            }
            return null;
        }

        public List<ClientAction> run() throws SQLException {
            List<ClientAction> actions = new ArrayList<ClientAction>();
            while (hasNext()) {
                ClientAction action = next();
                actions.add(action);
                if (action instanceof ContinueAutoActionsClientAction || action instanceof StopAutoActionsClientAction) {
                    break;
                }
            }

            return actions;
        }
    }

    private AutoActionsRunner autoActionsRunner;
    public List<ClientAction> continueAutoActions() throws SQLException {
        if (autoActionsRunner != null) {
            return autoActionsRunner.run();
        }

        return new ArrayList<ClientAction>();
    }

    private List<ClientAction> fireObjectChanged(ObjectInstance object, RemoteForm form) throws SQLException {
        return fireEvent(form, object.entity);
    }

    public List<ClientAction> fireOnApply(RemoteForm form) throws SQLException {
        return fireEvent(form, FormEventType.APPLY);
    }

    public List<ClientAction> fireOnOk(RemoteForm form) throws SQLException {
        return fireEvent(form, FormEventType.OK);
    }

    public List<ClientAction> fireOnClose(RemoteForm form) throws SQLException {
        return fireEvent(form, FormEventType.CLOSE);
    }

    public List<ClientAction> fireEvent(RemoteForm form, Object eventObject) throws SQLException {
        List<ClientAction> clientActions;
        List<PropertyObjectEntity> actionsOnEvent = entity.getActionsOnEvent(eventObject);
        if (actionsOnEvent != null) {
            autoActionsRunner = new AutoActionsRunner(form, actionsOnEvent);
            clientActions = autoActionsRunner.run();
        } else
            clientActions = new ArrayList<ClientAction>();

        for (FormEventListener listener : eventListeners)
            listener.handleEvent(eventObject);

        return clientActions;
    }

    private final WeakLinkedHashSet<FormEventListener> eventListeners = new WeakLinkedHashSet<FormEventListener>();
    public void addEventListener(FormEventListener listener) {
        eventListeners.add(listener);
    }

    public <P extends PropertyInterface> void fireChange(Property<P> property, PropertyChange<P> change) throws SQLException {
        entity.onChange(property, change, session, this);
    }
}
