package ch.geowerkstatt.xtfdifftool;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ChangeType;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.config.FileEntry;
import ch.interlis.ili2c.config.FileEntryKind;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.Iom_jObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;

public final class ObjectAnalyzerTest {
    private static final String MODEL_FILE = "src/test/data/ObjectAnalyzerTest/Model.ili";
    private static final String INTERLIS_CLASS_NAME = "ObjectAnalyzerTest.Topic.Class";
    private static final String INTERLIS_CLASS_NAME_WITHOUT_ID = "ObjectAnalyzerTest.Topic.ClassWithoutId";
    private TransferDescription transferDescription;

    @BeforeEach
    public void setUp() {
        Configuration config = new Configuration();
        config.addFileEntry(new FileEntry(MODEL_FILE, FileEntryKind.ILIMODELFILE));
        transferDescription = ch.interlis.ili2c.Main.runCompiler(config);
    }

    @Test
    public void analyzeNoObjects() {
        ObjectAnalyzer analyzer = new ObjectAnalyzer(transferDescription, Stream.empty(), Stream.empty());
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

        ObjectAnalyzer analyzer = new ObjectAnalyzer(transferDescription, first.stream(), second.stream());
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
                createChange("o2", ChangeType.ADDED),
                createChange("o3", ChangeType.ADDED)
        );

        ObjectAnalyzer analyzer = new ObjectAnalyzer(transferDescription, first.stream(), second.stream());
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
                createChange("o1", ChangeType.DELETED),
                createChange("o3", ChangeType.DELETED)
        );

        ObjectAnalyzer analyzer = new ObjectAnalyzer(transferDescription, first.stream(), second.stream());
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
                createChange("o1", ChangeType.DELETED),
                createChange("o4", ChangeType.ADDED)
        );

        ObjectAnalyzer analyzer = new ObjectAnalyzer(transferDescription, first.stream(), second.stream());
        List<Change> changes = getChanges(analyzer);
        assertIterableEquals(expectedChanges, changes);
    }

    @Test
    public void analyzeDifferentPrimitiveAttribute() {
        var first = List.of(createObject("o1", obj -> {
            obj.setattrvalue("text", "Wazuviti");
            obj.setattrvalue("text2", "Unchanged");
            obj.addattrvalue("textList", "A");
            obj.addattrvalue("textList", "B");
        }));
        var second = List.of(createObject("o1", obj -> {
            obj.setattrvalue("text", "Fazakapo");
            obj.setattrvalue("text2", "Unchanged");
            obj.addattrvalue("textList", "A");
            obj.addattrvalue("textList", "C");
            obj.addattrvalue("textList", "D");
        }));

        var expectedChanges = List.of(
                new Change("o1", ChangeType.CHANGED, ValueType.ATTRIBUTE, INTERLIS_CLASS_NAME, "Wazuviti", "Fazakapo"),
                new Change("o1", ChangeType.CHANGED, ValueType.ATTRIBUTE, INTERLIS_CLASS_NAME, "A,B", "A,C,D")
        );

        ObjectAnalyzer analyzer = new ObjectAnalyzer(transferDescription, first.stream(), second.stream());
        List<Change> changes = getChanges(analyzer);

        assertThat(changes).containsExactlyInAnyOrderElementsOf(expectedChanges);
    }

    @Test
    public void analyzeDeletePrimitiveAttribute() {
        var first = List.of(createObject("o1", obj -> {
            obj.setattrvalue("text", "Wazuviti");
            obj.setattrvalue("text2", "Unchanged");
            obj.addattrvalue("textList", "A");
            obj.addattrvalue("textList", "B");
        }));
        var second = List.of(createObject("o1", obj -> {
            obj.setattrvalue("text2", "Unchanged");
        }));

        var expectedChanges = List.of(
                new Change("o1", ChangeType.DELETED, ValueType.ATTRIBUTE, INTERLIS_CLASS_NAME, "Wazuviti", null),
                new Change("o1", ChangeType.DELETED, ValueType.ATTRIBUTE, INTERLIS_CLASS_NAME, "A,B", null)
        );

        ObjectAnalyzer analyzer = new ObjectAnalyzer(transferDescription, first.stream(), second.stream());
        List<Change> changes = getChanges(analyzer);

        assertThat(changes).containsExactlyInAnyOrderElementsOf(expectedChanges);
    }

    @Test
    public void analyzeAddedPrimitiveAttribute() {
        var first = List.of(createObject("o1", obj -> {
            obj.setattrvalue("text2", "Unchanged");
        }));
        var second = List.of(createObject("o1", obj -> {
            obj.setattrvalue("text", "Fazakapo");
            obj.setattrvalue("text2", "Unchanged");
            obj.addattrvalue("textList", "A");
            obj.addattrvalue("textList", "C");
        }));

        var expectedChanges = List.of(
                new Change("o1", ChangeType.ADDED, ValueType.ATTRIBUTE, INTERLIS_CLASS_NAME, null, "Fazakapo"),
                new Change("o1", ChangeType.ADDED, ValueType.ATTRIBUTE, INTERLIS_CLASS_NAME, null, "A,C")
        );

        ObjectAnalyzer analyzer = new ObjectAnalyzer(transferDescription, first.stream(), second.stream());
        List<Change> changes = getChanges(analyzer);

        assertThat(changes).containsExactlyInAnyOrderElementsOf(expectedChanges);
    }

    @Test
    public void analyzeDifferentPrimitiveCollectionAttribute() {
        var first = List.of(createObject("o1", obj -> {
            obj.addattrvalue("textList", "A");
            obj.addattrvalue("textList", "B");
            obj.addattrvalue("textBag", "C");
            obj.addattrvalue("textBag", "D");
        }), createObject("o2", obj -> {
            obj.addattrvalue("textBag", "S");
            obj.addattrvalue("textBag", "E");
            obj.addattrvalue("textBag", "T");
        }));
        var second = List.of(createObject("o1", obj -> {
            obj.addattrvalue("textList", "B");
            obj.addattrvalue("textList", "A");
            obj.addattrvalue("textBag", "D");
            obj.addattrvalue("textBag", "C");
        }), createObject("o2", obj -> {
            obj.addattrvalue("textBag", "T");
            obj.addattrvalue("textBag", "E");
            obj.addattrvalue("textBag", "A");
        }));

        var expectedChanges = List.of(
                new Change("o1", ChangeType.CHANGED, ValueType.ATTRIBUTE, INTERLIS_CLASS_NAME, "A,B", "B,A"),
                new Change("o2", ChangeType.CHANGED, ValueType.ATTRIBUTE, INTERLIS_CLASS_NAME, "E,S,T", "A,E,T")
        );

        ObjectAnalyzer analyzer = new ObjectAnalyzer(transferDescription, first.stream(), second.stream());
        List<Change> changes = getChanges(analyzer);

        assertThat(changes).containsExactlyInAnyOrderElementsOf(expectedChanges);
    }

    @Test
    public void analyzeIgnoresObjectsWithoutId() {
        List<IomObject> first = List.of(
                new Iom_jObject(INTERLIS_CLASS_NAME_WITHOUT_ID, "123"),
                new Iom_jObject(INTERLIS_CLASS_NAME_WITHOUT_ID, "456")
        );
        List<IomObject> second = List.of(
                new Iom_jObject(INTERLIS_CLASS_NAME_WITHOUT_ID, "789")
        );

        ObjectAnalyzer analyzer = new ObjectAnalyzer(transferDescription, first.stream(), second.stream());
        List<Change> changes = getChanges(analyzer);
        assertIterableEquals(Collections.emptyList(), changes);
    }

    private IomObject createObject(String oid, Consumer<Iom_jObject> configure) {
        var result = new Iom_jObject(INTERLIS_CLASS_NAME, oid);
        configure.accept(result);
        return result;
    }

    private IomObject createObject(String oid) {
        return new Iom_jObject(INTERLIS_CLASS_NAME, oid);
    }

    private Change createChange(String oid, ChangeType type) {
        return new Change(oid, type, ValueType.OBJECT, INTERLIS_CLASS_NAME, null, null);
    }

    private List<Change> getChanges(ObjectAnalyzer analyzer) {
        List<Change> changes = new ArrayList<>();
        analyzer.analyzeDifferences(changes::add);
        return changes;
    }
}
