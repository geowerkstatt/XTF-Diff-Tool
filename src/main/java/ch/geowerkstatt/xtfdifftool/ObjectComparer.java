package ch.geowerkstatt.xtfdifftool;

import ch.ehi.basics.settings.Settings;
import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ChangeType;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.geowerkstatt.xtfdifftool.value.Locations;
import ch.geowerkstatt.xtfdifftool.value.ObjectValue;
import ch.geowerkstatt.xtfdifftool.value.Value;
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
     * Creates a new ObjectAnalyzer for the given object streams, without the locations of the changes.
     *
     * @param transferDescription The INTERLIS transfer description.
     * @param firstObjects        The objects of the first transfer.
     * @param secondObjects       The objects of the second transfer.
     */
    public ObjectComparer(TransferDescription transferDescription, Stream<IomObject> firstObjects, Stream<IomObject> secondObjects) {
        this(transferDescription, firstObjects, secondObjects, false);
    }

    /**
     * Creates a new ObjectAnalyzer for the given object streams.
     *
     * @param transferDescription The INTERLIS transfer description.
     * @param firstObjects        The objects of the first transfer.
     * @param secondObjects       The objects of the second transfer.
     * @param includeLocations    Whether each change gets its location in the old and the new state.
     */
    public ObjectComparer(TransferDescription transferDescription, Stream<IomObject> firstObjects, Stream<IomObject> secondObjects, boolean includeLocations) {
        var rounder = new RounderFacade(transferDescription);
        this.firstObjects = new ObjectPool(firstObjects.map(rounder::roundIomObj), transferDescription, includeLocations);
        this.secondObjects = new ObjectPool(secondObjects.map(rounder::roundIomObj), transferDescription, includeLocations);
    }

    /**
     * Analyzes the differences between the two streams and passes each change to the {@code changeConsumer}.
     */
    public Stream<Change> analyzeDifferences() {
        return Stream.concat(
                firstObjects.objectsWithStableOid().flatMap(object -> {
                    var matchingObject = secondObjects.getObject(object.getOid());
                    return matchingObject
                            .map(m -> compareObjectValues(object, m))
                            .orElseGet(() -> Stream.of(new Change(object.getOid(), ChangeType.DELETED, ValueType.OBJECT, object.getTag(), null, null, null)
                                    .withLocations(object.getLocation(), null)));
                }),
                secondObjects.objectsWithStableOid().flatMap(object -> {
                    var matchingObject = firstObjects.getObject(object.getOid());
                    return matchingObject.isEmpty()
                            ? Stream.of(new Change(object.getOid(), ChangeType.ADDED, ValueType.OBJECT, object.getTag(), null, null, null)
                                    .withLocations(null, object.getLocation()))
                            : Stream.of();
        }));
    }

    private Stream<Change> compareObjectValues(ObjectValue first, ObjectValue second) {
        try {
            return first.getChanges(second).stream()
                    .map(o -> o.withObject(first).withLocations(locationOf(o, first, o.oldValue()), locationOf(o, second, o.newValue())));
        } catch (Exception ex) {
            LOGGER.error("Could not compare class {}", first.getTag(), ex);
            return Stream.of();
        }
    }

    // A changed geometry locates itself, any other change is located by the geometry of its object.
    private static String locationOf(Change change, ObjectValue object, Value value) {
        if (change.valueType() != ValueType.GEOMETRY) {
            return object.getLocation();
        }

        return value == null ? null : Locations.combine(value.getLocations());
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
