package ch.geowerkstatt.xtfdifftool.diff;

import java.util.Objects;

/**
 * A change detected between two XTF files.
 * Each instance represents a single change of an object, its attribute, reference or geometry.
 * @param changeId Unique identifier of the change (ignored in equality checks)
 * @param oid Transfer ID of the object
 * @param changeType Type of change (CHANGED, ADDED, DELETED)
 * @param valueType Type of the value that changed
 * @param interlisName INTERLIS element name
 * @param oldValue Old attribute value
 * @param newValue New attribute value
 */
public record Change(
        String changeId,
        String oid,
        ChangeType changeType,
        ValueType valueType,
        String interlisName,
        String oldValue,
        String newValue
) {
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Change change)) {
            return false;
        }
        return Objects.equals(oid, change.oid)
                && changeType == change.changeType
                && valueType == change.valueType
                && Objects.equals(interlisName, change.interlisName)
                && Objects.equals(oldValue, change.oldValue)
                && Objects.equals(newValue, change.newValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oid, changeType, valueType, interlisName, oldValue, newValue);
    }
}
