package ch.geowerkstatt.xtfdifftool;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ChangeType;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.geowerkstatt.xtfdifftool.value.ObjectValue;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom.IomObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Analyzes the differences between two INTERLIS transfers.
 */
public final class ObjectComparer {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ObjectPool firstObjects;
    private final ObjectPool secondObjects;

    /**
     * Creates a new ObjectAnalyzer for the given object streams.
     *
     * @param transferDescription The INTERLIS transfer description.
     * @param firstObjects        The objects of the first transfer.
     * @param secondObjects       The objects of the second transfer.
     */
    public ObjectComparer(TransferDescription transferDescription, Stream<IomObject> firstObjects, Stream<IomObject> secondObjects) {
        this.firstObjects = new ObjectPool(firstObjects, transferDescription);
        this.secondObjects = new ObjectPool(secondObjects, transferDescription);
    }

    /**
     * Analyzes the differences between the two streams and passes each change to the {@code changeConsumer}.
     */
    public void analyzeDifferences(Consumer<Change> changeConsumer) {
        firstObjects.objectsWithStableOid().forEach(object -> {
            String oid = object.getOid();
            var matchingObject = secondObjects.getObject(oid);
            if (matchingObject == null) {
                Change removeChange = new Change(oid, ChangeType.DELETED, ValueType.OBJECT, object.getTag(), null, null, null);
                changeConsumer.accept(removeChange);
            } else {
                secondObjects.remove(matchingObject);
                compareObjectValues(object, matchingObject, changeConsumer);
            }
        });

        secondObjects.objectsWithStableOid().forEach(object -> {
            Change addChange = new Change(object.getOid(), ChangeType.ADDED, ValueType.OBJECT, object.getTag(), null, null, null);
            changeConsumer.accept(addChange);
        });
    }

    private void compareObjectValues(ObjectValue first, ObjectValue second, Consumer<Change> changeConsumer) {
        try {
            var changes = first.getChanges(second).stream().map(o -> o.withObject(first));
            changes.forEach(changeConsumer);
        } catch (Exception ex) {
            LOGGER.error("Could not compare class {}", first.getTag(), ex);
        }
    }
}
