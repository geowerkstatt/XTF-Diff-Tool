package ch.geowerkstatt.xtfdifftool;

import ch.interlis.ili2c.metamodel.*;
import ch.interlis.iom.IomObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the INTERLIS objects from a Transfer
 */
public class ObjectPool {
    private static final Logger LOGGER = LogManager.getLogger();
    private final TransferDescription transferDescription;
    private final Map<String, Boolean> hasStableOidCache = new HashMap<>();

    private final Collection<AnalyzedObject> objects;
    private final Map<String, AnalyzedObject> objectsByTID;

    public ObjectPool(Stream<IomObject> objects, TransferDescription transferDescription) {
        this.transferDescription = transferDescription;
        this.objects = objects
                .map(o -> new AnalyzedObject(o, validateObjectHasStableOid(o)))
                .toList();
        this.objectsByTID = this.objects.stream()
                .filter(o -> o.hasStableOid)
                .collect(Collectors.toMap(o -> o.object.getobjectoid(), Function.identity()));

        for (var object : this.objects) {
            analyzeAssociations(object);
        }
    }

    public AnalyzedObject getObject(String tid) {
        return objectsByTID.get(tid);
    }

    public Stream<IomObject> objectsWithStableOid() {
        return objectsByTID.values().stream().map(o -> o.object);
    }

    public Stream<IomObject> objectsWithStableOidUnvisited() {
        return objectsByTID.values().stream().filter(o -> !o.visited).map(o -> o.object);
    }

    /**
     * Validates that the INTERLIS class of the given IomObject has a stable OID.
     * @param iomObject The IomObject to validate.
     * @return True if the class has a stable OID.
     */
    private boolean validateObjectHasStableOid(IomObject iomObject) {
        return hasStableOidCache.computeIfAbsent(iomObject.getobjecttag(), this::hasClassStableOid);
    }

    private boolean hasClassStableOid(String className) {
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

    private void analyzeAssociations(AnalyzedObject analyzedObject) {
        var object = analyzedObject.object;
        var classDef = getClassOrAssociationDef(object.getobjecttag());
        if (classDef == null) {
            return;
        }

        var roles = new HashMap<String, List<String>>();
        var hasOrdinaryAttributes = false;
        for (var it = classDef.getAttributesAndRoles2(); it.hasNext(); ) {
            var viewableElement = it.next();
            if (viewableElement.obj instanceof RoleDef role && object.getattrvaluecount(role.getName()) > 0) {
                if (viewableElement.embedded) {
                    var oppositeRole = role.getOppEnd();

                    // There is only one attrobj otherwise the association would not be embedded
                    var associationObject = object.getattrobj(role.getName(), 0);
                    if (associationObject.getattrcount() > 0) {
                        LOGGER.warn("Association \"{}\" has no OID but attributes that are not compared.", associationObject.getobjecttag());
                    }

                    var oppositeRefs = objectsByTID.get(analyzedObject.object.getobjectoid()).associations.computeIfAbsent(role.getName(), _ -> new ArrayList<>());
                    oppositeRefs.add(associationObject.getobjectrefoid());
                    
                    var thisRefs = objectsByTID.get(associationObject.getobjectrefoid()).associations.computeIfAbsent(oppositeRole.getName(), _ -> new ArrayList<>());
                    thisRefs.add(analyzedObject.object.getobjectoid());
                } else {
                    var refs = roles.computeIfAbsent(role.getName(), _ -> new ArrayList<>());
                    for (var i = 0; i < object.getattrvaluecount(role.getName()); i++) {
                        refs.add(object.getattrobj(role.getName(), i).getobjectrefoid());
                    }
                }
            } else if (viewableElement.obj instanceof AttributeDef) {
                hasOrdinaryAttributes = true;
            }
        }

        if (!roles.isEmpty()) {
            if (!analyzedObject.hasStableOid && hasOrdinaryAttributes) {
                LOGGER.warn("Association \"{}\" has no OID but attributes that are not compared.", object.getobjecttag());
            }
            
            for (var entry : roles.entrySet()) {
                for (var otherEntry : roles.entrySet()) {
                    if (entry != otherEntry) {
                        var roleB = otherEntry.getKey();
                        for (var refA : entry.getValue()) {
                            for (var refB : otherEntry.getValue()) {
                                objectsByTID.get(refA).associations.computeIfAbsent(roleB, _ -> new ArrayList<>()).add(refB);
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

    public static final class AnalyzedObject {
        public final IomObject object;
        public final boolean hasStableOid;
        public boolean visited;
        public Map<String, List<String>> associations = new HashMap<>();

        AnalyzedObject(IomObject object, boolean hasStableOid) {
            this.object = object;
            this.hasStableOid = hasStableOid;
        }
    }
}
