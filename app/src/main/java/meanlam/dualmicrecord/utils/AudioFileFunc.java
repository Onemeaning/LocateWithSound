package meanlam.dualmicrecord.utils;

/**
 * Created by Administrator on 2018/5/31.
 */

import android.media.MediaRecorder;
import android.os.Environment;

import java.io.File;

public class AudioFileFunc {

    //音频输入-麦克风
    public final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    //采用频率44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    public final static int AUDIO_SAMPLE_RATE = 44100;  //44.1KHz,普遍使用的频率

    private String filePath;//文件路径
    private String FolderPath;//文件夹路径
    public static String realPath;

    /**
     * 判断是否有外部存储设备sdcard
     * @return true | false
     */

    public static boolean isSdcardExit(){
        if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED))
            return true;
        else
            return false;
    }

    /**
     * 获取麦克风输入的原始音频流文件路径
     * @return
     */
    public static String getRawFilePath(String type ,int id){
        String mAudioRawPath = "";
        if(isSdcardExit()){
            String fileBasePath = Environment.getExternalStorageDirectory()+"/record1/";

            if(id==1) {
                File path = new File(fileBasePath);
                if(!path.exists())
                    path.mkdirs();
                mAudioRawPath = fileBasePath + type + TimeUtils.getCurrentTime() + ".pcm";
            }
            else if(id==2)
            {
                File path1 = new File(fileBasePath);
                if(!path1.exists())
                    path1.mkdirs();
                mAudioRawPath = fileBasePath + type + TimeUtils.getCurrentTime() + ".pcm";
            }
            else if(id==3)
            {
                File path2 = new File(fileBasePath);
                if(!path2.exists())
                    path2.mkdirs();
                mAudioRawPath = fileBasePath + type + TimeUtils.getCurrentTime() + ".pcm";
            }

        }
        return mAudioRawPath;
    }

    /**
     * 获取编码后的WAV格式音频文件路径
     * @return
     */
    public static String getWavFilePath(String type ,int id){
        String mAudioWavPath = "";
        if(isSdcardExit()){
            String fileBasePath = Environment.getExternalStorageDirectory() + "/record/";
            if(id==1) {
                File path = new File(fileBasePath);
                if (!path.exists())
                    path.mkdirs();
                mAudioWavPath = fileBasePath + type + TimeUtils.getCurrentTime() + ".wav";
            }
            if(id==2) {
                File path1 = new File(fileBasePath);
                if (!path1.exists())
                    path1.mkdirs();
                mAudioWavPath = fileBasePath + type + TimeUtils.getCurrentTime() + ".wav";
            }
            if(id==3) {
                File path2 = new File(fileBasePath);
                if (!path2.exists())
                    path2.mkdirs();
                mAudioWavPath = fileBasePath + type + TimeUtils.getCurrentTime() + ".wav";
            }

        }
        return mAudioWavPath;
    }

    /**
     * 获取文件大小
     * @param path,文件的绝对路径
     * @return
     */
    public static long getFileSize(String path){
        File mFile = new File(path);
        if(!mFile.exists())
            return -1;
        return mFile.length();
    }

}