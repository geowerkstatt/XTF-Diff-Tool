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
java -jar XTF-Diff-Tool.jar <first XTF file> <second XTF file> <diff output file>
```

Starting with Docker:
```shell
docker run -it --rm -v ${PWD}:/host ghcr.io/geowerkstatt/XTF-Diff-Tool <first XTF file inside volume: /host/**/*.xtf> <second XTF file inside volume: /host/**/*.xtf> <diff output inside volume: /host/**/*.json>
```
