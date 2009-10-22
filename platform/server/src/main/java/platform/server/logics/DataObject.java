package platform.server.logics;

import platform.base.BaseUtils;
import platform.interop.Compare;
import platform.server.data.classes.ConcreteClass;
import platform.server.data.classes.LogicalClass;
import platform.server.data.classes.where.AndClassSet;
import platform.server.data.query.exprs.SourceExpr;
import platform.server.data.query.exprs.ValueExpr;
import platform.server.data.sql.SQLSyntax;
import platform.server.data.types.Type;
import platform.server.where.Where;
import platform.server.view.form.PropertyObjectInterface;
import platform.server.view.form.GroupObjectImplement;
import platform.server.view.navigator.PropertyInterfaceNavigator;
import platform.server.view.navigator.Mapper;
import platform.server.view.navigator.ObjectNavigator;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class DataObject extends ObjectValue implements PropertyObjectInterface, PropertyInterfaceNavigator {

    public Object object;
    public ConcreteClass objectClass;

    @Override
    public String toString() {
        return object + " - " + objectClass;
    }

    public boolean equals(Object o) {
        return this==o || o instanceof DataObject && object.equals(((DataObject)o).object) && objectClass.equals(((DataObject)o).objectClass);
    }

    public int hashCode() {
        return object.hashCode()*31+objectClass.hashCode();
    }

    public DataObject(Object iObject, ConcreteClass iClass) {
        object = iObject;

        assert BaseUtils.isData(object);
        assert !(objectClass instanceof LogicalClass && !object.equals(true));

        objectClass = iClass;
    }

    public boolean isString(SQLSyntax syntax) {
        return objectClass.getType().isSafeString(object);
    }
    public String getString(SQLSyntax syntax) {
        return objectClass.getType().getString(object, syntax);
    }

    public ValueExpr getExpr() {
        return new ValueExpr(object,objectClass);
    }

    public Object getValue() {
        return object;
    }

    public static <K> Map<K,Object> getMapValues(Map<K,DataObject> map) {
        Map<K,Object> mapClasses = new HashMap<K,Object>();
        for(Map.Entry<K,DataObject> keyField : map.entrySet())
            mapClasses.put(keyField.getKey(), keyField.getValue().object);
        return mapClasses;
    }

    public static <K> Map<K,ConcreteClass> getMapClasses(Map<K,DataObject> map) {
        Map<K,ConcreteClass> mapClasses = new HashMap<K,ConcreteClass>();
        for(Map.Entry<K,DataObject> keyField : map.entrySet())
            mapClasses.put(keyField.getKey(), keyField.getValue().objectClass);
        return mapClasses;        
    }

    public Where order(SourceExpr expr, boolean desc, Where orderWhere) {
        Where greater = expr.compare(this,Compare.GREATER);
        return (desc?greater.not():greater).or(expr.compare(this,Compare.EQUALS).and(orderWhere));
    }

    public AndClassSet getClassSet(GroupObjectImplement classGroup) {
        return objectClass;
    }

    public DataObject getDataObject() {
        return this;
    }

    public GroupObjectImplement getApplyObject() {
        return null;
    }

    public Type getType() {
        return objectClass.getType();
    }

    public PropertyObjectInterface doMapping(Mapper mapper) {
        return this;
    }

    public void fillObjects(Set<ObjectNavigator> objects) {
    }
    
}
