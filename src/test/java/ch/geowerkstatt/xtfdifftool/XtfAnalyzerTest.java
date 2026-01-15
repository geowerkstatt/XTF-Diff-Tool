package ch.geowerkstatt.xtfdifftool;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ChangeType;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.Iom_jObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public class XtfAnalyzerTest {
    private static final String INTERLIS_CLASS_NAME = "XtfAnalyzerTest.Topic.Class";

    @Test
    public void analyzeNoObjects() {
        XtfAnalyzer analyzer = new XtfAnalyzer(Stream.empty(), Stream.empty());
        List<Change> changes = getChanges(analyzer);
        assertIterableEquals(Collections.emptyList(), changes);
    }

    @Test
    public void analyzeSameObjects() {
        List<IomObject> first = List.of(
                createObject("o1"),
                createObject("o2"),
                createObject("o3")
        );
        List<IomObject> second = List.of(
                createObject("o2"),
                createObject("o1"),
                createObject("o3")
        );

        XtfAnalyzer analyzer = new XtfAnalyzer(first.stream(), second.stream());
        List<Change> changes = getChanges(analyzer);
        assertIterableEquals(Collections.emptyList(), changes);
    }

    @Test
    public void analyzeAddedObjects() {
        List<IomObject> first = List.of(
                createObject("o1")
        );
        List<IomObject> second = List.of(
                createObject("o1"),
                createObject("o2"),
                createObject("o3")
        );

        List<Change> expectedChanges = List.of(
                createChange("0", "o2", ChangeType.ADDED),
                createChange("1", "o3", ChangeType.ADDED)
        );

        XtfAnalyzer analyzer = new XtfAnalyzer(first.stream(), second.stream());
        List<Change> changes = getChanges(analyzer);
        assertIterableEquals(expectedChanges, changes);
    }

    @Test
    public void analyzeDeletedObjects() {
        List<IomObject> first = List.of(
                createObject("o1"),
                createObject("o2"),
                createObject("o3")
        );
        List<IomObject> second = List.of(
                createObject("o2")
        );

        List<Change> expectedChanges = List.of(
                createChange("0", "o1", ChangeType.DELETED),
                createChange("1", "o3", ChangeType.DELETED)
        );

        XtfAnalyzer analyzer = new XtfAnalyzer(first.stream(), second.stream());
        List<Change> changes = getChanges(analyzer);
        assertIterableEquals(expectedChanges, changes);
    }

    @Test
    public void analyzeDifferentObjects() {
        List<IomObject> first = List.of(
                createObject("o1"),
                createObject("o2"),
                createObject("o3")
        );
        List<IomObject> second = List.of(
                createObject("o4"),
                createObject("o3"),
                createObject("o2")
        );

        List<Change> expectedChanges = List.of(
                createChange("0", "o1", ChangeType.DELETED),
                createChange("1", "o4", ChangeType.ADDED)
        );

        XtfAnalyzer analyzer = new XtfAnalyzer(first.stream(), second.stream());
        List<Change> changes = getChanges(analyzer);
        assertIterableEquals(expectedChanges, changes);
    }

    private IomObject createObject(String oid) {
        return new Iom_jObject(INTERLIS_CLASS_NAME, oid);
    }

    private Change createChange(String changeId, String oid, ChangeType type) {
        return new Change(changeId, oid, type, ValueType.OBJECT, INTERLIS_CLASS_NAME, null, null);
    }

    private List<Change> getChanges(XtfAnalyzer analyzer) {
        List<Change> changes = new ArrayList<>();
        analyzer.analyzeDifferences(changes::add);
        return changes;
    }
}
