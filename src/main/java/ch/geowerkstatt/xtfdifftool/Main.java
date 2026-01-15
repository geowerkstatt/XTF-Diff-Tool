package ch.geowerkstatt.xtfdifftool;

import java.nio.file.Path;

public final class Main {
    private Main() { }

    /**
     * Application entry point.
     * @param args Command line arguments.
     */
    static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: XTF-Diff-Tool <first XTF file> <second XTF file> <diff output file>");
            System.exit(1);
        }

        process(args[0], args[1], args[2]);
    }

    private static void process(String firstXtfFile, String secondXtfFile, String diffOutputFile) {
        try (
                XtfStreamReader firstReader = new XtfStreamReader(Path.of(firstXtfFile).toFile());
                XtfStreamReader secondReader = new XtfStreamReader(Path.of(secondXtfFile).toFile())
        ) {
            XtfAnalyzer xtfAnalyzer = new XtfAnalyzer(firstReader.readObjects(), secondReader.readObjects());
            xtfAnalyzer.analyzeDifferences(System.out::println);
        } catch (Exception e) {
            System.err.println("Error processing XTF files: " + e.getMessage());
            System.exit(1);
        }
    }
}
