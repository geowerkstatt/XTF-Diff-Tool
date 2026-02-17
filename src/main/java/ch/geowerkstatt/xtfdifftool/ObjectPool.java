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

    private void analyzeAssociations(String tag, List<IomObject> objects) {
        var classDef = getClassOrAssociationDef(tag);
        if (classDef == null) {
            return;
        }

        // Analyze Associations and Roles
        var roleDefs = new ArrayList<RoleDef>();
        var embeddedRoleDefs = new HashMap<RoleDef, RoleDef>();
        for (var it = classDef.getAttributesAndRoles2(); it.hasNext();) {
            var viewableElement = it.next();
            if (viewableElement.obj instanceof RoleDef role && validateRoleHasTargetWithStableOid(role)) {
                if (viewableElement.embedded) {
                    var association = (AssociationDef) role.getContainer();
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

            for (var object : objects) {
                if (object.getattrvaluecount(role.getName()) > 0) {
                    var oppositeRef = object.getattrobj(role.getName(), 0).getobjectrefoid();
                    var thisRef = object.getobjectoid();
                    objectsByStableOID.get(thisRef).addReference(role.getName(), oppositeRef);
                    objectsByStableOID.get(oppositeRef).addReference(oppositeRole.getName(), thisRef);
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
                    for (var i = 0; i < analyzedObject.getattrvaluecount(roleDef.getName()); i++) {
                        refs.add(analyzedObject.getattrobj(roleDef.getName(), i).getobjectrefoid());
                    }
                }

                // Add connections
                for (var entry : roles.entrySet()) {
                    for (var otherEntry : roles.entrySet()) {
                        if (entry != otherEntry) {
                            var roleB = otherEntry.getKey();
                            for (var refA : entry.getValue()) {
                                for (var refB : otherEntry.getValue()) {
                                    objectsByStableOID.get(refA).addReference(roleB, refB);
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

    private <T> Stream<T> toStream(Iterator<T> iterator) {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, Spliterator.ORDERED), false);
    }
}
