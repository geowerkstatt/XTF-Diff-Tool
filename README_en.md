[![CI](https://github.com/geowerkstatt/XTF-Diff-Tool/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/geowerkstatt/XTF-Diff-Tool/actions/workflows/ci.yml)
[![Release](https://github.com/geowerkstatt/XTF-Diff-Tool/actions/workflows/release.yml/badge.svg)](https://github.com/geowerkstatt/XTF-Diff-Tool/actions/workflows/release.yml)
[![Latest Release](https://img.shields.io/github/v/release/geowerkstatt/XTF-Diff-Tool)](https://github.com/geowerkstatt/XTF-Diff-Tool/releases/latest)
[![License](https://img.shields.io/github/license/geowerkstatt/XTF-Diff-Tool)](https://github.com/geowerkstatt/XTF-Diff-Tool/blob/main/LICENSE)

# XTF-Diff-Tool

The `XTF-Diff-Tool` can be used to analyze the differences of two INTERLIS XTF files.

## Requirements

Java 25 (LTS) or later is required to run `XTF-Diff-Tool`.
Required Jar dependencies are bundled with the distribution of the tool.

A [docker image](https://github.com/geowerkstatt/XTF-Diff-Tool/pkgs/container/XTF-Diff-Tool) containing all necessary dependencies is also available for download.

## Usage

Starting from JAR:
```shell
java -jar XTF-Diff-Tool.jar [options] <first XTF file> <second XTF file> <diff output file>
```

Starting with Docker:
```shell
docker run -it --rm -v ${PWD}:/host ghcr.io/geowerkstatt/xtf-diff-tool [options] <first XTF file inside volume: /host/**/*.xtf> <second XTF file inside volume: /host/**/*.xtf> <diff output inside volume: /host/**/*.json>
```

### Commandline Options
| Option | Description |
| --- | --- |
| --help | Show help message and exit |
| --version | Show version information and exit |
| --logfile \<file\> | Path to the log file |
| --modeldir \<modeldir\> | INTERLIS model search directories and repositories separated by `;` |
| --proxy \<host\> | Set the proxy server used to access the INTERLIS model repositories |
| --proxyPort \<port\> | Set the proxy port used to access the INTERLIS model repositories |
| --verbose | Enable debug log output |

### Output file description
The content and structure of the output file (*.json) is described [here](OutputFileDescription.md).
