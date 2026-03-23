package ch.geowerkstatt.xtfdifftool.value;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.iom.IomObject;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

public final class CollectionValue implements Value {
    @JsonValue
    private final List<Value> values;
    private final boolean isOrdered;

    CollectionValue(List<Value> values, boolean isOrdered) {
        this.values = values;
        this.isOrdered = isOrdered;
        if (!isOrdered) {
            Collections.sort(this.values);
        }
    }

    CollectionValue() {
        this(new ArrayList<>(), false);
    }

    /**
     * Add the value to this {@link CollectionValue}. If the collection is ordered, the value is appended at the end.
     */
    public void addValue(Value value) {
        values.add(value);
        if (!isOrdered) {
            Collections.sort(values);
        }
    }

    /**
     * Create a {@link CollectionValue} from an {@link IomObject} attribute.
     * @see ValueFactory.CreateValue#createValue(IomObject, String, Integer, Type, ValueFactory) ValueFactory.CreateValue
     */
    public static Optional<CollectionValue> createValue(IomObject obj, String attributeName, Integer index, Type type, ValueFactory factory) {
        if (type.getCardinality().getMaximum() == 1 || index != null) {
            return Optional.empty();
        }

        var baseType = type.resolveAliases();
        var values = new ArrayList<Value>();
        for (var i = 0; i < obj.getattrvaluecount(attributeName); i++) {
            values.add(factory.createValue(obj, attributeName, i, baseType));
        }

        return Optional.of(new CollectionValue(values, type.isOrdered()));
    }

    @Override
    public int compareTo(@NonNull Value o) {
        if (!(o instanceof CollectionValue otherCollection)) {
            throw new IllegalStateException("Cannot compare " + this.getClass().getSimpleName() + " with " + o.getClass().getSimpleName());
        }

        var sizeComparison = Integer.compare(values.size(), otherCollection.values.size());
        if (sizeComparison != 0) {
            return sizeComparison;
        }

        for (var i = 0; i < values.size(); i++) {
            var valueComparison = values.get(i).compareTo(otherCollection.values.get(i));
            if (valueComparison != 0) {
                return valueComparison;
            }
        }

        return 0;
    }

    @Override
    public ValueType getValueType() {
        // Assume all values have the same ValueType
        return values.isEmpty() ? ValueType.ATTRIBUTE : values.getFirst().getValueType();
    }

    @Override
    public List<Change> getChanges(Value o) {
        if (!(o instanceof CollectionValue otherCollection)
                || this.isOrdered
                || otherCollection.isOrdered
                || this.getValueType() != ValueType.REFERENCE
                || otherCollection.getValueType() != ValueType.REFERENCE) {
            return Value.super.getChanges(o);
        }

        var added = new ArrayList<Value>();
        var deleted = new ArrayList<Value>();
        var hasEqualElements = false;
        var iOld = 0;
        var iNew = 0;

        while (iOld < this.values.size() && iNew < otherCollection.values.size()) {
            var comparison = this.values.get(iOld).compareTo(otherCollection.values.get(iNew));
            if (comparison == 0) {
                hasEqualElements = true;
                iOld++;
                iNew++;
            } else if (comparison < 0) {
                deleted.add(this.values.get(iOld++));
            } else {
                added.add(otherCollection.values.get(iNew++));
            }
        }

        while (iOld < this.values.size()) {
            deleted.add(this.values.get(iOld++));
        }

        while (iNew < otherCollection.values.size()) {
            added.add(otherCollection.values.get(iNew++));
        }

        var changes = new ArrayList<Change>();
        if (!hasEqualElements) {
            changes.add(Change.attribute("", this, otherCollection));
        } else {
            if (!deleted.isEmpty()) {
                changes.add(Change.attribute("", new CollectionValue(deleted, false), null));
            }
            if (!added.isEmpty()) {
                changes.add(Change.attribute("", null, new CollectionValue(added, false)));
            }
        }
        return changes;
    }

    @Override
    public String toString() {
        return new ObjectMapper().writeValueAsString(this);
    }
}
