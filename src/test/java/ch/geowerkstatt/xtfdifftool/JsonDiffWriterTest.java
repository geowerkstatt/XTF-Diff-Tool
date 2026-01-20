package ch.geowerkstatt.xtfdifftool;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import ch.geowerkstatt.xtfdifftool.diff.ChangeType;
import ch.geowerkstatt.xtfdifftool.diff.ValueType;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JsonDiffWriterTest {
    @Test
    public void writeNoChanges() throws IOException {
        String json = serializeChanges();
        assertEquals("[]", json);
    }

    @Test
    public void writeSingleAddedObject() throws IOException {
        Change change = new Change("o1", ChangeType.ADDED, ValueType.OBJECT, "ClassName", null, null);
        String json = serializeChanges(change);
        assertEquals("""
                [{"oid":"o1","changeType":"added","valueType":"object","interlisName":"ClassName","oldValue":null,"newValue":null}]""", json);
    }

    @Test
    public void writeAddedAndDeletedObjects() throws IOException {
        String json = serializeChanges(
                new Change("o1", ChangeType.ADDED, ValueType.OBJECT, "ClassName", null, null),
                new Change("o2", ChangeType.DELETED, ValueType.OBJECT, "ClassName", null, null)
        );
        assertEquals("""
                [{"oid":"o1","changeType":"added","valueType":"object","interlisName":"ClassName","oldValue":null,"newValue":null},{"oid":"o2","changeType":"deleted","valueType":"object","interlisName":"ClassName","oldValue":null,"newValue":null}]""", json);
    }

    private String serializeChanges(Change... changes) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (JsonDiffWriter writer = new JsonDiffWriter(outputStream)) {
            for (Change change : changes) {
                writer.writeChange(change);
            }
        }
        return outputStream.toString(StandardCharsets.UTF_8);
    }
}
