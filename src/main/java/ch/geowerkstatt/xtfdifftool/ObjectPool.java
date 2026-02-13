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
                .collect(Collectors.toMap(
                        IomObject::getobjectoid,
                        factory::createValue,
                        (a, _) -> {
                            throw new IllegalStateException("Duplicate TID encountered " + a.getOid());
                        },
                        LinkedHashMap::new));
    }

    /**
     * Gets an object by its transfer identifier (TID).
     *
     * @param tid The transfer identifier of the object.
     * @return The IomObject with the specified TID, or {@code null} if not found.
     */
    public ObjectValue getObject(String tid) {
        return objectsByStableOID.get(tid);
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

    private AbstractClassDef<?> getClassOrAssociationDef(String className) {
        Element classElement = transferDescription.getElement(className);
        if (!(classElement instanceof AbstractClassDef<?> classDef)) {
            LOGGER.error("Class or Association \"{}\" not found.", className);
            return null;
        }

        return classDef;
    }
}
