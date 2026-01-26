package ch.geowerkstatt.xtfdifftool;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ChangeType;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.interlis.ili2c.metamodel.TransferDescription;
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
                        null);
                changeConsumer.accept(removeChange);
            } else {
                matchingObject.visited = true;
                comparePrimitiveAttributes(object, matchingObject.object, changeConsumer);
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

    /**
     * Compare the primitive attributes of two IomObjects without knowledge of the INTERLIS model and report changes via the changeConsumer.
     */
    private void comparePrimitiveAttributes(IomObject first, IomObject second, Consumer<Change> changeConsumer) {
        var primitiveAttributesFirst = getPrimitiveAttributes(first);
        var primitiveAttributesSecond = getPrimitiveAttributes(second);

        primitiveAttributesFirst.forEach((key, attribute) -> {
            var matchingAttribute = primitiveAttributesSecond.get(key);
            if (matchingAttribute == null) {
                changeConsumer.accept(new Change(
                        first.getobjectoid(),
                        ChangeType.DELETED,
                        ValueType.ATTRIBUTE,
                        first.getobjecttag(),
                        String.join(",", attribute),
                        null));
            } else {
                primitiveAttributesSecond.remove(key);
                if (!attribute.equals(matchingAttribute)) {
                    changeConsumer.accept(new Change(
                            first.getobjectoid(),
                            ChangeType.CHANGED,
                            ValueType.ATTRIBUTE,
                            first.getobjecttag(),
                            String.join(",", attribute),
                            String.join(",", matchingAttribute)));
                }
            }
        });

        primitiveAttributesSecond.forEach((key, attribute) -> {
            changeConsumer.accept(new Change(
                    first.getobjectoid(),
                    ChangeType.ADDED,
                    ValueType.ATTRIBUTE,
                    first.getobjecttag(),
                    null,
                    String.join(",", attribute)));
        });
    }

    private HashMap<String, List<String>> getPrimitiveAttributes(IomObject object) {
        var primitiveAttributes = new HashMap<String, List<String>>();
        for (var i = 0; i < object.getattrcount(); i++) {
            var name = object.getattrname(i);
            var elementCount = object.getattrvaluecount(name);
            for (var elementIndex = 0; elementIndex < elementCount; elementIndex++) {
                var value = object.getattrprim(name, elementIndex);
                if (value != null) {
                    primitiveAttributes.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
                }
            }
        }

        return primitiveAttributes;
    }
}
