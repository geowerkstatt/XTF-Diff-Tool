package ch.geowerkstatt.xtfdifftool;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.settings.Settings;
import ch.interlis.ili2c.Ili2c;
import ch.interlis.ili2c.Ili2cException;
import ch.interlis.ili2c.config.Configuration;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.ilirepository.IliManager;
import ch.interlis.iox.IoxException;
import ch.interlis.iox_j.logging.LogEventFactory;
import ch.interlis.iox_j.utility.IoxUtility;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public final class ModelReader {
    private static final LogEventFactory ERROR_FACTORY = new LogEventFactory();
    private static final Settings SETTINGS = new Settings();

    private ModelReader() { }

    /**
     * Validates the INTERLIS versions and models of the two XTF files and compiles the corresponding INTERLIS models.
     * @param options The XTF diff tool options.
     * @return The compiled INTERLIS models.
     */
    public static TransferDescription validateAndCompileIli(XtfDiffToolOptions options) {
        Path firstPath = Path.of(options.firstXtfFile());
        Path secondPath = Path.of(options.secondXtfFile());

        double version = validateVersion(firstPath, secondPath);
        List<String> models = validateModels(firstPath, secondPath);
        return compileIli(options.modelDir().orElse("https://models.interlis.ch/"), models, version);
    }

    private static double validateVersion(Path firstPath, Path secondPath) {
        try {
            String firstVersion = IoxUtility.getModelVersion(new String[]{firstPath.toString()}, ERROR_FACTORY, SETTINGS);
            String secondVersion = IoxUtility.getModelVersion(new String[]{secondPath.toString()}, ERROR_FACTORY, SETTINGS);

            if (!firstVersion.equals(secondVersion)) {
                throw new IllegalStateException("XTF files use different INTERLIS versions");
            }
            return Double.parseDouble(firstVersion);
        } catch (IoxException ex) {
            throw new RuntimeException("Failed to get INTERLIS version from XTF file", ex);
        }
    }

    private static List<String> validateModels(Path firstPath, Path secondPath) {
        try {
            List<String> firstModels = IoxUtility.getModels(firstPath.toFile(), ERROR_FACTORY, SETTINGS);
            List<String> secondModels = IoxUtility.getModels(secondPath.toFile(), ERROR_FACTORY, SETTINGS);

            if (!new HashSet<>(firstModels).equals(new HashSet<>(secondModels))) {
                throw new IllegalStateException("XTF files use different INTERLIS models");
            }
            return firstModels;
        } catch (IoxException ex) {
            throw new RuntimeException("Failed to read models of XTF files", ex);
        }
    }

    private static TransferDescription compileIli(String modelDir, List<String> modelNames, double version) {
        IliManager modelManager = new IliManager();
        modelManager.setRepositories(modelDir.split(";"));
        Configuration ili2cConfig;
        try {
            ili2cConfig = modelManager.getConfig(new ArrayList<>(modelNames), version);
        } catch (Ili2cException ex) {
            EhiLogger.logError(ex);
            return null;
        }

        Ili2c.logIliFiles(ili2cConfig);
        return ch.interlis.ili2c.Main.runCompiler(ili2cConfig);
    }
}
