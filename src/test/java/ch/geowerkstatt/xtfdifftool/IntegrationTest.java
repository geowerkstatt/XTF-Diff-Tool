package ch.geowerkstatt.xtfdifftool;

import ch.ehi.basics.settings.Settings;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.config.FileEntry;
import ch.interlis.ili2c.config.FileEntryKind;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iox.EndTransferEvent;
import ch.interlis.iox.IoxEvent;
import ch.interlis.iox.IoxException;
import ch.interlis.iox.IoxLogEvent;
import ch.interlis.iox.IoxLogging;
import ch.interlis.iox.IoxReader;
import ch.interlis.iox_j.IoxIliReader;
import ch.interlis.iox_j.PipelinePool;
import ch.interlis.iox_j.logging.LogEventFactory;
import ch.interlis.iox_j.utility.ReaderFactory;
import ch.interlis.iox_j.validator.ValidationConfig;
import ch.interlis.iox_j.validator.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public final class IntegrationTest {
    private static final String TEST_DIR = "src/test/data/IntegrationTest/";
    private static final String TEST_OUT_DIR = "src/test/data/Results/IntegrationTest/";

    private static final String MODEL_FILE_24 = TEST_DIR + "DiffToolTest_2.4.ili";
    private static final String XTF_FILE_1_24 = TEST_DIR + "DiffToolTest1_2.4.xtf";
    private static final String XTF_FILE_2_24 = TEST_DIR + "DiffToolTest2_2.4.xtf";

    private static final String MODEL_FILE_23 = TEST_DIR + "DiffToolTest_2.3.ili";
    private static final String XTF_FILE_1_23 = TEST_DIR + "DiffToolTest1_2.3.xtf";
    private static final String XTF_FILE_2_23 = TEST_DIR + "DiffToolTest2_2.3.xtf";

    @BeforeAll
    public static void initAll() {
        var _ = new File(TEST_OUT_DIR).mkdirs();

        var files = new File(TEST_OUT_DIR).listFiles();
        if (files != null) {
            for (File f : files) {
                var _ = f.delete();
            }
        }
    }

    @Test
    public void analyzeDifference24() throws IOException, IoxException {
        var transferDescription = compileIli(MODEL_FILE_24);

        // precondition, XTF files are valid
        assertXtfIsValid(transferDescription, new File(XTF_FILE_1_24));
        assertXtfIsValid(transferDescription, new File(XTF_FILE_2_24));

        var output = new File(TEST_OUT_DIR + "Diff_24.json");
        assertFalse(output.exists(), "Precondition: output file should not exist.");

        String[] arguments = {XTF_FILE_1_24, XTF_FILE_2_24, output.toString(), "--modeldir", TEST_DIR};
        Main.main(arguments);

        assertTrue(output.exists());

        var expectedOutput = Files.readString(Path.of(TEST_DIR + "ExpectedDiff_2.4.json"));
        var actualOutput = Files.readString(output.toPath());
        assertEquals(prettyPrintJson(expectedOutput), prettyPrintJson(actualOutput));
    }

    @Test
    public void analyzeDifference23() throws IOException, IoxException {
        var transferDescription = compileIli(MODEL_FILE_23);

        // precondition, XTF files are valid
        assertXtfIsValid(transferDescription, new File(XTF_FILE_1_23));
        assertXtfIsValid(transferDescription, new File(XTF_FILE_2_23));

        var output = new File(TEST_OUT_DIR + "Diff_23.json");
        assertFalse(output.exists(), "Precondition: output file should not exist.");

        String[] arguments = {XTF_FILE_1_23, XTF_FILE_2_23, output.toString(), "--modeldir", TEST_DIR};
        Main.main(arguments);

        assertTrue(output.exists());

        var expectedOutput = Files.readString(Path.of(TEST_DIR + "ExpectedDiff_2.3.json"));
        var actualOutput = Files.readString(output.toPath());
        assertEquals(prettyPrintJson(expectedOutput), prettyPrintJson(actualOutput));
    }

    private static TransferDescription compileIli(String iliFile) {
        Configuration config = new Configuration();
        config.addFileEntry(new FileEntry(iliFile, FileEntryKind.ILIMODELFILE));
        TransferDescription transferDescription = ch.interlis.ili2c.Main.runCompiler(config);
        assertNotNull(transferDescription);
        return transferDescription;
    }

    private static void assertXtfIsValid(TransferDescription transferDescription, File xtfFile) throws IoxException {
        final ArrayList<String> errors = new ArrayList<>();
        var logger = new IoxLogging() {
            @Override
            public void addEvent(IoxLogEvent ioxLogEvent) {
                System.out.println(ioxLogEvent.getEventMsg());
                if (ioxLogEvent.getEventKind() == IoxLogEvent.ERROR) {
                    errors.add(ioxLogEvent.getEventMsg() + " (" + ioxLogEvent.getSourceObjectTag() + "; Line:" + ioxLogEvent.getSourceLineNr() + "; TID:" + ioxLogEvent.getSourceObjectXtfId() + ")");
                }
            }
        };
        var config = new ValidationConfig();
        var logEventFactory = new LogEventFactory();
        var settings = new Settings();
        var pipelinePool = new PipelinePool();
        Validator validator = null;
        try {
            validator = new Validator(transferDescription, config, logger, logEventFactory, pipelinePool, settings);

            var readerFactory = new ReaderFactory();
            IoxReader reader = null;
            try {
                reader = readerFactory.createReader(xtfFile, logEventFactory, settings);
                ((IoxIliReader) reader).setModel(transferDescription);
                IoxEvent event = reader.read();
                while (!(event instanceof EndTransferEvent)) {
                    validator.validate(event);
                    event = reader.read();
                }
            } finally {
                if (reader != null) {
                    reader.close();
                }
            }
        } finally {
            if (validator != null) {
                validator.close();
            }
        }

        if (!errors.isEmpty()) {
            var errorList = errors.stream()
                    .map(e -> System.lineSeparator() + "> " + e)
                    .collect(Collectors.joining());
            fail("XTF file <" + xtfFile.getName() + "> is not valid!" + errorList);
        }
    }

    private static String prettyPrintJson(String input) {
        var mapper = new ObjectMapper();
        Object json = mapper.readValue(input, Object.class);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    }
}
