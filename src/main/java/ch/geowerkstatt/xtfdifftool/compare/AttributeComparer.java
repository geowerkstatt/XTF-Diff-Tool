package ch.geowerkstatt.xtfdifftool.compare;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ChangeType;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.interlis.ili2c.metamodel.Type;
import ch.interlis.iom.IomObject;

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
     * @return The result of the comparison as a {@link AttributeComparer.Result}.
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

    record Result(Equality equality, String attributePath, String oldValue, String newValue) {
        /** Constant for {@link Equality#INCONCLUSIVE} comparison Results. */
        public static final Result INCONCLUSIVE = new Result(Equality.INCONCLUSIVE, null, null, null);

        /** Constant for {@link Equality#EQUAL} comparison Results. */
        public static final Result EQUAL = new Result(Equality.EQUAL, null, null, null);

        /**
         * Create a {@link Change} from this {@link Result}.
         */
        public Change toChange(String oid, String tag) {
            if (equality != Equality.DIFFERENT) {
                throw new IllegalStateException("Cannot convert " + equality + " Result to a Change");
            }

            var changeType = oldValue == null ? ChangeType.ADDED : newValue == null ? ChangeType.DELETED : ChangeType.CHANGED;
            return new Change(oid, changeType, ValueType.ATTRIBUTE, tag, attributePath, oldValue, newValue);
        }
    }
}
