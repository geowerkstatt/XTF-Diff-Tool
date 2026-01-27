package ch.geowerkstatt.xtfdifftool;

import ch.ehi.basics.logging.EhiLogger;
import ch.ehi.basics.logging.StdListener;
import ch.interlis.ili2c.metamodel.TransferDescription;
import ch.interlis.iox_j.utility.IoxUtility;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.cli.help.TextHelpAppendable;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.appender.FileAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public final class Main {
    private static final String OPTION_HELP = "help";
    private static final String OPTION_LOGFILE = "logfile";
    private static final String OPTION_MODEL_DIR = "modeldir";
    private static final String OPTION_PROXY = "proxy";
    private static final String OPTION_PROXY_PORT = "proxyPort";
    private static final String OPTION_VERSION = "version";
    private static final String VERSION;
    private static final Logger LOGGER = LogManager.getLogger();

    static {
        String packageVersion = Main.class.getPackage().getImplementationVersion();
        VERSION = packageVersion != null ? packageVersion : "unknown";
    }

    private Main() { }

    /**
     * Application entry point.
     * @param args Command line arguments.
     */
    static void main(String[] args) {
        Options cliOptions = createCliOptions();
        CommandLine commandLine = parseCommandLine(cliOptions, args);

        if (commandLine == null) {
            System.exit(1);
        } else if (commandLine.hasOption(OPTION_HELP)) {
            printUsage(cliOptions);
        } else if (commandLine.hasOption(OPTION_VERSION)) {
            System.out.println(VERSION);
        } else {
            Optional<XtfDiffToolOptions> options = parseXtfDiffToolOptions(commandLine);
            if (options.isEmpty()) {
                printUsage(cliOptions);
                System.exit(1);
            }

            configureProxy(options.get());
            configureLogging(options.get());

            Instant start = Instant.now();
            process(options.get());
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            String formattedDuration = String.format("%d.%03ds", duration.toSeconds(), duration.toMillisPart());
            LOGGER.info("Processing took {}", formattedDuration);
        }
    }

    private static void process(XtfDiffToolOptions options) {
        int[] changeCount = {0};
        try {
            Path firstXtfPath = Path.of(options.firstXtfFile());
            Path secondXtfPath = Path.of(options.secondXtfFile());
            TransferDescription transferDescription = ModelReader.validateAndCompileIli(firstXtfPath, secondXtfPath, options.modelDir());

            try (
                    XtfStreamReader firstReader = new XtfStreamReader(firstXtfPath.toFile());
                    XtfStreamReader secondReader = new XtfStreamReader(secondXtfPath.toFile());
                    JsonDiffWriter diffWriter = new JsonDiffWriter(Files.newOutputStream(Path.of(options.diffOutputFile())))
            ) {
                ObjectAnalyzer objectAnalyzer = new ObjectAnalyzer(transferDescription, firstReader.readObjects(), secondReader.readObjects());
                objectAnalyzer.analyzeDifferences(change -> {
                    diffWriter.writeChange(change);
                    changeCount[0]++;
                });
                LOGGER.info("Total changes found: {}", changeCount[0]);
            }
        } catch (Exception e) {
            LOGGER.error("Error processing XTF files", e);
            System.exit(1);
        }
    }

    private static CommandLine parseCommandLine(Options options, String[] args) {
        try {
            DefaultParser parser = new DefaultParser();
            return parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println("Error parsing command line arguments: " + e.getMessage());
            printUsage(options);
            return null;
        }
    }

    private static void printUsage(Options options) {
        TextHelpAppendable appendable = new TextHelpAppendable(System.out);
        appendable.setMaxWidth(100);
        HelpFormatter formatter = HelpFormatter.builder()
                .setShowSince(false)
                .setHelpAppendable(appendable)
                .get();
        try {
            formatter.printHelp("java -jar XTF-Diff-Tool.jar [options] first.xtf second.xtf diff.json", "XTF-Diff-Tool version " + VERSION, options, "", false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Optional<XtfDiffToolOptions> parseXtfDiffToolOptions(CommandLine commandLine) {
        List<String> remainingArgs = commandLine.getArgList();
        if (remainingArgs.size() != 3) {
            return Optional.empty();
        }

        return Optional.of(new XtfDiffToolOptions(
                remainingArgs.getFirst(),
                remainingArgs.get(1),
                remainingArgs.get(2),
                Optional.ofNullable(commandLine.getOptionValue(OPTION_LOGFILE)),
                Optional.ofNullable(commandLine.getOptionValue(OPTION_MODEL_DIR)),
                Optional.ofNullable(commandLine.getOptionValue(OPTION_PROXY)),
                Optional.ofNullable(commandLine.getOptionValue(OPTION_PROXY_PORT))
        ));
    }

    private static void configureProxy(XtfDiffToolOptions options) {
        if (options.proxyHost().isPresent()) {
            System.setProperty("http.proxyHost", options.proxyHost().get());
            System.setProperty("https.proxyHost", options.proxyHost().get());

            if (options.proxyPort().isPresent()) {
                System.setProperty("http.proxyPort", options.proxyPort().get());
                System.setProperty("https.proxyPort", options.proxyPort().get());
            }
        } else {
            System.setProperty("java.net.useSystemProxies", "true");
        }
    }

    private static void configureLogging(XtfDiffToolOptions options) {
        Configurator.setRootLevel(Level.INFO);
        if (options.logfile().isPresent()) {
            var layout = PatternLayout.newBuilder()
                    .withPattern("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n")
                    .build();
            var fileAppender = FileAppender.newBuilder()
                    .setName("Logfile")
                    .setLayout(layout)
                    .withFileName(options.logfile().get())
                    .withAppend(false)
                    .build();
            var rootLogger = (org.apache.logging.log4j.core.Logger) LogManager.getRootLogger();
            rootLogger.get().addAppender(fileAppender, Level.INFO, null);
            fileAppender.start();
        }

        EhiLogger.getInstance().addListener(new EhiLogAdapter());
        EhiLogger.getInstance().removeListener(StdListener.getInstance());

        LOGGER.info("XTF-Diff-Tool version {}", VERSION);
        LOGGER.info("ili2c version {}", TransferDescription.getVersion());
        LOGGER.info("iox-ili version {}", IoxUtility.getVersion());
        LOGGER.info("Transfer files: {}, {}", options.firstXtfFile(), options.secondXtfFile());
        LOGGER.info("Diff output file: {}", options.diffOutputFile());
    }

    private static Options createCliOptions() {
        Options options = new Options();

        options.addOption(Option.builder("h")
                .longOpt(OPTION_HELP)
                .desc("print this help message")
                .get());
        options.addOption(Option.builder()
                .longOpt(OPTION_LOGFILE)
                .desc("path to the log file")
                .argName("file")
                .hasArg()
                .get());
        options.addOption(Option.builder()
                .longOpt(OPTION_MODEL_DIR)
                .desc("INTERLIS model search paths")
                .argName("modeldir")
                .hasArg()
                .get());
        options.addOption(Option.builder()
                .longOpt(OPTION_PROXY)
                .desc("set the proxy server to access the INTERLIS model repositories")
                .argName("host")
                .hasArg()
                .get());
        options.addOption(Option.builder()
                .longOpt(OPTION_PROXY_PORT)
                .desc("set the proxy port to access the INTERLIS model repositories")
                .argName("port")
                .hasArg()
                .get());
        options.addOption(Option.builder()
                .longOpt(OPTION_VERSION)
                .desc("print the version of this application")
                .get());

        return options;
    }
}
