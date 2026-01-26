package ch.geowerkstatt.xtfdifftool;

import ch.interlis.ili2c.metamodel.TransferDescription;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

public class ModelReaderTest {
    private static final String MODEL_DIR = "src/test/data/ModelReaderTest/";

    @Test
    public void validateAndCompileIliSameModel() {
        TransferDescription td = validateAndCompileIli("DataA1.xtf", "DataA2.xtf");
        assertNotNull(td);
    }

    @Test
    public void validateAndCompileIliSameModelV23() {
        TransferDescription td = validateAndCompileIli("DataA1_v23.xtf", "DataA1_v23.xtf");
        assertNotNull(td);
    }

    @Test
    public void validateAndCompileIliDifferentModelOrder() {
        TransferDescription td = validateAndCompileIli("DataB1.xtf", "DataB2.xtf");
        assertNotNull(td);
    }

    @Test
    public void validateAndCompileIliDifferentModel() {
        IllegalStateException exception = assertThrowsExactly(IllegalStateException.class, () -> validateAndCompileIli("DataA1.xtf", "DataB1.xtf"));
        assertEquals("XTF files use different INTERLIS models", exception.getMessage());
    }

    @Test
    public void validateAndCompileIliDifferentVersion() {
        IllegalStateException exception = assertThrowsExactly(IllegalStateException.class, () -> validateAndCompileIli("DataA1_v23.xtf", "DataA1.xtf"));
        assertEquals("XTF files use different INTERLIS versions", exception.getMessage());
    }

    private static TransferDescription validateAndCompileIli(String firstFileName, String secondFileName) {
        Path modelDirPath = Path.of(MODEL_DIR);
        return ModelReader.validateAndCompileIli(
                modelDirPath.resolve(firstFileName),
                modelDirPath.resolve(secondFileName),
                Optional.of(MODEL_DIR)
        );
    }
}
