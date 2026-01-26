package ch.geowerkstatt.xtfdifftool;

import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.config.FileEntry;
import ch.interlis.ili2c.config.FileEntryKind;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iom.IomObject;
import ch.interlis.iom_j.Iom_jObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class ModelValidatorTest {
    private static final String MODEL_FILE = "src/test/data/ModelValidatorTest/Model.ili";
    private ModelValidator validator;

    @BeforeEach
    public void setUp() {
        Configuration config = new Configuration();
        config.addFileEntry(new FileEntry(MODEL_FILE, FileEntryKind.ILIMODELFILE));
        TransferDescription transferDescription = ch.interlis.ili2c.Main.runCompiler(config);
        validator = new ModelValidator(transferDescription);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Model.TopicMissingOid.ClassOid",
            "Model.TopicMissingOid.AssocOid",
            "Model.TopicOid.ClassOid",
            "Model.TopicOid.ClassMissingOid",
            "Model.TopicOidExtended.ClassMissingOidExtended",
            "Model.TopicOidExtended.ClassOidExtended",
    })
    public void validateClassWithOid(String className) {
        assertTrue(validateObjectHasStableOid(className));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Model.TopicMissingOid.ClassMissingOid",
            "Model.TopicMissingOid.ClassNoOid",
            "Model.TopicOid.ClassNoOid",
            "Model.TopicOid.AssocMissingOid",
            "Model.TopicOidExtended.ClassNoOidExtended",
    })
    public void validateClassWithoutOid(String className) {
        assertFalse(validateObjectHasStableOid(className));
    }

    private boolean validateObjectHasStableOid(String className) {
        IomObject object = new Iom_jObject(className, "o1");
        return validator.validateObjectHasStableOid(object);
    }
}
