package ch.geowerkstatt.xtfdifftool.compare;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.interlis.ili2c.metamodel.AttributeDef;
import ch.interlis.ili2c.metamodel.CompositionType;
import ch.interlis.ili2c.metamodel.Extendable;
import ch.interlis.ili2c.metamodel.Table;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.iom.IomObject;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class StructAttributeComparer implements AttributeComparer {
    private static final StructAttributeComparer INSTANCE = new StructAttributeComparer();

    private StructAttributeComparer() {
    }

    /**
     * Get the {@link StructAttributeComparer} singleton instance.
     */
    public static StructAttributeComparer getInstance() {
        return INSTANCE;
    }

    @Override
    public Result compare(IomObject first, IomObject second, Type type, String attributePath) {
        if (!(type instanceof CompositionType compositionType)) {
            return Result.INCONCLUSIVE;
        }

        Table table = compositionType.getComponentType();
        String attributeName = AttributeComparer.getAttributeName(attributePath);
        IomObject firstValue = first.getattrobj(attributeName, 0);
        IomObject secondValue = second.getattrobj(attributeName, 0);
        if (firstValue == null && secondValue == null) {
            return Result.EQUAL;
        } else if (firstValue == null) {
            return Result.different(attributePath, null, table.getScopedName());
        } else if (secondValue == null) {
            return Result.different(attributePath, table.getScopedName(), null);
        }

        List<Change> changes = new ArrayList<>();
        for (Iterator<Extendable> it = table.getAttributes(); it.hasNext();) {
            if (it.next() instanceof AttributeDef attribute) {
                var result = AttributeComparer.compareAll(firstValue, secondValue, attribute.getDomainResolvingAll(), attributePath + "." + attribute.getName());
                if (result.equality() == Equality.DIFFERENT) {
                    changes.addAll(result.changes());
                }
            }
        }

        if (!changes.isEmpty()) {
            return Result.different(changes);
        }

        return Result.EQUAL;
    }
}
