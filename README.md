[![Release](https://jitpack.io/v/umjammer/vavi-sound-shazam.svg)](https://jitpack.io/#umjammer/vavi-sound-shazam)
[![Java CI](https://github.com/umjammer/vavi-sound-shazam/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-sound-shazam/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-sound-shazam/actions/workflows/codeql.yml/badge.svg)](https://github.com/umjammer/vavi-sound-shazam/actions/workflows/codeql.yml)
![Java](https://img.shields.io/badge/Java-17-b07219)

# vavi-sound-shazam

 👂️mavenized and library friendly sound detector.

## Install

 - [maven](https://jitpack.io/#umjammer/vavi-sound-shazam)

## Usage

 - [sample](src/test/java/TestCase.java)

## References

 - [original](https://github.com/wsieroci/audio-recognizer)

## TODO

---

## [Original](https://github.com/wsieroci/audio-recognizer)

Roy van Rijn has written wonderful post about Shazam algorithm and how to implement it on our own. To do this he placed many chunks of his project source code, but he did not upload all source code of his application because as he stated:

The Shazam patent holders lawyers are sending me emails to stop me from releasing the code and removing this blogpost.

It occurred that core of this algorithm is very simple. I have analyzed his post and as weekend project I have written simple Proof-Of-Concept application which outputs its findings to console. It gives surprisingly correct answers. For now I have tested 10 different mp3 audio files and this application was able to recognize each of them. Application is learning basing on path to mp3 file on your local disk or http stream of mp3 file from any source and recognize by sound from microphone. 
