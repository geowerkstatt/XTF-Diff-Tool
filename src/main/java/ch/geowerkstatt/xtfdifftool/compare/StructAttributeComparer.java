package ch.geowerkstatt.xtfdifftool.compare;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.interlis.ili2c.metamodel.CompositionType;
import ch.interlis.ili2c.metamodel.Table;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.iom.IomObject;

import java.util.ArrayList;
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

        List<Change> changes = new ArrayList<>();
        String attributeName = AttributeComparer.getAttributeName(attributePath);
        boolean isCollection = compositionType.getCardinality().getMaximum() > 1;
        int firstCount = first.getattrvaluecount(attributeName);
        int secondCount = second.getattrvaluecount(attributeName);
        int maxCount = Math.max(firstCount, secondCount);

        for (int i = 0; i < maxCount; i++) {
            IomObject firstValue = i < firstCount ? first.getattrobj(attributeName, i) : null;
            IomObject secondValue = i < secondCount ? second.getattrobj(attributeName, i) : null;
            String structPath = isCollection ? attributePath + "[" + i + "]" : attributePath;
            compareStructs(firstValue, secondValue, compositionType.getComponentType(), structPath, changes);
        }

        return changes.isEmpty() ? Result.EQUAL : Result.different(changes);
    }

    private void compareStructs(IomObject first, IomObject second, Table table, String attributePath, List<Change> changes) {
        if (first == null && second == null) {
            return;
        } else if (first == null) {
            changes.add(new Change(attributePath, null, table.getScopedName()));
            return;
        } else if (second == null) {
            changes.add(new Change(attributePath, table.getScopedName(), null));
            return;
        }

        ObjectComparer.compareAllAttributes(table, first, second, attributePath, changes::add);
    }
}
