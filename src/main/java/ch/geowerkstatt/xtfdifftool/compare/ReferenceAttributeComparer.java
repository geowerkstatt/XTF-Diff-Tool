package ch.geowerkstatt.xtfdifftool.compare;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.interlis.ili2c.metamodel.RoleDef;
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
    public Result compareRole(IomObject first, IomObject second, RoleDef role, boolean embedded, String attributePath) {
        if (!embedded) {
            LOGGER.warn("Found embedded value for non-embedded role: {}", role.getName());
            return Result.INCONCLUSIVE;
        }

        Map<String, IomObject> firstRefs = getRefMap(first, role.getName());
        Map<String, IomObject> secondRefs = getRefMap(second, role.getName());

        List<Change> changes = new ArrayList<>();
        for (var entry : firstRefs.entrySet()) {
            IomObject matchingEntry = secondRefs.get(entry.getKey());
            if (matchingEntry == null) {
                changes.add(new Change(attributePath, entry.getKey(), null));
            } else {
                if (role.getOppEnd().getContainer() instanceof Viewable<?> viewable) {
                    String path = attributePath + "[" + entry.getKey() + "]";
                    ObjectComparer.compareAllAttributes(viewable, entry.getValue(), matchingEntry, path, changes::add);
                }
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
