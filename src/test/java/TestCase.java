/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.lang.System.Logger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.tritonus.sampled.convert.PCM2PCMConversionProvider;
import vavi.sound.shazam.model.Complex;
import vavi.sound.shazam.model.FFT;
import vavi.sound.shazam.Application;
import vavi.sound.shazam.view.MainView;
import vavi.util.properties.annotation.Property;
import vavi.util.properties.annotation.PropsEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * TestCase for audio comparison.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2026-01-12 nsano initial version <br>
 */
@PropsEntity(url = "file:local.properties")
public class TestCase {

    private static final Logger logger = System.getLogger(TestCase.class.getName());

    static boolean localPropertiesExists() {
        return Files.exists(Paths.get("local.properties"));
    }

    @Property(name = "original")
    String original = "src/test/resources";

    @Property(name = "target")
    String target = "src/test/resources";

    @BeforeEach
    void setup() throws Exception {
        if (localPropertiesExists()) {
            PropsEntity.Util.bind(this);
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void test1() throws Exception {
        MainView.main(new String[] {});

        CountDownLatch cdl = new CountDownLatch(1);
        cdl.await();
    }

    @Test
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    void testCompare() throws Exception {
        Application app = new Application();

        // 1. Index mds1.mp3 (original)
        File mds1File = new File(original);
        assertTrue(mds1File.exists(), "Original file not found: " + mds1File.getAbsolutePath());
        System.out.println("Indexing " + mds1File.getName() + "...");
        byte[] mds1Audio = readAudio(mds1File);
        Complex[][] mds1Results = calculateFFT(mds1Audio);
        app.determineKeyPoints(mds1Results, 0L, false);

        // 2. Match vgm1.mp3 (target)
        File vgm1File = new File(target);
        assertTrue(vgm1File.exists(), "Target file not found: " + vgm1File.getAbsolutePath());
        System.out.println("Matching " + vgm1File.getName() + "...");
        byte[] vgm1Audio = readAudio(vgm1File);
        Complex[][] vgm1Results = calculateFFT(vgm1Audio);
        app.determineKeyPoints(vgm1Results, 0L, true);

        // 3. Check results
        System.out.println("Match results:");
        boolean found = false;
        if (!app.matchMap.isEmpty()) {
            for (Map.Entry<Integer, Map<Integer, Integer>> entry : app.matchMap.entrySet()) {
                int songId = entry.getKey();
                Map<Integer, Integer> offsets = entry.getValue();
                int maxCount = 0;
                int bestOffset = -1;
                for (Map.Entry<Integer, Integer> offsetEntry : offsets.entrySet()) {
                    if (offsetEntry.getValue() > maxCount) {
                        maxCount = offsetEntry.getValue();
                        bestOffset = offsetEntry.getKey();
                    }
                }
                System.out.println("Song ID: " + songId + ", Best Offset: " + bestOffset + ", Max Count: " + maxCount);
                found = true;
            }
        }
        
        if (!found) {
            System.out.println("No matches found.");
        }
    }

    /**
     * Replicates the FFT logic from AudioRecognizerWindow.makeSpectrum.
     */
    private static Complex[][] calculateFFT(byte[] audio) {
        int totalSize = audio.length;
        int amountPossible = totalSize / 4096;
        Complex[][] results = new Complex[amountPossible][];
        for (int times = 0; times < amountPossible; times++) {
            Complex[] complex = new Complex[4096];
            for (int i = 0; i < 4096; i++) {
                complex[i] = new Complex(audio[(times * 4096) + i], 0);
            }
            results[times] = FFT.fft(complex);
        }
        return results;
    }

    /**
     * Replicates the audio reading and conversion logic from AudioRecognizerWindow.
     */
    private static byte[] readAudio(File file) throws Exception {
        AudioInputStream in = AudioSystem.getAudioInputStream(file);
        AudioFormat baseFormat = in.getFormat();

        // Decoded format as used in AudioRecognizerWindow
        AudioFormat decodedFormat = new AudioFormat(
                AudioFormat.Encoding.PCM_SIGNED,
                baseFormat.getSampleRate(), 16, baseFormat.getChannels(),
                baseFormat.getChannels() * 2, baseFormat.getSampleRate(),
                false);

        AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in);

        // Target format from AudioRecognizerWindow.getFormat()
        AudioFormat targetFormat = new AudioFormat(44100, 8, 1, true, true);

        PCM2PCMConversionProvider conversionProvider = new PCM2PCMConversionProvider(); // TODO works?
        AudioInputStream outDin = conversionProvider.getAudioInputStream(targetFormat, din);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int n = 0;
        int count;
        // Limit to 1000 chunks as in the original code's loop
        while ((count = outDin.read(buffer)) != -1 && n < 1000) {
            out.write(buffer, 0, count);
            n++;
        }
        outDin.close();
        return out.toByteArray();
    }
}
