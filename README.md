[![CI](https://github.com/geowerkstatt/XTF-Diff-Tool/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/geowerkstatt/XTF-Diff-Tool/actions/workflows/ci.yml)
[![Release](https://github.com/geowerkstatt/XTF-Diff-Tool/actions/workflows/release.yml/badge.svg)](https://github.com/geowerkstatt/XTF-Diff-Tool/actions/workflows/release.yml)
[![Latest Release](https://img.shields.io/github/v/release/geowerkstatt/XTF-Diff-Tool)](https://github.com/geowerkstatt/XTF-Diff-Tool/releases/latest)
[![License](https://img.shields.io/github/license/geowerkstatt/XTF-Diff-Tool)](https://github.com/geowerkstatt/XTF-Diff-Tool/blob/main/LICENSE)

# XTF-Diff-Tool

Das `XTF-Diff-Tool` kann verwendet werden, um die Unterschiede zwischen zwei INTERLIS-XTF-Dateien zu analysieren.

## Anforderungen

Java 25 (LTS) oder neuer wird benötigt, um das `XTF-Diff-Tool` auszuführen.
Die erforderlichen Jar-Abhängigkeiten sind im Distributionspaket des Tools enthalten.

Ein [Docker-Image](https://github.com/geowerkstatt/XTF-Diff-Tool/pkgs/container/XTF-Diff-Tool), das alle notwendigen Abhängigkeiten enthält, steht ebenfalls zum Download bereit.

## Verwendung

Start über JAR:
```shell
java -jar XTF-Diff-Tool.jar [options] <first XTF file> <second XTF file> <diff output file>
```

Start mit Docker:
```shell
docker run -it --rm -v ${PWD}:/host ghcr.io/geowerkstatt/xtf-diff-tool [options] <first XTF file inside volume: /host/**/*.xtf> <second XTF file inside volume: /host/**/*.xtf> <diff output inside volume: /host/**/*.json>
```

### Commandline Optionen
| Option | Beschreibung |
| --- | --- |
| --help | Show help message and exit |
| --version | Show version information and exit |
| --logfile \<file\> | Path to the log file |
| --modeldir \<modeldir\> | INTERLIS model search directories and repositories separated by `;` |
| --proxy \<host\> | Set the proxy server used to access the INTERLIS model repositories |
| --proxyPort \<port\> | Set the proxy port used to access the INTERLIS model repositories |
| --verbose | Enable debug log output |

### Beschreibung der Outputdatei
Der Inhalt und Aufbau der Outputdatei (*.json) ist [hier](OutputFileDescription.md) beschrieben.