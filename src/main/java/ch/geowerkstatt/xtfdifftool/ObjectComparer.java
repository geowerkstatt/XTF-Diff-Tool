package ch.geowerkstatt.xtfdifftool;

import ch.ehi.basics.settings.Settings;
import ch.geowerkstatt.xtfdifftool.diff.Change;
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

import java.util.Optional;
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
                            .orElseGet(() -> Stream.of(Change.deleted(object)));
                }),
                secondObjects.objectsWithStableOid().flatMap(object -> {
                    var matchingObject = firstObjects.getObject(object.getOid());
                    return matchingObject.isEmpty()
                            ? Stream.of(Change.added(object))
                            : Stream.of();
        }));
    }

    private Stream<Change> compareObjectValues(ObjectValue first, ObjectValue second) {
        try {
            var changes = first.getChanges(second);
            if (changes.isEmpty()) {
                return Stream.of();
            }

            // Each object is located once for all its changes. The list is built here, so that a failure while
            // locating also skips only this class.
            var firstLocation = first.getLocation();
            var secondLocation = second.getLocation();
            return changes.stream()
                    .map(o -> o.withObject(first).withLocations(locationOf(o.oldValue(), firstLocation), locationOf(o.newValue(), secondLocation)))
                    .toList()
                    .stream();
        } catch (Exception ex) {
            LOGGER.error("Could not compare class {}", first.getTag(), ex);
            return Stream.of();
        }
    }

    // A changed geometry locates itself. Any other change, and a geometry missing on one side, is located by the
    // geometry of its object.
    private static String locationOf(Value value, String objectLocation) {
        return Optional.ofNullable(value)
                .map(v -> Locations.combine(v.getLocations()))
                .orElse(objectLocation);
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
