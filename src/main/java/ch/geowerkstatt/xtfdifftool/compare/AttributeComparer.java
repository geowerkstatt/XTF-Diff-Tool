package ch.geowerkstatt.xtfdifftool.compare;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.interlis.ili2c.metamodel.RoleDef;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.iom.IomObject;

import java.util.Collections;
import java.util.List;

public interface AttributeComparer {
    /**
     * A collection of all known comparers.
     */
    AttributeComparer[] COMPARERS = {
            PrimitiveAttributeComparer.getInstance(),
            StructAttributeComparer.getInstance(),
            ReferenceAttributeComparer.getInstance(),
    };

    /**
     * Compare the attribute with all {@link AttributeComparer#COMPARERS} until a comparer returns a conclusive result.
     * <p>May still return {@link Equality#INCONCLUSIVE} if no comparer is able to compare the attribute.</p>
     */
    static Result compareAll(IomObject first, IomObject second, Type type, String attributePath) {
        for (var comparer : COMPARERS) {
            var result = comparer.compare(first, second, type, attributePath);
            if (result.equality != Equality.INCONCLUSIVE) {
                return result;
            }
        }

        return Result.INCONCLUSIVE;
    }

    /**
     * Compare the role with all {@link AttributeComparer#COMPARERS} until a comparer returns a conclusive result.
     * <p>May still return {@link Equality#INCONCLUSIVE} if no comparer is able to compare the role.</p>
     */
    static Result compareAll(IomObject first, IomObject second, RoleDef role, boolean embedded, String attributePath) {
        for (var comparer : COMPARERS) {
            var result = comparer.compareRole(first, second, role, embedded, attributePath);
            if (result.equality != Equality.INCONCLUSIVE) {
                return result;
            }
        }

        return Result.INCONCLUSIVE;
    }

    /**
     * Extracts the attribute name from the given attribute path.
     *
     * @param attributePath The attribute path
     * @return The attribute name
     */
    static String getAttributeName(String attributePath) {
        var lastIndex = attributePath.lastIndexOf('.');
        return lastIndex == -1 ? attributePath : attributePath.substring(lastIndex + 1);
    }

    /**
     * Extracts the specified attribute's values from the given {@link IomObject}s and compares them.
     *
     * @param first         The first IomObject
     * @param second        The second IomObject
     * @param type          The INTERLIS type of the attribute
     * @param attributePath The path of the attribute
     * @return The result of the comparison as a {@link Result}.
     */
    default Result compare(IomObject first, IomObject second, Type type, String attributePath) {
        return Result.INCONCLUSIVE;
    }

    /**
     * Extracts the specified role's values from the given {@link IomObject}s and compares them.
     *
     * @param first         The first IomObject
     * @param second        The second IomObject
     * @param role          The INTERLIS role definition
     * @param embedded      Whether the role is embedded
     * @param attributePath The path of the attribute
     * @return The result of the comparison as a {@link Result}.
     */
    default Result compareRole(IomObject first, IomObject second, RoleDef role, boolean embedded, String attributePath) {
        return Result.INCONCLUSIVE;
    }

    enum Equality {
        /** The compared objects are equal. */
        EQUAL,

        /** The compared objects differ. */
        DIFFERENT,

        /** The comparison did not yield a conclusive answer. */
        INCONCLUSIVE,
    }

    final class Result {
        /** Constant for {@link Equality#INCONCLUSIVE} comparison Results. */
        public static final Result INCONCLUSIVE = new Result(Equality.INCONCLUSIVE, Collections.emptyList());

        /** Constant for {@link Equality#EQUAL} comparison Results. */
        public static final Result EQUAL = new Result(Equality.EQUAL, Collections.emptyList());

        private final Equality equality;
        private final List<Change> changes;

        private Result(Equality equality, List<Change> changes) {
            this.equality = equality;
            this.changes = changes;
        }

        /** Creates a Result indicating the compared objects are different. */
        public static Result different(String attributeName, String oldValue, String newValue) {
            return new Result(Equality.DIFFERENT, List.of(Change.attribute(attributeName, oldValue, newValue)));
        }

        /** Creates a Result indicating the compared objects are different. */
        public static Result different(List<Change> changes) {
            return new Result(Equality.DIFFERENT, changes);
        }

        /** Returns the equality status of this result. */
        public Equality equality() {
            return equality;
        }

        /** Returns the list of changes. */
        public List<Change> changes() {
            return changes;
        }
    }
}
