package ch.geowerkstatt.xtfdifftool;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ChangeType;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.interlis.iom.IomObject;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Analyzes the differences between two INTERLIS transfers.
 */
public final class XtfAnalyzer {
    private final Stream<IomObject> firstObjects;
    private final Stream<IomObject> secondObjects;

    /**
     * Creates a new XtfAnalyzer for the given object streams.
     * @param firstObjects The objects of the first transfer.
     * @param secondObjects The objects of the second transfer.
     */
    public XtfAnalyzer(Stream<IomObject> firstObjects, Stream<IomObject> secondObjects) {
        this.firstObjects = firstObjects;
        this.secondObjects = secondObjects;
    }

    /**
     * Analyzes the differences between the two streams and passes each change to the {@code changeConsumer}.
     */
    public void analyzeDifferences(Consumer<Change> changeConsumer) {
        Map<String, IomObject> secondObjectMap = createObjectMap(secondObjects);

        firstObjects.forEach(object -> {
            String oid = object.getobjectoid();
            IomObject matchingObject = secondObjectMap.get(oid);
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
                // Mark that the object of the second transfer has a matching entry in the first transfer
                secondObjectMap.put(oid, null);
            }
        });

        for (IomObject remainingObject : secondObjectMap.values()) {
            if (remainingObject != null) {
                Change addChange = new Change(
                        remainingObject.getobjectoid(),
                        ChangeType.ADDED,
                        ValueType.OBJECT,
                        remainingObject.getobjecttag(),
                        null,
                        null);
                changeConsumer.accept(addChange);
            }
        }
    }

    private Map<String, IomObject> createObjectMap(Stream<IomObject> objects) {
        return objects.collect(Collectors.toMap(IomObject::getobjectoid, Function.identity()));
    }
}
