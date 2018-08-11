package meanlam.dualmicrecord.utils;

/**
 * Created by Administrator on 2018/5/31.
 */

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import meanlam.dualmicrecord.MainActivity;

public class AudioRecordFunc {

    public static int micbufferSizeInBytes  = 0; // MIC缓冲区字节大小（立体声）
    private int micbufferSizeInBytes1 = 0; // MIC缓冲区字节大小(单声道)

    private String micAudioRawPath      = ""; //AudioName裸音频数据文件 ，麦克风
    private String micAudioWavePath     = "";//NewAudioName可播放的音频文件
    private String leftRawPath          = "";
    private String leftWavPath          = "";
    private String rightWavPath         = "";
    private String rightRawPath         = "";

    public static AudioRecord micRecord;
    public static boolean ismicRecord = false;// 设置MIC正在录制的状态
    private static AudioRecordFunc             micInstance;



    public synchronized static AudioRecordFunc getInstance() {
        if (micInstance == null)
            micInstance = new AudioRecordFunc();
        return micInstance;
    }

    private void creatAudioMicRecord() {

        // 获得缓冲区字节大小
        micbufferSizeInBytes = AudioRecord.getMinBufferSize(AudioFileFunc.AUDIO_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT);

        micbufferSizeInBytes1 = AudioRecord.getMinBufferSize(AudioFileFunc.AUDIO_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        // 获取音频文件路径
        micAudioRawPath = AudioFileFunc.getRawFilePath("mic", 1);
        micAudioWavePath = AudioFileFunc.getWavFilePath("mic", 1);

        //左右声道文件
        leftRawPath = AudioFileFunc.getRawFilePath("leftmic", 2);
        leftWavPath = AudioFileFunc.getWavFilePath("leftmic", 2);

        rightRawPath = AudioFileFunc.getRawFilePath("rightmic", 3);
        rightWavPath = AudioFileFunc.getWavFilePath("rigtmic", 3);


        // 创建AudioRecord对象
        micRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, AudioFileFunc.AUDIO_SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_STEREO, AudioFormat.ENCODING_PCM_16BIT, micbufferSizeInBytes);
        Log.i("Onemeaning", "3、MIC正在录音。。。");

    }

    //开始录音并保存录音文件
    public void startRecordMic() {

        if (micRecord == null)
            creatAudioMicRecord();

        micRecord.startRecording();
        ismicRecord = true;                     //设置MIC正在录音
        MainActivity.startTime = System.currentTimeMillis();
        MainActivity.updateMicStatus();

        Log.i("Onemeaning", "4、来自MIC的PCM文件正在写入内存");
        writeMICDateTOFile();//往文件中写入裸数据

        Log.i("Onemeaning", "5、来自MIC的WAV文件正在写入内存");
        copyWaveFile(micAudioRawPath, micAudioWavePath, micbufferSizeInBytes);
        copyWaveFile(leftRawPath, leftWavPath, micbufferSizeInBytes1);//给左声道裸数据加上头文件变成WAV文件
        copyWaveFile(rightRawPath, rightWavPath, micbufferSizeInBytes1);//给右声道裸数据加上头文件变成WAV文件
    }

    public void stopMicRecordAndFile() {
        closeMic();
    }

    //    public long getRecordFileSize(){
    //        return AudioFileFunc.getFileSize(micAudioRawPath);
    //    }

    private void closeMic() {

        if (micRecord != null) {
            ismicRecord = false;//停止文件写入
            micRecord.stop();
            micRecord.release();//释放资源
            micRecord = null;
        }
    }

//    private double calculateVolume(byte[] buffer){
//            double sumVolume = 0.0;
//            double avgVolume = 0.0;
//            double volume = 0.0;
//            for(int i = 0; i < buffer.length; i+=2){
//                int v1 = buffer[i] & 0xFF;
//                int v2 = buffer[i + 1] & 0xFF;
//                int temp = v1 + (v2 << 8);// 小端
//                if (temp >= 0x8000) {
//                    temp = 0xffff - temp;
//                }
//                sumVolume += Math.abs(temp);
//            }
//            avgVolume = sumVolume / buffer.length / 2;
//            volume = Math.log10(1 + avgVolume) * 10;
//            return volume;
//        }

    /**
     * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
     * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
     * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
     */
    private void writeMICDateTOFile() {
        // new一个byte数组用来存一些字节数据，大小为缓冲区大小
        byte[] audiodata = new byte[micbufferSizeInBytes];
        byte[] leftChannelAudioData = new byte[micbufferSizeInBytes / 2];
        byte[] rightChannelAudioData = new byte[micbufferSizeInBytes / 2];

        FileOutputStream fos = null;
        FileOutputStream fos1 = null;
        FileOutputStream fos2 = null;

        int readsize = 0;
        try {
            File file = new File(micAudioRawPath);
            if (file.exists()) {
                file.delete();
            }
            fos = new FileOutputStream(file);// 建立一个可存取MIC数据流的文件
            fos1 = new FileOutputStream(new File(leftRawPath));// 建立一个可存取MIC数据流的文件
            fos2 = new FileOutputStream(new File(rightRawPath));// 建立一个可存取MIC数据流的文件

        } catch (Exception e) {
            e.printStackTrace();
        }
        while (ismicRecord == true) {

            readsize = micRecord.read(audiodata, 0, micbufferSizeInBytes);

            //双声道分离代码
            for (int i = 0; i < readsize / 2; i = i + 2) {
                leftChannelAudioData[i] = audiodata[2 * i];
                leftChannelAudioData[i + 1] = audiodata[2 * i + 1];
                rightChannelAudioData[i] = audiodata[2 * i + 2];
                rightChannelAudioData[i + 1] = audiodata[2 * i + 3];
            }

            if (AudioRecord.ERROR_INVALID_OPERATION != readsize && fos != null) {
                try {
                    fos.write(audiodata);
                    fos1.write(leftChannelAudioData);
                    fos2.write(rightChannelAudioData);

                    //左右声道写入文件
                    //                   new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File
                    // (leftRawPath)))).write(leftChannelAudioData);
                    //                   new DataOutputStream(new BufferedOutputStream(new FileOutputStream(new File
                    // (rigtRawPath)))).write(rightChannelAudioData);


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        try {
            if (fos != null) {
                fos.close();// 关闭写入流
                fos1.close();
                fos2.close();

            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 这里得到可播放的WAV音频文件
    private void copyWaveFile(String inFilename, String outFilename, int flag) {
        FileInputStream in = null;
        FileOutputStream out = null;

        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = AudioFileFunc.AUDIO_SAMPLE_RATE;
        int channels = 2;
        long byteRate = 16 * AudioFileFunc.AUDIO_SAMPLE_RATE * channels / 8;
        byte[] data = new byte[flag];
        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);

            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            if (flag == micbufferSizeInBytes1) {
                WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                        longSampleRate / 2, channels, byteRate);
            } else if (flag == micbufferSizeInBytes) {
                WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                        longSampleRate, channels, byteRate);
            }

            while (in.read(data) != -1) {
                out.write(data);
            }
            Log.i("Onemeaning", "6、来自MIC的WAV音频已写入到文件中！");

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 这里提供一个头信息。插入这些信息就可以得到可以播放的文件。
     * 为我为啥插入这44个字节，这个还真没深入研究，不过你随便打开一个wav
     * 音频的文件，可以发现前面的头文件可以说基本一样哦。每种格式的文件都有
     * 自己特有的头文件。
     */
    private void WriteWaveFileHeader(FileOutputStream out, long totalAudioLen,
                                     long totalDataLen, long longSampleRate, int channels, long byteRate)
            throws IOException {
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16; // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1; // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16; // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        out.write(header, 0, 44);
    }

    /**
     * 清空文件夹中的所有文件
     *
     * @param file 文件名
     * @throws Exception
     */
    public static void clearCache(File file) throws Exception {
        File[] files = file.listFiles();
        for (int i = 0; i < files.length; i++) {
            if (files[i].isDirectory())
                clearCache(files[i]);
            else {
                files[i].delete();
            }
        }

        //        file.delete();
    }
}