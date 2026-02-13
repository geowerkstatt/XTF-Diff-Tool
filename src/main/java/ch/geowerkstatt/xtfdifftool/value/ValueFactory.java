package ch.geowerkstatt.xtfdifftool.value;

import ch.interlis.ili2c.metamodel.AbstractClassDef;
import ch.interlis.ili2c.metamodel.Element;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.iom.IomObject;

public final class ValueFactory {
    private final CreateValue[] factories = {
            CollectionValue::createValue,
            ReferenceValue::createValue,
            ObjectValue::createValue,
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
    public ObjectValue createValue(IomObject obj) {
        Element element = transferDescription.getElement(obj.getobjecttag());
        if (!(element instanceof AbstractClassDef<?> classDef)) {
            return null;
        }

        return ObjectValue.createValue(obj, classDef, this);
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
        for (var factory : factories) {
            var result = factory.createValue(obj, attributeName, index, type, this);
            if (result != null) {
                return result;
            }
        }

        throw new IllegalArgumentException("Unsupported type: " + type.resolveAliases().getClass().getName());
    }

    @FunctionalInterface
    interface CreateValue {
        /**
         * Create a {@link Value} from an {@link IomObject} attribute.
         */
        Value createValue(IomObject obj, String attributeName, Integer index, Type type, ValueFactory factory);
    }

}
