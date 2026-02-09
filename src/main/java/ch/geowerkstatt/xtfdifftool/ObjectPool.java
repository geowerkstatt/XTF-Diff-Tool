package ch.geowerkstatt.xtfdifftool;

import ch.interlis.ili2c.metamodel.*;
import ch.interlis.iom.IomObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Manages the INTERLIS objects from a Transfer
 */
public class ObjectPool {
    private static final Logger LOGGER = LogManager.getLogger();
    private final TransferDescription transferDescription;
    private final Map<String, Boolean> hasStableOidCache = new HashMap<>();

    private final Collection<AnalyzedObject> objects;
    private final Map<String, AnalyzedObject> objectsByStableOID;

    public ObjectPool(Stream<IomObject> objects, TransferDescription transferDescription) {
        this.transferDescription = transferDescription;
        this.objects = objects
                .map(o -> new AnalyzedObject(o, validateObjectHasStableOid(o)))
                .toList();
        this.objectsByStableOID = this.objects.stream()
                .filter(o -> o.hasStableOid)
                .collect(Collectors.toMap(o -> o.object.getobjectoid(), Function.identity(), (a, _) -> { throw new IllegalStateException("Duplicate TID encountered " + a.object.getobjectoid()); }, LinkedHashMap::new));

        var groups = this.objects.stream().collect(Collectors.groupingBy(o -> o.object.getobjecttag()));
        for (var entry : groups.entrySet()) {
            analyzeAssociations(entry.getKey(), entry.getValue());
        }
    }

    public IomObject getObject(String tid) {
        var analyzedObject = objectsByStableOID.get(tid);
        return analyzedObject == null ? null : analyzedObject.object;
    }

    public Stream<IomObject> objectsWithStableOid() {
        return objectsByStableOID.values().stream().map(o -> o.object);
    }

    public Stream<IomObject> objectsWithStableOidUnvisited() {
        return objectsByStableOID.values().stream().filter(o -> !o.visited).map(o -> o.object);
    }

    public void markVisited(IomObject object) {
        objectsByStableOID.get(object.getobjectoid()).visited = true;
    }

    public Map<String, List<String>> getAssociations(String tid) {
        var analyzedObject = objectsByStableOID.get(tid);
        return analyzedObject == null ? Collections.emptyMap() : analyzedObject.associations;
    }

    /**
     * Validates that the INTERLIS class of the given IomObject has a stable OID.
     * @param iomObject The IomObject to validate.
     * @return True if the class has a stable OID.
     */
    private boolean validateObjectHasStableOid(IomObject iomObject) {
        return hasClassStableOid(iomObject.getobjecttag());
    }

    private boolean validateRoleHasTargetWithStableOid(RoleDef role) {
        if (toStream(role.iteratorDestination()).anyMatch(a -> !hasClassStableOid(a.getScopedName()))) {
            LOGGER.warn("Target of role \"{}\" has no stable OID and is ignored in comparison.", role);
            return false;
        }

        return true;
    }

    private boolean hasClassStableOid(String className) {
        return hasStableOidCache.computeIfAbsent(className, this::calculateHasClassStableOid);
    }

    private boolean calculateHasClassStableOid(String className) {
        var classDef = getClassOrAssociationDef(className);
        if (classDef == null) {
            return false;
        }

        Domain oid = classDef.getOid();
        if (oid == null) {
            LOGGER.warn("Class or Association \"{}\" has no stable OID.", className);
            return false;
        }

        return true;
    }

    private void analyzeAssociations(String tag, List<AnalyzedObject> objects) {
        var classDef = getClassOrAssociationDef(tag);
        if (classDef == null && hasClassStableOid(tag)) {
            return;
        }

        // Analyze Associations and Roles
        var roleDefs = new ArrayList<RoleDef>();
        var embeddedRoleDefs = new HashMap<RoleDef, RoleDef>();
        for (var it = classDef.getAttributesAndRoles2(); it.hasNext(); ) {
            var viewableElement = it.next();
            if (viewableElement.obj instanceof RoleDef role && validateRoleHasTargetWithStableOid(role)) {
                if (viewableElement.embedded) {
                    var association = (AssociationDef)role.getContainer();
                    if (toStream(association.getAttributesAndRoles2()).anyMatch(a -> a.obj instanceof AttributeDef)) {
                        // Attributes of embedded association are not compared, because the association has no OID
                        LOGGER.warn("Embedded association \"{}\" has attributes that are not compared.", association.getScopedName());
                    }
                    embeddedRoleDefs.put(role, role.getOppEnd());
                } else {
                    roleDefs.add(role);
                }
            }
        }

        // Handle embedded roles
        for (var embeddedRoleDef : embeddedRoleDefs.entrySet()) {
            var role = embeddedRoleDef.getKey();
            var oppositeRole = embeddedRoleDef.getValue();

            for (var analyzedObject : objects) {
                if (analyzedObject.object.getattrvaluecount(role.getName()) > 0) {
                    var oppositeRef = analyzedObject.object.getattrobj(role.getName(), 0).getobjectrefoid();
                    var thisRef = analyzedObject.object.getobjectoid();
                    objectsByStableOID.get(thisRef).associations.computeIfAbsent(role.getName(), _ -> new ArrayList<>()).add(oppositeRef);
                    objectsByStableOID.get(oppositeRef).associations.computeIfAbsent(oppositeRole.getName(), _ -> new ArrayList<>()).add(thisRef);
                }
            }
        }

        // Handle roles of standalone association
        if (!roleDefs.isEmpty()) {
            for (var analyzedObject : objects) {
                // Gather referenced OIDs of each role
                var roles = new HashMap<String, List<String>>();
                for (var roleDef : roleDefs) {
                    var refs = roles.computeIfAbsent(roleDef.getName(), _ -> new ArrayList<>());
                    for (var i = 0; i < analyzedObject.object.getattrvaluecount(roleDef.getName()); i++) {
                        refs.add(analyzedObject.object.getattrobj(roleDef.getName(), i).getobjectrefoid());
                    }
                }

                // Add connections
                for (var entry : roles.entrySet()) {
                    for (var otherEntry : roles.entrySet()) {
                        if (entry != otherEntry) {
                            var roleB = otherEntry.getKey();
                            for (var refA : entry.getValue()) {
                                for (var refB : otherEntry.getValue()) {
                                    objectsByStableOID.get(refA).associations.computeIfAbsent(roleB, _ -> new ArrayList<>()).add(refB);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private AbstractClassDef<?> getClassOrAssociationDef(String className) {
        Element classElement = transferDescription.getElement(className);
        if (!(classElement instanceof AbstractClassDef<?> classDef)) {
            LOGGER.error("Class or Association \"{}\" not found.", className);
            return null;
        }

        return classDef;
    }

    private static final class AnalyzedObject {
        public final IomObject object;
        public final boolean hasStableOid;
        public boolean visited;
        public Map<String, List<String>> associations = new HashMap<>();

        AnalyzedObject(IomObject object, boolean hasStableOid) {
            this.object = object;
            this.hasStableOid = hasStableOid;
        }
    }

    private <T> Stream<T> toStream(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }
}
