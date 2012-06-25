package platform.gwt.view.changes;

import platform.gwt.view.*;
import platform.gwt.view.changes.dto.GFormChangesDTO;
import platform.gwt.view.changes.dto.GGroupObjectValueDTO;
import platform.gwt.view.changes.dto.GPropertyReaderDTO;
import platform.gwt.view.changes.dto.ObjectDTO;
import platform.gwt.view.reader.GPropertyReader;

import java.io.Serializable;
import java.util.*;

public class GFormChanges implements Serializable {
    public final HashMap<GGroupObject, String> classViews = new HashMap<GGroupObject, String>(); 
    public final HashMap<GGroupObject, GGroupObjectValue> objects = new HashMap<GGroupObject, GGroupObjectValue>();
    public final HashMap<GGroupObject, ArrayList<GGroupObjectValue>> gridObjects = new HashMap<GGroupObject, ArrayList<GGroupObjectValue>>();
    public final Map<GGroupObject, ArrayList<GGroupObjectValue>> parentObjects = new HashMap<GGroupObject, ArrayList<GGroupObjectValue>>();
    public final HashMap<GPropertyReader, HashMap<GGroupObjectValue, Object>> properties = new HashMap<GPropertyReader, HashMap<GGroupObjectValue, Object>>();
    public final HashSet<GPropertyReader> panelProperties = new HashSet<GPropertyReader>();
    public final HashSet<GPropertyDraw> dropProperties = new HashSet<GPropertyDraw>();

    public static GFormChanges remap(GForm form, GFormChangesDTO dto) {
        GFormChanges remapped = new GFormChanges();
        
        for (Map.Entry<Integer, ObjectDTO> e : dto.classViews.entrySet()) {
            remapped.classViews.put(form.getGroupObject(e.getKey()), (String) e.getValue().getValue());
        }
        
        for (Map.Entry<Integer, ArrayList<GGroupObjectValueDTO>> e : dto.gridObjects.entrySet()) {
            remapped.gridObjects.put(form.getGroupObject(e.getKey()), remapGroupObjectValues(form, e.getValue()));
        }

        for (Map.Entry<Integer, ArrayList<GGroupObjectValueDTO>> e : dto.parentObjects.entrySet()) {
            remapped.parentObjects.put(form.getGroupObject(e.getKey()), remapGroupObjectValues(form, e.getValue()));
        }

        for (Map.Entry<GPropertyReaderDTO, HashMap<GGroupObjectValueDTO, ObjectDTO>> e : dto.properties.entrySet()) {
            remapped.properties.put(defineReader(form, e.getKey()), remapGroupObjectValueMap(form, e.getValue()));
        }

        for (GPropertyReaderDTO property : dto.panelProperties) {
            remapped.panelProperties.add(defineReader(form, property));
        }

        for (Integer propertyID : dto.dropProperties) {
            remapped.dropProperties.add(form.getProperty(propertyID));
        }

        for (Map.Entry<Integer, GGroupObjectValueDTO> e : dto.objects.entrySet()) {
            remapped.objects.put(form.getGroupObject(e.getKey()), remapGroupObjectValue(form, e.getValue()));
        }

        return remapped;
    }

    private static HashMap<GGroupObjectValue, Object> remapGroupObjectValueMap(GForm form, HashMap<GGroupObjectValueDTO, ObjectDTO> values) {
        HashMap<GGroupObjectValue, Object> res = new HashMap<GGroupObjectValue, Object>();
        for (Map.Entry<GGroupObjectValueDTO, ObjectDTO> e : values.entrySet()) {
            res.put(remapGroupObjectValue(form, e.getKey()), e.getValue().getValue());
        }
        return res;
    }

    private static ArrayList<GGroupObjectValue> remapGroupObjectValues(GForm form, ArrayList<GGroupObjectValueDTO> values) {
        ArrayList<GGroupObjectValue> res = new ArrayList<GGroupObjectValue>();
        for (GGroupObjectValueDTO key : values) {
            res.add(remapGroupObjectValue(form, key));
        }
        return res;
    }

    private static GGroupObjectValue remapGroupObjectValue(GForm form, GGroupObjectValueDTO key) {
        GGroupObjectValue remapped = new GGroupObjectValue();
        for (Map.Entry<Integer, ObjectDTO> e : key.entrySet()) {
            remapped.put(form.getObject(e.getKey()), e.getValue().getValue());
        }
        return remapped;
    }

    private static GPropertyReader defineReader(GForm form, GPropertyReaderDTO readerDTO) {
        switch (readerDTO.type) {
            case GPropertyReadType.DRAW:
                return form.getProperty(readerDTO.readerID);
            case GPropertyReadType.CAPTION:
                return form.getProperty(readerDTO.readerID).captionReader;
            case GPropertyReadType.CELL_BACKGROUND:
                return form.getProperty(readerDTO.readerID).backgroundReader;
            case GPropertyReadType.CELL_FOREGROUND:
                return form.getProperty(readerDTO.readerID).foregroundReader;
            case GPropertyReadType.FOOTER:
                return form.getProperty(readerDTO.readerID).footerReader;
            case GPropertyReadType.ROW_BACKGROUND:
                return form.getGroupObject(readerDTO.readerID).rowBackgroundReader;
            case GPropertyReadType.ROW_FOREGROUND:
                return form.getGroupObject(readerDTO.readerID).rowForegroundReader;
            default:
                return null;
        }
    }

    public class GPropertyReadType {
        public final static byte DRAW = 0;
        public final static byte CAPTION = 1;
        public final static byte CELL_BACKGROUND = 2;
        public final static byte ROW_BACKGROUND = 3;
        public final static byte FOOTER = 4;
        public final static byte CELL_FOREGROUND = 5;
        public final static byte ROW_FOREGROUND = 6;
    }
}
