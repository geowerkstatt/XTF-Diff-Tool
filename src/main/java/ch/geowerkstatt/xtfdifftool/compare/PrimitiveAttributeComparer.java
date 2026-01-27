package ch.geowerkstatt.xtfdifftool.compare;

import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.iom.IomObject;

import java.util.ArrayList;
import java.util.List;

public final class PrimitiveAttributeComparer implements AttributeComparer {
    private static final PrimitiveAttributeComparer INSTANCE = new PrimitiveAttributeComparer();

    private PrimitiveAttributeComparer() {
    }

    /**
     * Get the {@link PrimitiveAttributeComparer} singleton instance.
     */
    public static PrimitiveAttributeComparer getInstance() {
        return INSTANCE;
    }

    @Override
    public Result compare(IomObject first, IomObject second, Type type, String attributeName) {
        if (first.getattrobj(attributeName, 0) != null || second.getattrobj(attributeName, 0) != null) {
            // The attribute is not primitive
            return Result.INCONCLUSIVE;
        }

        var firstValues = getValues(attributeName, first);
        var secondValues = getValues(attributeName, second);

        if (firstValues.size() != secondValues.size()) {
            return Result.different(attributeName, joinValues(firstValues), joinValues(secondValues));
        }

        if (!type.isOrdered()) {
            firstValues.sort(null);
            secondValues.sort(null);
        }

        for (var i = 0; i < firstValues.size(); i++) {
            if (!firstValues.get(i).equals(secondValues.get(i))) {
                return Result.different(attributeName, joinValues(firstValues), joinValues(secondValues));
            }
        }

        return Result.EQUAL;
    }

    private List<String> getValues(String attributeName, IomObject obj) {
        var values = new ArrayList<String>();
        for (var i = 0; i < obj.getattrvaluecount(attributeName); i++) {
            values.add(obj.getattrprim(attributeName, i));
        }

        return values;
    }

    private String joinValues(List<String> values) {
        return values.isEmpty() ? null : String.join(",", values);
    }
}
