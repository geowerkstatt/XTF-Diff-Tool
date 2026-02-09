package ch.geowerkstatt.xtfdifftool;

import ch.geowerkstatt.xtfdifftool.compare.ObjectComparer;
import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ChangeType;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ili2c.metamodel.Viewable;
import ch.interlis.iom.IomObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Analyzes the differences between two INTERLIS transfers.
 */
public final class ObjectAnalyzer {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ObjectPool firstObjects;
    private final ObjectPool secondObjects;
    private final TransferDescription transferDescription;

    /**
     * Creates a new ObjectAnalyzer for the given object streams.
     *
     * @param transferDescription The INTERLIS transfer description.
     * @param firstObjects        The objects of the first transfer.
     * @param secondObjects       The objects of the second transfer.
     */
    public ObjectAnalyzer(TransferDescription transferDescription, Stream<IomObject> firstObjects, Stream<IomObject> secondObjects) {
        this.transferDescription = transferDescription;
        this.firstObjects = new ObjectPool(firstObjects, transferDescription);
        this.secondObjects = new ObjectPool(secondObjects, transferDescription);
    }

    /**
     * Analyzes the differences between the two streams and passes each change to the {@code changeConsumer}.
     */
    public void analyzeDifferences(Consumer<Change> changeConsumer) {
        firstObjects.objectsWithStableOid().forEach(object -> {
            String oid = object.getobjectoid();
            var matchingObject = secondObjects.getObject(oid);
            if (matchingObject == null) {
                Change removeChange = new Change(
                        oid,
                        ChangeType.DELETED,
                        ValueType.OBJECT,
                        object.getobjecttag(),
                        null,
                        null,
                        null);
                changeConsumer.accept(removeChange);
            } else {
                secondObjects.markVisited(matchingObject);
                compareObjectValues(object, matchingObject, changeConsumer);
                compareAssociationReferences(object, matchingObject, changeConsumer);
            }
        });

        secondObjects.objectsWithStableOidUnvisited().forEach(object -> {
            Change addChange = new Change(
                    object.getobjectoid(),
                    ChangeType.ADDED,
                    ValueType.OBJECT,
                    object.getobjecttag(),
                    null,
                    null,
                    null);
            changeConsumer.accept(addChange);
        });
    }

    private void compareObjectValues(IomObject first, IomObject second, Consumer<Change> changeConsumer) {
        if (!first.getobjecttag().equals(second.getobjecttag())) {
            LOGGER.warn("Matching transfer objects have different INTERLIS classes. OID: {}", first.getobjectoid());
            return;
        }

        var element = transferDescription.getElement(first.getobjecttag());
        if (element instanceof Viewable<?> classElement) {
            ObjectComparer.compareAllAttributesAndRoles(classElement, first, second, "", changeConsumer);
        }
    }

    private void compareAssociationReferences(IomObject first, IomObject second, Consumer<Change> changeConsumer) {
        var firstAssociations = firstObjects.getAssociations(first.getobjectoid());
        var secondAssociations = secondObjects.getAssociations(second.getobjectoid());

        for (var entry : firstAssociations.entrySet()) {
            var refs = entry.getValue();
            var matchingRefs = secondAssociations.get(entry.getKey());
            if (matchingRefs == null) {
                changeConsumer.accept(new Change(
                        first.getobjectoid(),
                        ChangeType.DELETED,
                        ValueType.REFERENCE,
                        first.getobjecttag(),
                        entry.getKey(),
                        String.join(",", refs),
                        null));
            } else {
                secondAssociations.remove(entry.getKey());
                if (matchingRefs.size() == refs.size()) {
                    Collections.sort(refs);
                    Collections.sort(matchingRefs);

                    for (var i = 0; i < refs.size(); i++) {
                        if (!refs.get(i).equals(matchingRefs.get(i))) {
                            changeConsumer.accept(createReferenceChange(first, entry.getKey(), refs, matchingRefs));
                            continue;
                        }
                    }
                } else {
                    changeConsumer.accept(createReferenceChange(first, entry.getKey(), refs, matchingRefs));
                }
            }
        }

        for (var entry : secondAssociations.entrySet()) {
            changeConsumer.accept(new Change(
                    second.getobjectoid(),
                    ChangeType.ADDED,
                    ValueType.REFERENCE,
                    second.getobjecttag(),
                    entry.getKey(),
                    null,
                    String.join(",", entry.getValue())));
        }
    }

    private Change createReferenceChange(IomObject object, String role, List<String> oldRefs, List<String> newRefs) {
        return new Change(
                object.getobjectoid(),
                ChangeType.CHANGED,
                ValueType.REFERENCE,
                object.getobjecttag(),
                role,
                String.join(",", oldRefs),
                String.join(",", newRefs));
    }
}
