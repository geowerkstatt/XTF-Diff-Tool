package ch.geowerkstatt.xtfdifftool.diff;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ValueType {
    /** The complete object has been added or removed. */
    @JsonProperty("object")
    OBJECT,

    /** A reference to this object has been added or removed. */
    @JsonProperty("reference")
    REFERENCE,

    /** An attribute value has been changed. */
    @JsonProperty("attribute")
    ATTRIBUTE,

    /** A geometry value has been changed. */
    @JsonProperty("geometry")
    GEOMETRY
}
