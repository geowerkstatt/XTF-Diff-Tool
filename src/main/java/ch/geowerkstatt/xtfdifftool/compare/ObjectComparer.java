package ch.geowerkstatt.xtfdifftool.compare;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.interlis.ili2c.metamodel.AttributeDef;
import ch.interlis.ili2c.metamodel.Element;
import ch.interlis.ili2c.metamodel.Viewable;
import ch.interlis.ili2c.metamodel.ViewableTransferElement;
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
        compareViewable(viewable, first, second, attributePath, changeConsumer, false);
    }

    /**
     * Compares all attributes and roles of the given objects.
     * @param viewable The class, struct or association definition of the objects.
     * @param first The first object.
     * @param second The second object.
     * @param attributePath The attribute path leading to the objects, can be empty for root objects.
     * @param changeConsumer The consumer to receive detected changes.
     */
    public static void compareAllAttributesAndRoles(Viewable<?> viewable, IomObject first, IomObject second, String attributePath, Consumer<Change> changeConsumer) {
        compareViewable(viewable, first, second, attributePath, changeConsumer, true);
    }

    private static void compareViewable(Viewable<?> viewable, IomObject first, IomObject second, String attributePath, Consumer<Change> changeConsumer, boolean includeRoles) {
        for (Iterator<ViewableTransferElement> it = viewable.getAttributesAndRoles2(); it.hasNext();) {
            var viewableElement = it.next();
            var name = ((Element) viewableElement.obj).getName();
            if (first.getattrvaluecount(name) == 0 && second.getattrvaluecount(name) == 0) {
                continue;
            }

            var path = attributePath.isEmpty() ? name : attributePath + "." + name;
            var result = switch (viewableElement.obj) {
                case AttributeDef attribute -> AttributeComparer.compareAll(first, second, attribute.getDomainResolvingAll(), path);
                default -> null;
            };
            if (result != null) {
                switch (result.equality()) {
                    case DIFFERENT -> result.changes().stream().map(c -> c.oid() == null ? c.withObject(first) : c).forEach(changeConsumer);
                    case INCONCLUSIVE -> LOGGER.error("Could not compare attribute or role {} of {}", name, viewable.getName());
                    case null, default -> { }
                }
            }
        }
    }
}
