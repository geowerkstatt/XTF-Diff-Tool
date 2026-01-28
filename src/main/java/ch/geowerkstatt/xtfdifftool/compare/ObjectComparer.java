package ch.geowerkstatt.xtfdifftool.compare;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.interlis.ili2c.metamodel.AttributeDef;
import ch.interlis.ili2c.metamodel.Extendable;
import ch.interlis.ili2c.metamodel.Viewable;
import ch.interlis.iom.IomObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Iterator;
import java.util.function.Consumer;

public final class ObjectComparer {
    private static final Logger LOGGER = LogManager.getLogger();

    private ObjectComparer() { }

    /**
     * Compares all attributes of the given objects.
     * @param viewable The class, struct or association definition of the objects.
     * @param first The first object.
     * @param second The second object.
     * @param attributePath The attribute path leading to the objects, can be empty for root objects.
     * @param changeConsumer The consumer to receive detected changes.
     */
    public static void compareAllAttributes(Viewable<?> viewable, IomObject first, IomObject second, String attributePath, Consumer<Change> changeConsumer) {
        for (Iterator<Extendable> it = viewable.getAttributes(); it.hasNext();) {
            var attribute = (AttributeDef) it.next();
            var name = attribute.getName();
            if (first.getattrvaluecount(name) != 0 || second.getattrvaluecount(name) != 0) {
                var type = attribute.getDomainResolvingAll();
                var path = attributePath.isEmpty() ? name : attributePath + "." + name;
                var result = AttributeComparer.compareAll(first, second, type, path);
                switch (result.equality()) {
                    case DIFFERENT -> result.changes().stream().map(c -> c.withObject(first)).forEach(changeConsumer);
                    case INCONCLUSIVE -> LOGGER.error("Could not compare attribute {} of {}", name, viewable.getName());
                    case null, default -> { }
                }
            }
        }
    }
}
