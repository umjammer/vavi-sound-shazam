/*
 * https://github.com/wsieroci/audio-recognizer
 */

package vavi.sound.shazam.view;

import java.awt.Button;
import java.awt.FlowLayout;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.URL;
import java.util.List;
import java.util.Map;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFrame;
import javax.swing.JTextField;

import org.tritonus.sampled.convert.PCM2PCMConversionProvider;
import vavi.sound.shazam.Application;
import vavi.sound.shazam.model.Complex;
import vavi.sound.shazam.model.DataPoint;
import vavi.sound.shazam.model.FFT;


public class AudioRecognizerWindow extends JFrame {

    private static final Logger logger = System.getLogger(AudioRecognizerWindow.class.getName());

    boolean running = false;
    long nrSong = 0;
    JTextField fileTextField = null;
    Application app;

    private AudioFormat getFormat() {
        float sampleRate = 44100;
        int sampleSizeInBits = 8;
        int channels = 1; // mono
        boolean signed = true;
        boolean bigEndian = true;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    private synchronized SourceDataLine getLine(AudioFormat audioFormat) throws LineUnavailableException {
        SourceDataLine res;
        DataLine.Info info = new DataLine.Info(SourceDataLine.class, audioFormat);
        res = (SourceDataLine) AudioSystem.getLine(info);
        res.open(audioFormat);
        return res;
    }

    private synchronized void rawplay(AudioFormat targetFormat, AudioInputStream din)
            throws IOException, LineUnavailableException {
        byte[] data = new byte[4096];
        SourceDataLine line = getLine(targetFormat);
        // Start
        line.start();
        int nBytesRead = 0, nBytesWritten = 0;
        while (nBytesRead != -1) {
            nBytesRead = din.read(data, 0, data.length);
            if (nBytesRead != -1) {
                nBytesWritten = line.write(data, 0, nBytesRead);
            }
        }
        // Stop
        line.drain();
        line.stop();
        line.close();
        din.close();
    }

    private void listenSound(long songId, boolean isMatching)
            throws LineUnavailableException, IOException, UnsupportedAudioFileException {

        AudioFormat formatTmp;
        TargetDataLine lineTmp;
        String filePath = fileTextField.getText();
        AudioInputStream din;
        AudioInputStream outDin = null;
        PCM2PCMConversionProvider conversionProvider = new PCM2PCMConversionProvider();
        boolean isMicrophone = false;

        if (filePath == null || filePath.isEmpty() || isMatching) {

            formatTmp = getFormat(); // Fill AudioFormat with the wanted settings
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, formatTmp);
            lineTmp = (TargetDataLine) AudioSystem.getLine(info);
            isMicrophone = true;
        } else {
            AudioInputStream in;

            if (filePath.contains("http")) {
                URL url = new URL(filePath);
                in = AudioSystem.getAudioInputStream(url);
            } else {
                File file = new File(filePath);
                in = AudioSystem.getAudioInputStream(file);
            }

            AudioFormat baseFormat = in.getFormat();

            System.out.println(baseFormat.toString());

            AudioFormat decodedFormat = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    baseFormat.getSampleRate(), 16, baseFormat.getChannels(),
                    baseFormat.getChannels() * 2, baseFormat.getSampleRate(),
                    false);

            din = AudioSystem.getAudioInputStream(decodedFormat, in);

            if (!conversionProvider.isConversionSupported(getFormat(), decodedFormat)) {
                System.out.println("Conversion is not supported");
            }

            System.out.println(decodedFormat);

            outDin = conversionProvider.getAudioInputStream(getFormat(), din);
            formatTmp = decodedFormat;

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, formatTmp);
            lineTmp = (TargetDataLine) AudioSystem.getLine(info);
        }

        AudioFormat format = formatTmp;
        TargetDataLine line = lineTmp;
        boolean isMicro = isMicrophone;
        AudioInputStream outDinSound = outDin;

        if (isMicro) {
            try {
                line.open(format);
                line.start();
            } catch (LineUnavailableException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }
        }

        long sId = songId;
        boolean isMatch = isMatching;

        Thread listeningThread = new Thread(() -> {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            running = true;
            int n = 0;
            byte[] buffer = new byte[(int) 1024];

            try {
                while (running) {
                    n++;
                    if (n > 1000)
                        break;

                    int count = 0;
                    if (isMicro) {
                        count = line.read(buffer, 0, 1024);
                    } else {
                        count = outDinSound.read(buffer, 0, 1024);
                    }
                    if (count > 0) {
                        out.write(buffer, 0, count);
                    }
                }

                byte[] b = out.toByteArray();
                for (byte item : b) {
                    System.out.println(item);
                }

                try {
                    makeSpectrum(out, sId, isMatch);

                    FileWriter fstream = new FileWriter("out.txt");
                    BufferedWriter outFile = new BufferedWriter(fstream);

                    byte[] bytes = out.toByteArray();
                    for (byte value : b) {
                        outFile.write(value + ";");
                    }
                    outFile.close();

                } catch (Exception e) {
                    System.err.println("Error: " + e.getMessage());
                }

                out.close();
                line.close();
            } catch (IOException e) {
                System.err.println("I/O problems: " + e);
                System.exit(-1);
            }
        });

        listeningThread.start();
    }

    void makeSpectrum(ByteArrayOutputStream out, long songId, boolean isMatching) {
        byte[] audio = out.toByteArray();

        int totalSize = audio.length;

        int amountPossible = totalSize / 4096;

        // When turning into frequency domain we'll need complex numbers:
        Complex[][] results = new Complex[amountPossible][];

        // For all the chunks:
        for (int times = 0; times < amountPossible; times++) {
            Complex[] complex = new Complex[4096];
            for (int i = 0; i < 4096; i++) {
                // Put the time domain data into a complex number with imaginary part as 0:
                complex[i] = new Complex(audio[(times * 4096) + i], 0);
            }
            // Perform FFT analysis on the chunk:
            results[times] = FFT.fft(complex);
        }
        app.determineKeyPoints(results, songId, isMatching);
        JFrame spectrumView = new SpectrumView(results, 4096, app.highScores, app.recordPoints);
        spectrumView.setVisible(true);
    }

    AudioRecognizerWindow(String windowName) {
        super(windowName);
    }

    public void createWindow() {
        app = new Application();
        this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Button buttonStart = new Button("Start");
        Button buttonStop = new Button("Stop");
        Button buttonMatch = new Button("Match");
        Button buttonStartMatch = new Button("Start Match");
        Button buttonStopMatch = new Button("Stop Match");
        fileTextField = new JTextField(20);

        fileTextField.setText("/home/wiktor/audio/billy.mp3");

        buttonStart.addActionListener(e -> {
            try {
                try {
                    listenSound(nrSong, false);
                } catch (IOException | UnsupportedAudioFileException e1) {
                    logger.log(Level.ERROR, e1.getMessage(), e1);
                }
                nrSong++;
            } catch (LineUnavailableException ex) {
                logger.log(Level.ERROR, ex.getMessage(), ex);
            }
        });

        buttonStop.addActionListener(e -> running = false);

        buttonStartMatch.addActionListener(e -> {
            try {
                try {
                    listenSound(nrSong, true);
                } catch (IOException | UnsupportedAudioFileException e1) {
                    logger.log(Level.ERROR, e1.getMessage(), e1);
                }
            } catch (LineUnavailableException ex) {
                logger.log(Level.ERROR, ex.getMessage(), ex);
            }
        });

        buttonStopMatch.addActionListener(e -> running = false);

        buttonMatch.addActionListener(e -> {
            List<DataPoint> listPoints;
            int bestCount = 0;
            int bestSong = -1;

            for (int id = 0; id < nrSong; id++) {

                System.out.println("For song id: " + id);
                Map<Integer, Integer> tmpMap = app.matchMap.get(id);
                int bestCountForSong = 0;

                for (Map.Entry<Integer, Integer> entry : tmpMap.entrySet()) {
                    if (entry.getValue() > bestCountForSong) {
                        bestCountForSong = entry.getValue();
                    }
                    System.out.println("Time offset = " + entry.getKey() + ", Count = " + entry.getValue());
                }

                if (bestCountForSong > bestCount) {
                    bestCount = bestCountForSong;
                    bestSong = id;
                }
            }

            System.out.println("Best song id: " + bestSong);
        });

        this.add(buttonStart);
        this.add(buttonStop);
        this.add(buttonStartMatch);
        this.add(buttonStopMatch);
        this.add(buttonMatch);
        this.add(fileTextField);
        this.setLayout(new FlowLayout());
        this.setSize(300, 100);
        this.setVisible(true);
    }
}
