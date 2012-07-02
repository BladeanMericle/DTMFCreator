package org.mericle;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * DTMF生成ツール.
 * @author BladeanMericle
 */
public final class DtmfCreator {

    /** DTMFの周波数テーブル. */
    private static final Hashtable<String, double[]> FREQUENCIES =
        new Hashtable<String, double[]>() { {
            put("1", new double[]{697.0, 1209.0});
            put("2", new double[]{697.0, 1336.0});
            put("3", new double[]{697.0, 1477.0});
            put("A", new double[]{697.0, 1633.0});
            put("4", new double[]{770.0, 1209.0});
            put("5", new double[]{770.0, 1336.0});
            put("6", new double[]{770.0, 1477.0});
            put("B", new double[]{770.0, 1633.0});
            put("7", new double[]{852.0, 1209.0});
            put("8", new double[]{852.0, 1336.0});
            put("9", new double[]{852.0, 1477.0});
            put("C", new double[]{852.0, 1633.0});
            put("*", new double[]{941.0, 1209.0});
            put("0", new double[]{941.0, 1336.0});
            put("#", new double[]{941.0, 1477.0});
            put("D", new double[]{941.0, 1633.0});
        } };

    /** 音声エンコードテーブル. */
    private static final Hashtable<String, AudioFormat.Encoding> ENCODINGS =
        new Hashtable<String, AudioFormat.Encoding>() { {
            put("ALAW", AudioFormat.Encoding.ALAW);
            put("PCM_SIGNED", AudioFormat.Encoding.PCM_SIGNED);
            put("PCM_UNSIGNED", AudioFormat.Encoding.PCM_UNSIGNED);
            put("ULAW", AudioFormat.Encoding.ULAW);
        } };

    /** コマンドラインオプションテーブル. */
    private static final Options OPTIONS =
        new Options() { {
            addOption("e", "Encoding", true,
                    "エンコーディング(ALAW, PCM_SIGNED, PCM_UNSIGNED, ULAW)");
            addOption("r", "SampleRate", true, "サンプリングレート(Hz)");
            addOption("s", "SampleSize", true, "サンプリングサイズ(bit)");
            addOption("c", "Correction", true, "周波数補正(%)");
            addOption("l", "Length", true, "音声の長さ(ms)");
            addOption("p", "Path", true, "出力先ファイルパス");
            addOption("v", "Volume", true, "ボリューム");
        } };

    /** デフォルトコンストラクタの禁止. */
    private DtmfCreator() { };

    /**
     * @param args コマンドラインオプション
     */
    public static void main(final String[] args) {
        try {
            CommandLineParser parser = new BasicParser();
            CommandLine commandLine = parser.parse(OPTIONS, args);

            AudioFormat.Encoding encoding = getEncoding(commandLine);
            float sampleRate = getSampleRate(commandLine);
            int sampleSize = getSampleSize(commandLine);
            int correction = getCorrection(commandLine);
            long length = getLength(commandLine);
            String path = getPath(commandLine);

            byte[] waveData = new byte[(int) (sampleRate * (length / 1000.0))];
            double[] frequencies = FREQUENCIES.get("2");
            for (int i = 0; i < waveData.length; ++i) {
                double magnification = (double) (Byte.MAX_VALUE - frequencies.length) / (double) frequencies.length;
                for (int j = 0; j < frequencies.length; ++j) {
                    frequencies[j] = sampleRate / (frequencies[j] * ((double) (correction + 100) / 100.0));
                    //frequencies[j] = sampleRate / frequencies[j];
                    waveData[i] += (byte)(magnification * Math.sin(((double) i / frequencies[j]) * Math.PI * 2.0));
                }
            }
            AudioInputStream inputStream = new AudioInputStream(
                    new ByteArrayInputStream(waveData),
//                    new AudioFormat(
//                            encoding,
//                            sampleRate,
//                            sampleSize,
//                            1,
//                            sampleSize,
//                            sampleRate,
//                            false),
                    new AudioFormat(sampleRate, sampleSize, 1, true, false),
                    waveData.length);
            AudioSystem.write(
                    inputStream,
                    AudioFileFormat.Type.WAVE,
                    new File(path
                            + File.separator
                            + "2"
                            + "."
                            + AudioFileFormat.Type.WAVE.getExtension()));

        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * コマンドラインからエンコーディングを取得します.
     * @param commandLine コマンドライン
     * @return エンコーディング
     */
    private static AudioFormat.Encoding getEncoding(
            final CommandLine commandLine) {
        AudioFormat.Encoding encoding = AudioFormat.Encoding.PCM_SIGNED;
        if (commandLine.hasOption("e")) {
            encoding = ENCODINGS.get(commandLine.getOptionValue("e"));
        }
        if (encoding == null) {
            throw new IllegalArgumentException("無効なエンコーディングです。");
        }
        return encoding;
    }

    /**
     * コマンドラインからサンプリングレートを取得します.
     * @param commandLine コマンドライン
     * @return サンプリングレート
     */
    private static float getSampleRate(final CommandLine commandLine) {
        float sampleRate = 8000.0f;
        if (commandLine.hasOption("r")) {
            sampleRate = Float.valueOf(commandLine.getOptionValue("r"));
        }
        return sampleRate;
    }

    /**
     * コマンドラインからサンプリングサイズを取得します.
     * @param commandLine コマンドライン
     * @return サンプリングサイズ
     */
    private static int getSampleSize(final CommandLine commandLine) {
        int sampleSize = 16;
        if (commandLine.hasOption("s")) {
            sampleSize = Integer.valueOf(commandLine.getOptionValue("s"));
        }
        return sampleSize;
    }

    /**
     * コマンドラインから周波数補正値を取得します.
     * @param commandLine コマンドライン
     * @return 周波数補正値
     */
    private static int getCorrection(final CommandLine commandLine) {
        int correction = 0;
        if (commandLine.hasOption("c")) {
            correction = Integer.valueOf(commandLine.getOptionValue("c"));
        }
        return correction;
    }

    /**
     * コマンドラインから音声の長さを取得します.
     * @param commandLine コマンドライン
     * @return 音声の長さ
     */
    private static long getLength(final CommandLine commandLine) {
        long length = 1000;
        if (commandLine.hasOption("l")) {
            length = Long.valueOf(commandLine.getOptionValue("l"));
        }
        return length;
    }

    /**
     * コマンドラインから出力先ファイルパスを取得します.
     * @param commandLine コマンドライン
     * @return 出力先ファイルパス
     */
    private static String getPath(final CommandLine commandLine) {
        String path = "." + File.separator;
        if (commandLine.hasOption("p")) {
            path = commandLine.getOptionValue("p");
            File folder = new File(path);
            if (!folder.isDirectory()) {
                throw new IllegalArgumentException("出力先がディレクトリではありません。");
            }
            // 未存在フォルダの場合は生成してしまう
            if (!folder.exists()) {
                folder.mkdirs();
            }
        }
        return path;
    }
}
