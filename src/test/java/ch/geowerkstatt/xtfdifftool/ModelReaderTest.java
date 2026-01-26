package ch.geowerkstatt.xtfdifftool;

import ch.interlis.ili2c.metamodel.TransferDescription;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class ModelReaderTest {
    private static final String MODEL_DIR = "src/test/data/ModelReaderTest/";

    @Test
    public void validateAndCompileIliSameModel() {
        XtfDiffToolOptions options = createOptions(MODEL_DIR + "DataA1.xtf", MODEL_DIR + "DataA2.xtf");
        TransferDescription td = ModelReader.validateAndCompileIli(options);
        assertNotNull(td);
    }

    @Test
    public void validateAndCompileIliSameModelV23() {
        XtfDiffToolOptions options = createOptions(MODEL_DIR + "DataA1_v23.xtf", MODEL_DIR + "DataA1_v23.xtf");
        TransferDescription td = ModelReader.validateAndCompileIli(options);
        assertNotNull(td);
    }

    @Test
    public void validateAndCompileIliDifferentModelOrder() {
        XtfDiffToolOptions options = createOptions(MODEL_DIR + "DataB1.xtf", MODEL_DIR + "DataB2.xtf");
        TransferDescription td = ModelReader.validateAndCompileIli(options);
        assertNotNull(td);
    }

    @Test
    public void validateAndCompileIliDifferentModel() {
        XtfDiffToolOptions options = createOptions(MODEL_DIR + "DataA1.xtf", MODEL_DIR + "DataB1.xtf");
        IllegalStateException exception = assertThrowsExactly(IllegalStateException.class, () -> ModelReader.validateAndCompileIli(options));
        assertEquals("XTF files use different INTERLIS models", exception.getMessage());
    }

    @Test
    public void validateAndCompileIliDifferentVersion() {
        XtfDiffToolOptions options = createOptions(MODEL_DIR + "DataA1_v23.xtf", MODEL_DIR + "DataA1.xtf");
        IllegalStateException exception = assertThrowsExactly(IllegalStateException.class, () -> ModelReader.validateAndCompileIli(options));
        assertEquals("XTF files use different INTERLIS versions", exception.getMessage());
    }

    private static XtfDiffToolOptions createOptions(String firstXtf, String secondXtf) {
        return new XtfDiffToolOptions(
                firstXtf,
                secondXtf,
                "diffOutput.json",
                Optional.empty(),
                Optional.of(MODEL_DIR),
                Optional.empty(),
                Optional.empty()
        );
    }
}
