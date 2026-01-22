package ch.geowerkstatt.xtfdifftool;

import ch.interlis.ili2c.metamodel.TransferDescription;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.help.HelpFormatter;
import org.apache.commons.cli.help.TextHelpAppendable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

            applyGlobalOptions(options.get());
            process(options.get());
        }
    }

    private static void process(XtfDiffToolOptions options) {
        try {
            ModelReader modelReader = new ModelReader();
            TransferDescription transfer = modelReader.validateAndCompileIli(options);

            try (
                    XtfStreamReader firstReader = new XtfStreamReader(Path.of(options.firstXtfFile()).toFile());
                    XtfStreamReader secondReader = new XtfStreamReader(Path.of(options.secondXtfFile()).toFile());
                    JsonDiffWriter diffWriter = new JsonDiffWriter(Files.newOutputStream(Path.of(options.diffOutputFile())))
            ) {
                XtfAnalyzer xtfAnalyzer = new XtfAnalyzer(transfer, firstReader.readObjects(), secondReader.readObjects());
                xtfAnalyzer.analyzeDifferences(diffWriter::writeChange);
            }
        } catch (Exception e) {
            System.err.println("Error processing XTF files: " + e.getMessage());
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

    private static void applyGlobalOptions(XtfDiffToolOptions options) {
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
