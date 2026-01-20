package ch.geowerkstatt.xtfdifftool.diff;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ChangeType {
    /** The attribute of the object has been modified. */
    @JsonProperty("changed")
    CHANGED,

    /** The object has been added. */
    @JsonProperty("added")
    ADDED,

    /** The object has been deleted. */
    @JsonProperty("deleted")
    DELETED
}
