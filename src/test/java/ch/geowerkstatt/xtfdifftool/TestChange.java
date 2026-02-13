package ch.geowerkstatt.xtfdifftool;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ChangeType;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;

/**
 * A {@link Change} where the {@link TestChange#oldValue} and {@link TestChange#newValue} are Strings for easier testability.
 */
public record TestChange(
        String oid,
        ChangeType changeType,
        ValueType valueType,
        String interlisName,
        String attributePath,
        String oldValue,
        String newValue) {

    public TestChange(Change c) {
        this(
                c.oid(),
                c.changeType(),
                c.valueType(),
                c.interlisName(),
                c.attributePath(),
                c.oldValue() == null ? null : c.oldValue().toString(),
                c.newValue() == null ? null : c.newValue().toString());
    }
}
