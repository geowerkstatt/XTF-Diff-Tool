package ch.geowerkstatt.xtfdifftool;

import ch.geowerkstatt.xtfdifftool.diff.Change;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.json.JsonMapper;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public final class JsonDiffWriter implements Closeable {
    private static final JsonMapper JSON_MAPPER = JsonMapper.builder().build();
    private final OutputStream outputStream;
    private final JsonGenerator jsonGenerator;

    /**
     * Creates a new JsonDiffWriter that writes to the given output stream.
     */
    public JsonDiffWriter(OutputStream outputStream) {
        this.outputStream = outputStream;
        this.jsonGenerator = JSON_MAPPER.createGenerator(outputStream, JsonEncoding.UTF8);
        this.jsonGenerator.writeStartArray();
    }

    /**
     * Writes a change to the output stream in JSON format.
     */
    public void writeChange(Change change) {
        jsonGenerator.writePOJO(change);
    }

    @Override
    public void close() throws IOException {
        jsonGenerator.writeEndArray();
        jsonGenerator.close();
        outputStream.close();
    }
}
