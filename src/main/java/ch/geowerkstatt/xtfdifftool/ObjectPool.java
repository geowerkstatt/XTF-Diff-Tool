package ch.geowerkstatt.xtfdifftool;

import ch.geowerkstatt.xtfdifftool.value.ObjectValue;
import ch.geowerkstatt.xtfdifftool.value.ValueFactory;
import ch.interlis.ili2c.metamodel.AbstractClassDef;
import ch.interlis.ili2c.metamodel.AssociationDef;
import ch.interlis.ili2c.metamodel.AttributeDef;
import ch.interlis.ili2c.metamodel.Domain;
import ch.interlis.ili2c.metamodel.Element;
import ch.interlis.ili2c.metamodel.RoleDef;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom.IomObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Manages the INTERLIS objects from a Transfer.
 */
public final class ObjectPool {
    private static final Logger LOGGER = LogManager.getLogger();
    private final TransferDescription transferDescription;
    private final Map<String, Boolean> hasStableOidCache = new HashMap<>();
    private final Map<String, Integer> ignoredReferenceCountByRole = new HashMap<>();

    private final Map<String, ObjectValue> objectsByStableOID;

    /**
     * Creates a new instance of the ObjectPool.
     */
    public ObjectPool(Stream<IomObject> objects, TransferDescription transferDescription) {
        this.transferDescription = transferDescription;
        var factory = new ValueFactory(transferDescription);
        var objectList = objects.toList();
        this.objectsByStableOID = objectList.stream()
                .filter(o -> hasClassStableOid(o.getobjecttag()))
                .map(o ->  factory.createValue(o).orElseThrow(() -> new IllegalArgumentException("Could not create Value from object with Id " + o.getobjectoid())))
                .collect(Collectors.toMap(
                        ObjectValue::getOid,
                        Function.identity(),
                        (a, _) -> {
                            throw new IllegalStateException("Duplicate TID encountered " + a.getOid());
                        },
                        LinkedHashMap::new));

        var groups = objectList.stream().collect(Collectors.groupingBy(IomObject::getobjecttag));
        for (var entry : groups.entrySet()) {
            analyzeAssociations(entry.getKey(), entry.getValue());
        }

        ignoredReferenceCountByRole.forEach((roleName, count) ->
                LOGGER.warn("Ignored {} reference(s) of role \"{}\" because the objects are not part of the compared transfers or have no stable OID.", count, roleName));
    }

    /**
     * Gets an object by its transfer identifier (TID).
     *
     * @param tid The transfer identifier of the object.
     * @return The IomObject with the specified TID, or {@code null} if not found.
     */
    public Optional<ObjectValue> getObject(String tid) {
        return Optional.ofNullable(objectsByStableOID.get(tid));
    }

    /**
     * Returns a stream of all objects that have a stable OID.
     *
     * @return A stream of IomObjects with stable OIDs.
     */
    public Stream<ObjectValue> objectsWithStableOid() {
        return objectsByStableOID.values().stream();
    }

    /**
     * Removes an object.
     *
     * @param object The Object to remove.
     */
    public void remove(ObjectValue object) {
        objectsByStableOID.remove(object.getOid());
    }

    private boolean validateRoleHasTargetWithStableOid(RoleDef role) {
        if (toStream(role.iteratorDestination()).noneMatch(this::hasClassOrExtensionStableOid)) {
            LOGGER.warn("No target of role \"{}\" has a stable OID, the role is ignored in comparison.", role);
            return false;
        }

        return true;
    }

    private boolean hasClassOrExtensionStableOid(AbstractClassDef<?> classDef) {
        return classDef.getExtensions().stream()
                .anyMatch(extension -> hasClassStableOid(((Element) extension).getScopedName()));
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

    /**
     * Record to hold partitioned role definitions.
     *
     * @param embeddedRoles Map of embedded roles to their opposite roles.
     * @param standaloneRoles List of standalone role definitions.
     */
    private record AssociationRoles(Map<RoleDef, RoleDef> embeddedRoles, List<RoleDef> standaloneRoles) { }

    private void analyzeAssociations(String tag, List<IomObject> objects) {
        var classDef = getClassOrAssociationDef(tag);
        if (classDef == null) {
            return;
        }

        var partitionedRoles = analyzeRoles(classDef);
        processEmbeddedRoles(partitionedRoles.embeddedRoles(), objects);
        processStandaloneAssociations(partitionedRoles.standaloneRoles(), objects);
    }

    /**
     * Analyze roles into embedded and standalone roles.
     */
    private AssociationRoles analyzeRoles(AbstractClassDef<?> classDef) {
        var embeddedRoles = new HashMap<RoleDef, RoleDef>();
        var standaloneRoles = new ArrayList<RoleDef>();

        toStream(classDef.getAttributesAndRoles2())
                .filter(viewableElement -> viewableElement.obj instanceof RoleDef role && validateRoleHasTargetWithStableOid(role))
                .forEach(viewableElement -> {
                    var role = (RoleDef) viewableElement.obj;
                    if (viewableElement.embedded) {
                        var association = (AssociationDef) role.getContainer();
                        if (toStream(association.getAttributesAndRoles2()).anyMatch(a -> a.obj instanceof AttributeDef)) {
                            // Attributes of embedded association are not compared, because the association has no OID
                            LOGGER.warn("Embedded association \"{}\" has attributes that are not compared.", association.getScopedName());
                        }
                        embeddedRoles.put(role, role.getOppEnd());
                    } else {
                        standaloneRoles.add(role);
                    }
                });

        return new AssociationRoles(embeddedRoles, standaloneRoles);
    }

    /**
     * Processes embedded roles and adds bidirectional references between objects.
     */
    private void processEmbeddedRoles(Map<RoleDef, RoleDef> embeddedRoles, List<IomObject> objects) {
        embeddedRoles.forEach((role, oppositeRole) ->
                objects.stream()
                        .filter(object -> object.getattrvaluecount(role.getName()) > 0)
                        .forEach(object -> {
                            var oppositeRef = object.getattrobj(role.getName(), 0).getobjectrefoid();
                            var thisRef = object.getobjectoid();
                            var thisObject = objectsByStableOID.get(thisRef);
                            var oppositeObject = objectsByStableOID.get(oppositeRef);
                            if (thisObject == null || oppositeObject == null) {
                                LOGGER.debug(
                                        "Reference of role \"{}\" between \"{}\" and \"{}\" is ignored because an object is not part of the compared transfers or has no stable OID.",
                                        role.getName(), thisRef, oppositeRef);
                                ignoredReferenceCountByRole.merge(role.getScopedName(), 1, Integer::sum);
                                ignoredReferenceCountByRole.merge(oppositeRole.getScopedName(), 1, Integer::sum);
                                return;
                            }

                            thisObject.addReference(role.getName(), oppositeRef);
                            oppositeObject.addReference(oppositeRole.getName(), thisRef);
                        })
        );
    }

    /**
     * Processes standalone associations and adds cross-role references.
     */
    private void processStandaloneAssociations(List<RoleDef> standaloneRoles, List<IomObject> objects) {
        if (standaloneRoles.isEmpty()) {
            return;
        }

        objects.forEach(object -> {
            // Gather referenced OIDs of each role
            var roleReferences = standaloneRoles.stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            roleDef -> getAttrObj(object, roleDef.getName()).stream().map(IomObject::getobjectrefoid).toList()
                    ));

            // Add cross-role connections
            roleReferences.forEach((roleA, refsA) ->
                    roleReferences.entrySet().stream()
                            .filter(entry -> !entry.getKey().equals(roleA))
                            .forEach(entry -> {
                                var roleB = entry.getKey();
                                var refsB = entry.getValue();
                                refsA.forEach(refA ->
                                        refsB.forEach(refB -> {
                                            var objectA = objectsByStableOID.get(refA);
                                            if (objectA == null || !objectsByStableOID.containsKey(refB)) {
                                                LOGGER.debug(
                                                        "Reference of role \"{}\" between \"{}\" and \"{}\" is ignored because an object is not part of the compared transfers or has no stable OID.",
                                                        roleB.getName(), refA, refB);
                                                ignoredReferenceCountByRole.merge(roleB.getScopedName(), 1, Integer::sum);
                                                return;
                                            }

                                            objectA.addReference(roleB.getName(), refB);
                                        })
                                );
                            })
            );
        });
    }

    private AbstractClassDef<?> getClassOrAssociationDef(String className) {
        Element classElement = transferDescription.getElement(className);
        if (!(classElement instanceof AbstractClassDef<?> classDef)) {
            LOGGER.error("Class or Association \"{}\" not found.", className);
            return null;
        }

        return classDef;
    }

    private List<IomObject> getAttrObj(IomObject object, String attributeName) {
        var result = new ArrayList<IomObject>();
        for (var i = 0; i < object.getattrvaluecount(attributeName); i++) {
            result.add(object.getattrobj(attributeName, i));
        }

        return result;
    }

    private <T> Stream<T> toStream(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }
}
