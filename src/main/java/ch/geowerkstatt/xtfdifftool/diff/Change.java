package ch.geowerkstatt.xtfdifftool.diff;

import ch.interlis.iom.IomObject;

/**
 * A change detected between two XTF files.
 * Each instance represents a single change of an object, its attribute, reference or geometry.
 * @param oid Transfer ID of the object
 * @param changeType Type of change (CHANGED, ADDED, DELETED)
 * @param valueType Type of the value that changed
 * @param interlisName Fully qualified INTERLIS element name
 * @param attributePath The attribute path of the changed attribute
 * @param oldValue Old attribute value
 * @param newValue New attribute value
 */
public record Change(
        String oid,
        ChangeType changeType,
        ValueType valueType,
        String interlisName,
        String attributePath,
        String oldValue,
        String newValue) {

    /**
     * Construct a new change from an attributePath, old and new value.
     */
    public Change(String attributePath, String oldValue, String newValue) {
        this(
                null,
                oldValue == null ? ChangeType.ADDED : newValue == null ? ChangeType.DELETED : ChangeType.CHANGED,
                ValueType.ATTRIBUTE,
                null,
                attributePath,
                oldValue,
                newValue);
    }

    /**
     * Get a new Change where the oid and tag properties are set according to the object.
     */
    public Change withObject(IomObject object) {
        return new Change(object.getobjectoid(), changeType, valueType, object.getobjecttag(), attributePath, oldValue, newValue);
    }
}
