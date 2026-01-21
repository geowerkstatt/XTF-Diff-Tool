package ch.geowerkstatt.xtfdifftool;

import java.util.Optional;

public record XtfDiffToolOptions(
        String firstXtfFile,
        String secondXtfFile,
        String diffOutputFile,
        Optional<String> logfile,
        Optional<String> modelDir,
        Optional<String> proxyHost,
        Optional<String> proxyPort
) {
}
