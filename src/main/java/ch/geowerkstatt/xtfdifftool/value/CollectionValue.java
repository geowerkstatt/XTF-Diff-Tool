package ch.geowerkstatt.xtfdifftool.value;

import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.iom.IomObject;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class CollectionValue extends Value {
    @JsonValue
    private final List<Value> values;
    private final boolean isOrdered;

    CollectionValue(List<Value> values, boolean isOrdered) {
        super("BAG/LIST");
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
    public static CollectionValue createValue(IomObject obj, String attributeName, Integer index, Type type, ValueFactory factory) {
        if (type.getCardinality().getMaximum() == 1 || index != null) {
            return null;
        }

        var baseType = type.resolveAliases();
        var values = new ArrayList<Value>();
        for (var i = 0; i < obj.getattrvaluecount(attributeName); i++) {
            values.add(factory.createValue(obj, attributeName, i, baseType));
        }

        return new CollectionValue(values, type.isOrdered());
    }

    @Override
    public int compareTo(@NonNull Value o) {
        var superComparison = super.compareTo(o);
        if (superComparison != 0) {
            return superComparison;
        }

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
}
