package ch.geowerkstatt.xtfdifftool.compare;

import ch.geowerkstatt.xtfdifftool.diff.Change;
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
    };

    /**
     * Compare the attribute with all {@link AttributeComparer#COMPARERS} until a comparer returns a conclusive result.
     * <p>May still return {@link Equality#INCONCLUSIVE} if no comparer is able to compare the attribute.</p>
     */
    static Result compareAll(IomObject first, IomObject second, Type type, String attributeName) {
        for (var comparer : COMPARERS) {
            var result = comparer.compare(first, second, type, attributeName);
            if (result.equality != Equality.INCONCLUSIVE) {
                return result;
            }
        }

        return Result.INCONCLUSIVE;
    }

    /**
     * Extracts the specified attribute's values from the given {@link IomObject}s and compares them.
     *
     * @param first         The first IomObject
     * @param second        The second IomObject
     * @param attributeName The name of the attribute
     * @return The result of the comparison as a {@link Result}.
     */
    Result compare(IomObject first, IomObject second, Type type, String attributeName);

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
            return new Result(Equality.DIFFERENT, List.of(new Change(attributeName, oldValue, newValue)));
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
