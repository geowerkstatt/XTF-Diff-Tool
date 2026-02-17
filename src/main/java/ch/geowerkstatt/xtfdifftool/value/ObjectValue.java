package ch.geowerkstatt.xtfdifftool.value;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.interlis.ili2c.metamodel.AbstractClassDef;
import ch.interlis.ili2c.metamodel.AttributeDef;
import ch.interlis.ili2c.metamodel.CompositionType;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.iom.IomObject;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.ObjectMapper;

import java.util.*;

public final class ObjectValue implements Value {
    @JsonValue
    private final Map<String, Value> values;
    private final Map<String, CollectionValue> associations;
    private final String oid;
    private final String tag;

    ObjectValue(String tag, String oid, Map<String, Value> values) {
        this.tag = tag;
        this.oid = oid;
        this.values = values;
        associations = new LinkedHashMap<>();
    }

    /**
     * Create a {@link ObjectValue} that represents the given {@link IomObject}.
     */
    public static ObjectValue createValue(IomObject obj, AbstractClassDef<?> classDef, ValueFactory factory) {
        var values = new LinkedHashMap<String, Value>();
        for (var it = classDef.getAttributes(); it.hasNext();) {
            var attributeDef = (AttributeDef) it.next();
            var attributeName = attributeDef.getName();
            if (obj.getattrvaluecount(attributeName) > 0) {
                var value = factory.createValue(obj, attributeName, null, attributeDef.getDomain());
                values.put(attributeName, value);
            }
        }

        return new ObjectValue(obj.getobjecttag(), obj.getobjectoid(), values);
    }

    /**
     * Create a {@link ObjectValue} from an {@link IomObject} attribute.
     * @see ValueFactory.CreateValue#createValue(IomObject, String, Integer, Type, ValueFactory) ValueFactory.CreateValue
     */
    public static Value createValue(IomObject obj, String attributeName, Integer index, Type type, ValueFactory factory) {
        if (!(type.resolveAliases() instanceof CompositionType)) {
            return null;
        }

        var value = obj.getattrobj(attributeName, index == null ? 0 : index);
        return factory.createValue(value);
    }

    /**
     * Add an association reference to this {@link ObjectValue}.
     */
    public void addReference(String roleName, String refOid) {
        var role = associations.computeIfAbsent(roleName, _ -> new CollectionValue());
        role.addValue(new ReferenceValue(refOid));
    }

    public String getOid() {
        return oid;
    }

    public String getTag() {
        return tag;
    }

    public Map<String, Value> getValues() {
        return values;
    }

    public Map<String, CollectionValue> getAssociations() {
        return associations;
    }

    @Override
    public int compareTo(@NonNull Value o) {
        if (!(o instanceof ObjectValue otherObject)) {
            throw new IllegalStateException("Cannot compare " + this.getClass().getSimpleName() + " with " + o.getClass().getSimpleName());
        }

        var tagComparison = this.tag.compareTo(otherObject.tag);
        if (tagComparison != 0) {
            return tagComparison;
        }

        for (var key : getCombinedKeys(values.keySet(), otherObject.values.keySet())) {
            var thisValue = values.get(key);
            var otherValue = otherObject.values.get(key);
            if (thisValue == null && otherValue == null) {
                continue;
            } else if (thisValue == null) {
                return -1;
            } else if (otherValue == null) {
                return 1;
            }

            var valueComparison = thisValue.compareTo(otherValue);
            if (valueComparison != 0) {
                return valueComparison;
            }
        }

        return 0;
    }

    @Override
    public List<Change> getChanges(Value o) {
        if (!(o instanceof ObjectValue otherObject)) {
            throw new IllegalStateException("Cannot get changes from " + this.getClass().getSimpleName() + " with " + o.getClass().getSimpleName());
        }

        var changes = new ArrayList<>(getChanges(values, otherObject.values));
        changes.addAll(getChanges(associations, otherObject.associations));

        return changes;
    }

    @Override
    public ValueType getValueType() {
        return ValueType.ATTRIBUTE;
    }

    private List<Change> getChanges(Map<String, ? extends Value> first, Map<String, ? extends Value> second) {
        var changes = new ArrayList<Change>();
        for (var key : getCombinedKeys(first.keySet(), second.keySet())) {
            var firstValue = first.get(key);
            var secondValue = second.get(key);
            if (firstValue != null && secondValue != null) {
                changes.addAll(firstValue.getChanges(secondValue).stream().map(o -> o.withBaseAttribute(key)).toList());
            } else if (firstValue != null || secondValue != null) {
                changes.add(Change.attribute(key, firstValue, secondValue));
            }
        }

        return changes;
    }

    private Set<String> getCombinedKeys(Set<String> firstKeys, Set<String> secondKeys) {
        var combined = new TreeSet<>(firstKeys);
        combined.addAll(secondKeys);
        return combined;
    }

    @Override
    public String toString() {
        return new ObjectMapper().writeValueAsString(this);
    }
}
