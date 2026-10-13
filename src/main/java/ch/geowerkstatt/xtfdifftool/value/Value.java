package ch.geowerkstatt.xtfdifftool.value;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;

import java.util.List;
import java.util.stream.Stream;

/**
 * A {@link Value} is a piece of data from a transfer that can be compared to other {@link Value}s.
 */
public interface Value extends Comparable<Value> {
    /**
     * Computes the list of changes between this value and another value.
     *
     * @param o the other newer value to compare against
     * @return a list of changes representing the differences between this value and the other value
     */
    default List<Change> getChanges(Value o) {
        var comparison = this.compareTo(o);
        if (comparison == 0) {
            return List.of();
        } else {
            return List.of(Change.attribute("", this, o));
        }
    }

    /**
     * Returns the type of this value.
     *
     * @return the value type, defaults to {@link ValueType#ATTRIBUTE}
     */
    ValueType getValueType();

    /**
     * Returns the locations of the geometries this value holds, as WKT. Empty unless the {@link ValueFactory} computed
     * locations, and for a structure, whose geometries do not locate the object that contains it.
     *
     * @return the locations of the geometries in this value
     */
    default Stream<String> getLocations() {
        return Stream.empty();
    }
}
