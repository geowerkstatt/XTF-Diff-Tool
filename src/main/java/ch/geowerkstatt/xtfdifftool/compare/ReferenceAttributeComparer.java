package ch.geowerkstatt.xtfdifftool.compare;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.interlis.ili2c.metamodel.ReferenceType;
import ch.interlis.ili2c.metamodel.RoleDef;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.ili2c.metamodel.Viewable;
import ch.interlis.iom.IomObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ReferenceAttributeComparer implements AttributeComparer {
    private static final ReferenceAttributeComparer INSTANCE = new ReferenceAttributeComparer();
    private static final Logger LOGGER = LogManager.getLogger();

    private ReferenceAttributeComparer() {
    }

    /**
     * Get the {@link ReferenceAttributeComparer} singleton instance.
     */
    public static ReferenceAttributeComparer getInstance() {
        return INSTANCE;
    }

    @Override
    public Result compare(IomObject first, IomObject second, Type type, String attributePath) {
        // Attributes of type "REFERENCE TO ..."
        if (!(type instanceof ReferenceType referenceType) || referenceType.getReferred() == null) {
            return Result.INCONCLUSIVE;
        }

        return compareReferences(first, second, referenceType.getReferred(), attributePath);
    }

    @Override
    public Result compareRole(IomObject first, IomObject second, RoleDef role, boolean embedded, String attributePath) {
        if (!embedded) {
            LOGGER.warn("Found embedded value for non-embedded role: {}", role.getName());
            return Result.INCONCLUSIVE;
        }

        if (!(role.getContainer() instanceof Viewable<?> viewable)) {
            LOGGER.warn("Role {} is not part of a class, struct or association", role.getName());
            return Result.INCONCLUSIVE;
        }

        return compareReferences(first, second, viewable, attributePath);
    }

    private Result compareReferences(IomObject first, IomObject second, Viewable<?> viewable, String attributePath) {
        String name = AttributeComparer.getAttributeName(attributePath);
        Map<String, IomObject> firstRefs = getRefMap(first, name);
        Map<String, IomObject> secondRefs = getRefMap(second, name);

        List<Change> changes = new ArrayList<>();
        for (var entry : firstRefs.entrySet()) {
            IomObject matchingEntry = secondRefs.get(entry.getKey());
            if (matchingEntry == null) {
                changes.add(new Change(attributePath, entry.getKey(), null));
            } else {
                String path = attributePath + "[" + entry.getKey() + "]";
                ObjectComparer.compareAllAttributes(viewable, entry.getValue(), matchingEntry, path, changes::add);
                secondRefs.remove(entry.getKey());
            }
        }

        for (var entry : secondRefs.entrySet()) {
            changes.add(new Change(attributePath, null, entry.getKey()));
        }

        return changes.isEmpty() ? Result.EQUAL : Result.different(changes);
    }

    private Map<String, IomObject> getRefMap(IomObject object, String roleName) {
        Map<String, IomObject> map = new HashMap<>();
        int count = object.getattrvaluecount(roleName);
        for (int i = 0; i < count; i++) {
            IomObject refObj = object.getattrobj(roleName, i);
            map.put(refObj.getobjectrefoid(), refObj);
        }
        return map;
    }
}
