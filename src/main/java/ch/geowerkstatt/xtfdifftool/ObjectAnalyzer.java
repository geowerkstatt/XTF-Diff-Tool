package ch.geowerkstatt.xtfdifftool;

import ch.geowerkstatt.xtfdifftool.compare.AttributeComparer;
import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ChangeType;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.interlis.ili2c.metamodel.AttributeDef;
import ch.interlis.ili2c.metamodel.Extendable;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ili2c.metamodel.Viewable;
import ch.interlis.iom.IomObject;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Analyzes the differences between two INTERLIS transfers.
 */
public final class ObjectAnalyzer {
    private final Stream<AnalyzedObject> firstObjects;
    private final Stream<AnalyzedObject> secondObjects;
    private final ModelValidator modelValidator;
    private final TransferDescription transferDescription;

    private static final class AnalyzedObject {
        private final IomObject object;
        private final boolean hasStableOid;
        private boolean visited;

        AnalyzedObject(IomObject object, boolean hasStableOid) {
            this.object = object;
            this.hasStableOid = hasStableOid;
        }
    }

    /**
     * Creates a new ObjectAnalyzer for the given object streams.
     * @param transferDescription The INTERLIS transfer description.
     * @param firstObjects The objects of the first transfer.
     * @param secondObjects The objects of the second transfer.
     */
    public ObjectAnalyzer(TransferDescription transferDescription, Stream<IomObject> firstObjects, Stream<IomObject> secondObjects) {
        this.transferDescription = transferDescription;
        this.modelValidator = new ModelValidator(transferDescription);
        this.firstObjects = firstObjects.map(this::validateObject);
        this.secondObjects = secondObjects.map(this::validateObject);
    }

    /**
     * Analyzes the differences between the two streams and passes each change to the {@code changeConsumer}.
     */
    public void analyzeDifferences(Consumer<Change> changeConsumer) {
        Map<String, AnalyzedObject> secondObjectMap = createObjectMap(secondObjects);

        firstObjects.forEach(analyzedObject -> {
            if (!analyzedObject.hasStableOid) {
                return;
            }

            IomObject object = analyzedObject.object;
            String oid = object.getobjectoid();
            AnalyzedObject matchingObject = secondObjectMap.get(oid);
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
                matchingObject.visited = true;
                compareAttributes(object, matchingObject.object, changeConsumer);
            }
        });

        for (AnalyzedObject remainingObject : secondObjectMap.values()) {
            if (remainingObject.hasStableOid && !remainingObject.visited) {
                IomObject object = remainingObject.object;
                Change addChange = new Change(
                        object.getobjectoid(),
                        ChangeType.ADDED,
                        ValueType.OBJECT,
                        object.getobjecttag(),
                        null,
                        null,
                        null);
                changeConsumer.accept(addChange);
            }
        }
    }

    private Map<String, AnalyzedObject> createObjectMap(Stream<AnalyzedObject> objects) {
        return objects.collect(Collectors.toMap(analyzedObject -> analyzedObject.object.getobjectoid(), Function.identity()));
    }

    private AnalyzedObject validateObject(IomObject iomObject) {
        boolean hasStableOid = modelValidator.validateObjectHasStableOid(iomObject);
        return new AnalyzedObject(iomObject, hasStableOid);
    }

    private void compareAttributes(IomObject first, IomObject second, Consumer<Change> changeConsumer) {
        if (!first.getobjecttag().equals(second.getobjecttag())) {
            System.err.println("WARNING: Matching Transfer Objects have Different INTERLIS classes. OID:" + first.getobjectoid());
            return;
        }

        var element = transferDescription.getElement(first.getobjecttag());
        if (element instanceof Viewable<?> classElement) {
            for (Iterator<Extendable> it = classElement.getAttributes(); it.hasNext();) {
                var attribute = (AttributeDef) it.next();
                var name = attribute.getName();
                if (first.getattrvaluecount(name) != 0 || second.getattrvaluecount(name) != 0) {
                    var type = attribute.getDomainResolvingAll();
                    var result = AttributeComparer.compareAll(first, second, type, name);
                    switch (result.equality()) {
                        case DIFFERENT -> result.changes().stream().map(c -> c.withObject(first)).forEach(changeConsumer);
                        case INCONCLUSIVE -> System.err.println("ERROR could not compare attribute " + name);
                        case null, default -> { }
                    }
                }
            }
        }
    }
}
