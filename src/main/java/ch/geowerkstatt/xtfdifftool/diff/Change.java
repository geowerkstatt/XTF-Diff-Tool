package ch.geowerkstatt.xtfdifftool.diff;

/**
 * A change detected between two XTF files.
 * Each instance represents a single change of an object, its attribute, reference or geometry.
 * @param oid Transfer ID of the object
 * @param changeType Type of change (CHANGED, ADDED, DELETED)
 * @param valueType Type of the value that changed
 * @param interlisName INTERLIS element name
 * @param oldValue Old attribute value
 * @param newValue New attribute value
 */
public record Change(
        String oid,
        ChangeType changeType,
        ValueType valueType,
        String interlisName,
        String oldValue,
        String newValue
) {
}
