package ch.geowerkstatt.xtfdifftool.value;

import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.iom.IomObject;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jspecify.annotations.NonNull;
import tools.jackson.databind.ObjectMapper;

public final class PrimitiveValue implements Value {
    @JsonValue
    private final String value;

    PrimitiveValue(String value) {
        this.value = value;
    }

    /**
     * Create a {@link PrimitiveValue} from an {@link IomObject} attribute.
     * @see ValueFactory.CreateValue#createValue(IomObject, String, Integer, Type, ValueFactory) ValueFactory.CreateValue
     */
    public static PrimitiveValue createValue(IomObject obj, String attributeName, Integer index, Type type, ValueFactory factory) {
        var value = obj.getattrprim(attributeName, index == null ? 0 : index);
        if (value == null) {
            return null;
        }

        return new PrimitiveValue(value);
    }

    @Override
    public int compareTo(@NonNull Value o) {
        if (!(o instanceof PrimitiveValue otherPrimitive)) {
            throw new IllegalStateException("Cannot compare " + this.getClass().getSimpleName() + " with " + o.getClass().getSimpleName());
        }

        return value.compareTo(otherPrimitive.value);
    }

    @Override
    public ValueType getValueType() {
        return ValueType.ATTRIBUTE;
    }

    @Override
    public String toString() {
        return new ObjectMapper().writeValueAsString(this);
    }
}
