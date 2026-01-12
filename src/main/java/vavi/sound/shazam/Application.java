/*
 * https://github.com/wsieroci/audio-recognizer
 */

package vavi.sound.shazam;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import vavi.sound.shazam.model.Complex;
import vavi.sound.shazam.model.DataPoint;


/** */
public class Application {

    private static final Logger logger = System.getLogger(Application.class.getName());

    public static final int UPPER_LIMIT = 300;
    public static final int LOWER_LIMIT = 40;

    public final int[] RANGE = new int[] {40, 80, 120, 180, UPPER_LIMIT + 1};

    public double[][] highScores;
    public double[][] recordPoints;
    long[][] points;
    Map<Long, List<DataPoint>> hashMap;
    public Map<Integer, Map<Integer, Integer>> matchMap; // Map<SongId, Map<Offset, Count>>

    public Application() {
        this.hashMap = new HashMap<>();
    }

    // Find out in which range
    public int getIndex(int freq) {
        int i = 0;
        while (RANGE[i] < freq)
            i++;
        return i;
    }

    public void determineKeyPoints(Complex[][] results, long songId, boolean isMatching) {
        this.matchMap = new HashMap<>();

        FileWriter fstream = null;
        try {
            fstream = new FileWriter("result.txt");
        } catch (IOException e1) {
            logger.log(Level.ERROR, e1.getMessage(), e1);
        }
        BufferedWriter outFile = new BufferedWriter(fstream);

        highScores = new double[results.length][5];
        for (int i = 0; i < results.length; i++) {
            for (int j = 0; j < 5; j++) {
                highScores[i][j] = 0;
            }
        }

        recordPoints = new double[results.length][UPPER_LIMIT];
        for (int i = 0; i < results.length; i++) {
            for (int j = 0; j < UPPER_LIMIT; j++) {
                recordPoints[i][j] = 0;
            }
        }

        points = new long[results.length][5];
        for (int i = 0; i < results.length; i++) {
            for (int j = 0; j < 5; j++) {
                points[i][j] = 0;
            }
        }

        for (int t = 0; t < results.length; t++) {
            for (int freq = LOWER_LIMIT; freq < UPPER_LIMIT - 1; freq++) {
                // Get the magnitude:
                double mag = Math.log(results[t][freq].abs() + 1);

                // Find out which range we are in:
                int index = getIndex(freq);

                // Save the highest magnitude and corresponding frequency:
                if (mag > highScores[t][index]) {
                    highScores[t][index] = mag;
                    recordPoints[t][freq] = 1;
                    points[t][index] = freq;
                }
            }

            try {
                for (int k = 0; k < 5; k++) {
                    outFile.write(highScores[t][k] + ";" + recordPoints[t][k] + "\t");
                }
                outFile.write("\n");

            } catch (IOException e) {
                logger.log(Level.ERROR, e.getMessage(), e);
            }

            long h = hash(points[t][0], points[t][1], points[t][2], points[t][3]);

            if (isMatching) {
                List<DataPoint> listPoints;

                if ((listPoints = hashMap.get(h)) != null) {
                    for (DataPoint dP : listPoints) {
                        int offset = Math.abs(dP.time() - t);
                        Map<Integer, Integer> tmpMap = null;
                        if ((tmpMap = this.matchMap.get(dP.songId())) == null) {
                            tmpMap = new HashMap<>();
                            tmpMap.put(offset, 1);
                            matchMap.put(dP.songId(), tmpMap);
                        } else {
                            tmpMap.merge(offset, 1, Integer::sum);
                        }
                    }
                }
            } else {
                List<DataPoint> listPoints;
                if ((listPoints = hashMap.get(h)) == null) {
                    listPoints = new ArrayList<>();
                    DataPoint point = new DataPoint((int) songId, t);
                    listPoints.add(point);
                    hashMap.put(h, listPoints);
                } else {
                    DataPoint point = new DataPoint((int) songId, t);
                    listPoints.add(point);
                }
            }
        }
        try {
            outFile.close();
        } catch (IOException e) {
            logger.log(Level.ERROR, e.getMessage(), e);
        }
    }

    private static final int FUZ_FACTOR = 2;

    private static long hash(long p1, long p2, long p3, long p4) {
        return (p4 - (p4 % FUZ_FACTOR)) * 100000000 + (p3 - (p3 % FUZ_FACTOR))
                * 100000 + (p2 - (p2 % FUZ_FACTOR)) * 100
                + (p1 - (p1 % FUZ_FACTOR));
    }
}
