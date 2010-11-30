package platform.client.logics;

import platform.base.context.ContextIdentityObject;
import platform.client.descriptor.editor.ComponentEditor;
import platform.base.context.ApplicationContext;
import platform.client.descriptor.nodes.ComponentNode;
import platform.client.serialization.ClientIdentitySerializable;
import platform.client.serialization.ClientSerializationPool;
import platform.interop.ComponentDesign;
import platform.interop.form.layout.AbstractComponent;
import platform.interop.form.layout.DoNotIntersectSimplexConstraint;
import platform.interop.form.layout.SimplexConstraints;

import javax.swing.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class ClientComponent extends ContextIdentityObject implements Serializable, ClientIdentitySerializable, AbstractComponent<ClientContainer, ClientComponent> {

    public ComponentDesign design;

    public ClientContainer container;
    public SimplexConstraints<ClientComponent> constraints;

    public boolean defaultComponent = false;

    public ClientComponent() {
    }

    public ClientComponent(ApplicationContext context) {
        super(context);
        initAggregateObjects(context);
    }

    public ClientComponent(int ID, ApplicationContext context) {
        super(ID, context);
        initAggregateObjects(context);
    }

    protected void initAggregateObjects(ApplicationContext context) {
        constraints = new SimplexConstraints<ClientComponent>(context);
        design = new ComponentDesign(context);
    }

    public void customSerialize(ClientSerializationPool pool, DataOutputStream outStream, String serializationType) throws IOException {
        pool.writeObject(outStream, design);
        pool.serializeObject(outStream, container);
        pool.writeObject(outStream, constraints);

        outStream.writeInt(constraints.intersects.size());
        for (Map.Entry<ClientComponent, DoNotIntersectSimplexConstraint> intersect : constraints.intersects.entrySet()) {
            pool.serializeObject(outStream, intersect.getKey());
            pool.writeObject(outStream, intersect.getValue());
        }

        outStream.writeBoolean(defaultComponent);
    }

    public void customDeserialize(ClientSerializationPool pool, DataInputStream inStream) throws IOException {
        design = pool.readObject(inStream);

        container = pool.deserializeObject(inStream);

        constraints = pool.readObject(inStream);

        constraints.intersects = new HashMap<ClientComponent, DoNotIntersectSimplexConstraint>();
        int count = inStream.readInt();
        for (int i = 0; i < count; i++) {
            ClientComponent view = pool.deserializeObject(inStream);
            DoNotIntersectSimplexConstraint constraint = pool.readObject(inStream);
            constraints.intersects.put(view, constraint);
        }

        defaultComponent = inStream.readBoolean();
    }

    public ComponentNode getNode() {
        return new ComponentNode(this);
    }

    public JComponent getPropertiesEditor() {
        return new ComponentEditor(this);
    }

    public SimplexConstraints<ClientComponent> getConstraints() {
        return constraints;
    }

    public void setConstraints(SimplexConstraints<ClientComponent> constraints) {
        this.constraints = constraints;
        updateDependency(this, "constraints");
    }
  
    public void setDefaultComponent(boolean defaultComponent) {
        this.defaultComponent = defaultComponent;
        updateDependency(this, "defaultComponent");
    }

    public boolean getDefaultComponent() {
        return defaultComponent;
    }

    public Map<ClientComponent, DoNotIntersectSimplexConstraint> getIntersects() {
        return constraints.intersects;
    }

    public void setIntersects(Map<ClientComponent, DoNotIntersectSimplexConstraint> intersects) {
        constraints.intersects = intersects;
        updateDependency(this.constraints, "intersects");
    }

    public abstract String getCaption();
}
