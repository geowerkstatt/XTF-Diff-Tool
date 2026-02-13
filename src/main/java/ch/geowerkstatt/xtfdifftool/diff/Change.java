package ch.geowerkstatt.xtfdifftool.diff;

import ch.geowerkstatt.xtfdifftool.value.ObjectValue;
import ch.geowerkstatt.xtfdifftool.value.Value;

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
        Value oldValue,
        Value newValue) {

    /**
     * Construct a new change for an attribute from an attributePath, old and new value.
     */
    public static Change attribute(String attributePath, Value oldValue, Value newValue) {
        var changeType = oldValue == null ? ChangeType.ADDED : newValue == null ? ChangeType.DELETED : ChangeType.CHANGED;
        var valueType = oldValue == null ? newValue.getValueType() : oldValue.getValueType();

        return new Change(null, changeType, valueType, null, attributePath, oldValue, newValue);
    }

    /**
     * Get a new Change where the oid and tag properties are set according to the object.
     */
    public Change withObject(ObjectValue object) {
        return new Change(object.getOid(), changeType, valueType, object.getTag(), attributePath, oldValue, newValue);
    }

    /**
     * Get a new Change where the attribute path is prefixed with the given base attribute name.
     */
    public Change withBaseAttribute(String attributeName) {
        var combinedPath = attributePath.isEmpty() ? attributeName : attributeName + "." + attributePath;
        return new Change(oid, changeType, valueType, interlisName, combinedPath, oldValue, newValue);
    }
}
