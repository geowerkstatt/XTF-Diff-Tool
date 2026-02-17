package ch.geowerkstatt.xtfdifftool.value;

import ch.interlis.ili2c.metamodel.AbstractClassDef;
import ch.interlis.ili2c.metamodel.Element;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.iom.IomObject;

import java.util.Arrays;
import java.util.Optional;

public final class ValueFactory {
    private final CreateValue[] factories = {
            CollectionValue::createValue,
            ReferenceValue::createValue,
            ObjectValue::createValue,
            GeometryValue::createValue,
            PrimitiveValue::createValue,
    };

    private final TransferDescription transferDescription;

    /**
     * Constructs a factory using the provided transfer description to resolve elements.
     */
    public ValueFactory(TransferDescription transferDescription) {
        this.transferDescription = transferDescription;
    }

    /**
     * Creates an {@link ObjectValue} for the supplied {@link IomObject}.
     */
    public Optional<ObjectValue> createValue(IomObject obj) {
        Element element = transferDescription.getElement(obj.getobjecttag());
        if (!(element instanceof AbstractClassDef<?> classDef)) {
            return Optional.empty();
        }

        return Optional.of(ObjectValue.createValue(obj, classDef, this));
    }

    /**
     * Creates a {@link Value} from the specified attribute of the IomObject.
     *
     * @param obj           the object that contains the attribute
     * @param attributeName the attribute to resolve
     * @param index         the array index (may be {@code null})
     * @param type          the expected attribute type
     * @return the created {@link Value}
     */
    public Value createValue(IomObject obj, String attributeName, Integer index, Type type) {
        return Arrays.stream(factories)
                .map(f -> f.createValue(obj, attributeName, index, type, this))
                .flatMap(Optional::stream)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported type: " + type.resolveAliases().getClass().getName()));
    }

    @FunctionalInterface
    interface CreateValue {
        /**
         * Create a {@link Value} from an {@link IomObject} attribute.
         */
        Optional<? extends Value> createValue(IomObject obj, String attributeName, Integer index, Type type, ValueFactory factory);
    }

}
