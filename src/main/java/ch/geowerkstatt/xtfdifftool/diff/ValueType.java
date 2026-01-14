package ch.geowerkstatt.xtfdifftool.diff;

public enum ValueType {
    /** The complete object has been added or removed. */
    OBJECT,
    /** A reference to this object has been added or removed. */
    REFERENCE,
    /** An attribute value has been changed. */
    ATTRIBUTE,
    /** A geometry value has been changed. */
    GEOMETRY
}
