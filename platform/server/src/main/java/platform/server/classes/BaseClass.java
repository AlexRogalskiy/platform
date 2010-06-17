package platform.server.classes;

import platform.server.classes.sets.ConcreteCustomClassSet;
import platform.server.logics.table.ObjectTable;

public class BaseClass extends AbstractCustomClass {

    public ObjectTable table;

    public final UnknownClass unknown;

    public BaseClass(Integer ID, String caption) {
        super(ID, caption);
        table = new ObjectTable(this);
        unknown = new UnknownClass(this);
    }

    @Override
    public BaseClass getBaseClass() {
        return this;
    }

    public ObjectClass findClassID(Integer idClass) {
        if(idClass==null) return unknown;

        return findClassID((int)idClass);
    }

    public ConcreteClass findConcreteClassID(Integer idClass) {
        if(idClass==null) return unknown;

        return findConcreteClassID((int)idClass);
    }

    public ConcreteCustomClass getConcrete() {
        ConcreteCustomClassSet concrete = new ConcreteCustomClassSet();
        fillNextConcreteChilds(concrete);
        return concrete.get(0);
    }
}
