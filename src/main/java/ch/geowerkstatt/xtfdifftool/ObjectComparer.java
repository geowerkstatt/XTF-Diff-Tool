package ch.geowerkstatt.xtfdifftool;

import ch.ehi.basics.settings.Settings;
import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ChangeType;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.geowerkstatt.xtfdifftool.value.ObjectValue;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom.IomObject;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.ObjectEvent;
import ch.interlis.iox_j.filter.Rounder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        var rounder = new RounderFacade(transferDescription);
        this.firstObjects = new ObjectPool(firstObjects.map(rounder::roundIomObj), transferDescription);
        this.secondObjects = new ObjectPool(secondObjects.map(rounder::roundIomObj), transferDescription);
    }

    /**
     * Analyzes the differences between the two streams and passes each change to the {@code changeConsumer}.
     */
    public Stream<Change> analyzeDifferences() {
        var firstObjectChanges = firstObjects.objectsWithStableOid().flatMap(object -> {
            String oid = object.getOid();
            var matchingObject = secondObjects.getObject(oid);
            if (matchingObject == null) {
                Change removeChange = new Change(oid, ChangeType.DELETED, ValueType.OBJECT, object.getTag(), null, null, null);
                return Stream.of(removeChange);
            } else {
                return compareObjectValues(object, matchingObject);
            }
        });

        var secondObjectsAddedChanges = secondObjects.objectsWithStableOid()
                .flatMap(object -> {
                    var matchingObject = firstObjects.getObject(object.getOid());
                    if (matchingObject == null) {
                        return Stream.of(new Change(object.getOid(), ChangeType.ADDED, ValueType.OBJECT, object.getTag(), null, null, null));
                    } else {
                        return Stream.of();
                    }
                });

        return Stream.concat(firstObjectChanges, secondObjectsAddedChanges);
    }

    private Stream<Change> compareObjectValues(ObjectValue first, ObjectValue second) {
        try {
            return first.getChanges(second).stream().map(o -> o.withObject(first));
        } catch (Exception ex) {
            LOGGER.error("Could not compare class {}", first.getTag(), ex);
            return Stream.of();
        }
    }

    private record RounderFacade(Rounder rounder) {
        private RounderFacade(TransferDescription rounder) {
            this(new Rounder(rounder, new Settings()));
        }

        public IomObject roundIomObj(IomObject input) {
            try {
                var event = new ObjectEvent(input);
                rounder.filter(event);
            } catch (IoxException e) {
                LOGGER.debug("Could not round object with id {}", input.getobjectoid(), e);
            }

            return input;
        }
    }
}
