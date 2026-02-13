package ch.geowerkstatt.xtfdifftool.value;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

public abstract class Value implements Comparable<Value> {
    private static final ObjectMapper OBJECT_MAPPER =  new ObjectMapper();
    protected String tag;

    protected Value(String tag) {
        this.tag = tag;
    }

    /**
     * Compares the {@link Value}s according to their tag.
     * Subclasses that override this method must call {@code super.compareTo()} to ensure a consistent ordering.
     */
    @Override
    public int compareTo(@NonNull Value o) {
        return tag.compareTo(o.tag);
    }

    /**
     * Computes the list of changes between this value and another value.
     *
     * @param o the other newer value to compare against
     * @return a list of changes representing the differences between this value and the other value
     */
    public List<Change> getChanges(Value o) {
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
    public ValueType getValueType() {
        return ValueType.ATTRIBUTE;
    }

    @Override
    public final String toString() {
        return OBJECT_MAPPER.writeValueAsString(this);
    }
}
