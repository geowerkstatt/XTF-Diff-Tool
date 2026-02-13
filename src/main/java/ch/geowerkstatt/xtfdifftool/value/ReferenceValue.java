package ch.geowerkstatt.xtfdifftool.value;

import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.interlis.ili2c.metamodel.ReferenceType;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.iom.IomObject;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jspecify.annotations.NonNull;

public final class ReferenceValue extends Value {
    @JsonValue
    private final String value;

    ReferenceValue(String value) {
        super("REF");
        this.value = value;
    }

    /**
     * Create a {@link ReferenceValue} from an {@link IomObject} attribute.
     * @see ValueFactory.CreateValue#createValue(IomObject, String, Integer, Type, ValueFactory) ValueFactory.CreateValue
     */
    public static ReferenceValue createValue(IomObject obj, String attributeName, Integer index, Type type, ValueFactory factory) {
        if (!(type.resolveAliases() instanceof ReferenceType)) {
            return null;
        }

        var refObject = obj.getattrobj(attributeName, index == null ? 0 : index);
        return new ReferenceValue(refObject.getobjectrefoid());
    }

    @Override
    public int compareTo(@NonNull Value o) {
        var superComparison = super.compareTo(o);
        if (superComparison != 0) {
            return superComparison;
        }

        if (!(o instanceof ReferenceValue otherReference)) {
            throw new IllegalStateException("Cannot compare " + this.getClass().getSimpleName() + " with " + o.getClass().getSimpleName());
        }

        return value.compareTo(otherReference.value);
    }

    @Override
    public ValueType getValueType() {
        return ValueType.REFERENCE;
    }
}
