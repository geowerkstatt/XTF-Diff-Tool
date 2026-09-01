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
    private final Map<RoleDef, Boolean> hasUnstableTargetCache = new HashMap<>();
    private final ReferenceStatistics referenceStatistics = new ReferenceStatistics();

    private final Map<String, ObjectValue> objectsByStableOID;
    private final Set<String> allTransferTids;

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

        this.allTransferTids = objectList.stream()
                .map(IomObject::getobjectoid)
                .filter(oid -> oid != null && !oid.isEmpty())
                .collect(Collectors.toSet());

        var groups = objectList.stream().collect(Collectors.groupingBy(IomObject::getobjecttag));
        for (var entry : groups.entrySet()) {
            analyzeAssociations(entry.getKey(), entry.getValue());
        }

        referenceStatistics.logSummary();
    }

    /**
     * Gets an object by its transfer identifier (TID).
     *
     * @param tid The transfer identifier of the object.
     * @return The {@link ObjectValue} with the specified TID, or an empty {@link Optional} if not found.
     */
    public Optional<ObjectValue> getObject(String tid) {
        return Optional.ofNullable(objectsByStableOID.get(tid));
    }

    /**
     * Returns a stream of all objects that have a stable OID.
     *
     * @return A stream of {@link ObjectValue} objects with stable OIDs.
     */
    public Stream<ObjectValue> objectsWithStableOid() {
        return objectsByStableOID.values().stream();
    }

    /**
     * Gets the statistics of the analyzed references. Visible for testing.
     */
    ReferenceStatistics getReferenceStatistics() {
        return referenceStatistics;
    }

    /**
     * Checks if any possible target of the role has no stable OID.
     * References of such roles to objects that are not part of the transfer might have an unstable OID.
     */
    private boolean hasRoleUnstableTarget(RoleDef role) {
        return hasUnstableTargetCache.computeIfAbsent(role, r -> getPossibleRoleTargets(r).anyMatch(classDef -> !hasClassStableOid(classDef)));
    }

    /**
     * Returns all non-abstract classes an object referenced by the role could belong to.
     */
    private Stream<AbstractClassDef<?>> getPossibleRoleTargets(RoleDef role) {
        return toStream(role.iteratorDestination())
                .flatMap(this::getClassWithExtensions)
                .filter(classDef -> !classDef.isAbstract());
    }

    private Stream<AbstractClassDef<?>> getClassWithExtensions(AbstractClassDef<?> classDef) {
        Set<?> extensions = classDef.getExtensions();
        return extensions.stream().map(extension -> (AbstractClassDef<?>) extension);
    }

    private boolean hasClassStableOid(AbstractClassDef<?> classDef) {
        return hasStableOidCache.computeIfAbsent(classDef.getScopedName(), _ -> calculateHasClassStableOid(classDef));
    }

    private boolean hasClassStableOid(String className) {
        return hasStableOidCache.computeIfAbsent(className, _ -> {
            var classDef = getClassOrAssociationDef(className);
            return classDef != null && calculateHasClassStableOid(classDef);
        });
    }

    private boolean calculateHasClassStableOid(AbstractClassDef<?> classDef) {
        Domain oid = classDef.getOid();
        if (oid == null) {
            LOGGER.warn("Class or Association \"{}\" has no stable OID.", classDef.getScopedName());
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
                .forEach(viewableElement -> {
                    if (!(viewableElement.obj instanceof RoleDef role)) {
                        return;
                    }

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
                            if (thisObject == null && oppositeObject == null) {
                                referenceStatistics.count(ReferenceOutcome.IGNORED_NO_STABLE_ENDPOINT, role, thisRef, oppositeRef);
                                referenceStatistics.count(ReferenceOutcome.IGNORED_NO_STABLE_ENDPOINT, oppositeRole, oppositeRef, thisRef);
                                return;
                            }

                            if (thisObject != null) {
                                addReference(thisObject, role, oppositeRef);
                            }
                            if (oppositeObject != null) {
                                addReference(oppositeObject, oppositeRole, thisRef);
                            }
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
                    roleReferences.forEach((roleB, refsB) -> {
                        if (!roleA.equals(roleB)) {
                            addCrossReferences(refsA, roleB, refsB);
                        }
                    })
            );
        });
    }

    /**
     * Adds the references of role B to the objects referenced by role A of the same association instance.
     */
    private void addCrossReferences(List<String> refsA, RoleDef roleB, List<String> refsB) {
        for (var refA : refsA) {
            var objectA = objectsByStableOID.get(refA);
            for (var refB : refsB) {
                if (objectA != null) {
                    addReference(objectA, roleB, refB);
                } else if (!objectsByStableOID.containsKey(refB)) {
                    // The reference from the side of role A is compared with swapped roles, if that object exists
                    referenceStatistics.count(ReferenceOutcome.IGNORED_NO_STABLE_ENDPOINT, roleB, refA, refB);
                }
            }
        }
    }

    /**
     * Adds the reference to the object. References to objects that are part of the transfer without
     * a stable OID are ignored, because their TIDs carry no identity between transfers. References to
     * objects that are not part of the transfer are compared by TID; if such a reference could target
     * a class without stable OID, it is reported.
     */
    private void addReference(ObjectValue object, RoleDef role, String referencedOid) {
        if (!objectsByStableOID.containsKey(referencedOid)) {
            if (allTransferTids.contains(referencedOid)) {
                referenceStatistics.count(ReferenceOutcome.IGNORED_UNSTABLE_IN_TRANSFER, role, object.getOid(), referencedOid);
                return;
            }

            if (hasRoleUnstableTarget(role)) {
                referenceStatistics.count(ReferenceOutcome.COMPARED_MAYBE_UNSTABLE, role, object.getOid(), referencedOid);
            }
        }

        object.addReference(role.getName(), referencedOid);
    }

    /**
     * The possible outcomes for a reference that is not compared like a regular reference between
     * two objects with stable OIDs, with the log messages describing them.
     */
    enum ReferenceOutcome {
        IGNORED_NO_STABLE_ENDPOINT(
                "Ignored reference of role \"{}\" from \"{}\" to \"{}\" because no involved object is part of the transfer with a stable OID.",
                "Ignored {} reference(s) of role \"{}\" because no involved object is part of the transfer with a stable OID."),
        IGNORED_UNSTABLE_IN_TRANSFER(
                "Ignored reference of role \"{}\" from \"{}\" to \"{}\" because the referenced object is part of the transfer but has no stable OID.",
                "Ignored {} reference(s) of role \"{}\" because the referenced objects are part of the transfer but have no stable OID."),
        COMPARED_MAYBE_UNSTABLE(
                "Compared reference of role \"{}\" from \"{}\" to \"{}\" that may have an unstable OID.",
                "Compared {} reference(s) of role \"{}\" that may have an unstable OID.");

        private final String debugMessage;
        private final String summaryMessage;

        ReferenceOutcome(String debugMessage, String summaryMessage) {
            this.debugMessage = debugMessage;
            this.summaryMessage = summaryMessage;
        }
    }

    /**
     * Tallies the {@link ReferenceOutcome} of the analyzed references per role and reports them
     * as aggregated warnings at the end of the analysis.
     */
    static final class ReferenceStatistics {
        private final Map<ReferenceOutcome, Map<RoleDef, Integer>> countsByOutcome = new EnumMap<>(ReferenceOutcome.class);

        void count(ReferenceOutcome outcome, RoleDef role, String fromRef, String toRef) {
            LOGGER.debug(outcome.debugMessage, role.getName(), fromRef, toRef);
            countsByOutcome.computeIfAbsent(outcome, _ -> new LinkedHashMap<>()).merge(role, 1, Integer::sum);
        }

        void logSummary() {
            countsByOutcome.forEach((outcome, countByRole) ->
                    countByRole.forEach((role, count) -> LOGGER.warn(outcome.summaryMessage, count, role.getScopedName())));
        }

        /**
         * Gets the reference counts of the outcome by the scoped name of the role. Visible for testing.
         */
        Map<String, Integer> getCounts(ReferenceOutcome outcome) {
            return countsByOutcome.getOrDefault(outcome, Map.of()).entrySet().stream()
                    .collect(Collectors.toMap(entry -> entry.getKey().getScopedName(), Map.Entry::getValue));
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
