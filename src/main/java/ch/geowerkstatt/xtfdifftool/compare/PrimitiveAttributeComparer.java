package ch.geowerkstatt.xtfdifftool.compare;

import ch.interlis.ili2c.metamodel.NumericType;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.iom.IomObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public final class PrimitiveAttributeComparer implements AttributeComparer {
    private static final PrimitiveAttributeComparer INSTANCE = new PrimitiveAttributeComparer();
    private static final Logger LOGGER = LogManager.getLogger();

    private PrimitiveAttributeComparer() {
    }

    /**
     * Get the {@link PrimitiveAttributeComparer} singleton instance.
     */
    public static PrimitiveAttributeComparer getInstance() {
        return INSTANCE;
    }

    @Override
    public Result compare(IomObject first, IomObject second, Type type, String attributePath) {
        String attributeName = AttributeComparer.getAttributeName(attributePath);
        if (first.getattrobj(attributeName, 0) != null || second.getattrobj(attributeName, 0) != null) {
            // The attribute is not primitive
            return Result.INCONCLUSIVE;
        }

        var firstValues = getValues(attributeName, first, type);
        var secondValues = getValues(attributeName, second, type);

        if (firstValues.size() != secondValues.size()) {
            return Result.different(attributePath, joinValues(firstValues), joinValues(secondValues));
        }

        if (!type.isOrdered()) {
            firstValues.sort(null);
            secondValues.sort(null);
        }

        for (var i = 0; i < firstValues.size(); i++) {
            if (!firstValues.get(i).equals(secondValues.get(i))) {
                return Result.different(attributePath, joinValues(firstValues), joinValues(secondValues));
            }
        }

        return Result.EQUAL;
    }

    private List<String> getValues(String attributeName, IomObject obj, Type type) {
        var values = new ArrayList<String>();
        for (var i = 0; i < obj.getattrvaluecount(attributeName); i++) {
            var value = obj.getattrprim(attributeName, i);
            if (value != null && type instanceof NumericType numericType) {
                value = roundValue(value, numericType);
            }
            values.add(value);
        }

        return values;
    }

    private String joinValues(List<String> values) {
        return values.isEmpty() ? null : String.join(",", values);
    }

    private String roundValue(String value, NumericType numericType) {
        var minimum = numericType.getMinimum();
        if (minimum == null) {
            LOGGER.warn("Missing minimum value to round numeric type {}", numericType.getName());
            return value;
        }

        var numberValue = new BigDecimal(value);
        var roundedValue = roundNumber(numberValue, minimum.getAccuracy());
        return roundedValue.toPlainString();
    }

    private static BigDecimal roundNumber(BigDecimal value, int precision) {
        var isNegative = value.signum() == -1;
        return isNegative
                ? value.setScale(precision, RoundingMode.HALF_DOWN)
                : value.setScale(precision, RoundingMode.HALF_UP);
    }
}
