package meanlam.dualmicrecord.audioUtils;

/**
 * Created by Administrator on 2018/5/31.
 */

import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Environment;
import android.util.Log;

import java.io.File;

public class AudioFileFunc {

    //音频输入-麦克风
    public final static int AUDIO_INPUT = MediaRecorder.AudioSource.MIC;
    //采用频率44100是目前的标准，但是某些设备仍然支持22050，16000，11025
    public static int AUDIO_SAMPLE_RATE = 44100;  //44.1KHz,普遍使用的频率
    public static int AUDIO_ENCODING_BYTES = AudioFormat.ENCODING_PCM_16BIT;

    private String filePath;//文件路径
    private String FolderPath;//文件夹路径
    public static String realPath;
    public static final String TAG = "AudioFileFunTest";
    public static String sampleRateName="";
    public static String encodeBytes = "";
    public static  int  maxTimeLength = 1050;

   public static void initParams(int sample,int encode )
   {
       AudioFileFunc.AUDIO_SAMPLE_RATE = sample;
       AudioFileFunc.AUDIO_ENCODING_BYTES = (encode ==16?AudioFormat.ENCODING_PCM_16BIT:AudioFormat.ENCODING_PCM_8BIT);
       AudioFileFunc.sampleRateName = sample+"hz";
       AudioFileFunc.encodeBytes = encode+"bits";
       Log.i(TAG, "采样率："+AudioFileFunc.AUDIO_SAMPLE_RATE+",编码位数"+ AudioFileFunc.AUDIO_ENCODING_BYTES+"最大时间："+AudioFileFunc.maxTimeLength);
   }


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
    public static String getRawFilePath(){
        String mAudioRawPath = "";
        if(isSdcardExit()){
            String fileBasePath = Environment.getExternalStorageDirectory()+"/record1/";
            File path = new File(fileBasePath);
            if(!path.exists())
            {
                path.mkdirs();
            }
            mAudioRawPath = fileBasePath +TimeUtils.getCurrentTime() +"_"+ sampleRateName+"_"+encodeBytes + ".pcm";
        }
        return mAudioRawPath;
    }

    /**
     * 获取编码后的WAV格式音频文件路径
     * @return
     */
    public static String getWavFilePath() {
        String mAudioWavPath = "";
        if (isSdcardExit()) {
            String fileBasePath = Environment.getExternalStorageDirectory() + "/record/";
            File path = new File(fileBasePath);
            if (!path.exists()){
                path.mkdirs();
            }
            mAudioWavPath = fileBasePath + TimeUtils.getCurrentTime()+"_" + sampleRateName+"_"+encodeBytes + ".wav";
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