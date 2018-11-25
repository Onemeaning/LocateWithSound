package meanlam.dualmicrecord.utils;

import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.IOException;

import meanlam.dualmicrecord.MainActivity;

public class MediaRecoderUtils {

    //文件路径
    private String filePath;

    //文件夹路径
    private String FolderPath;

    private MediaRecorder mMediaRecorder;
    private final       String TAG        = "meanlam";
    public static final int    MAX_LENGTH = 1000 * 60 * 10;// 最大录音时长1000*60*10;

    private OnAudioStatusUpdateListener audioStatusUpdateListener;
    public boolean isRecord = false;

    /**
     * 文件存储默认sdcard/record
     */
    public MediaRecoderUtils() {

        //默认保存路径为/sdcard/record/下
        this(Environment.getExternalStorageDirectory() + "/record/");
        mMediaRecorder = new MediaRecorder();
    }

    public MediaRecoderUtils(String filePath) {

        File path = new File(filePath);
        if (!path.exists())
            path.mkdirs();

        this.FolderPath = filePath;
        mMediaRecorder = new MediaRecorder();
    }

    private long startTime;
    private long endTime;


    /**
     * 开始录音 使用3gp格式
     * 录音文件
     *
     * @return
     */
    public void startRecord(int type) {
        // 开始录音
        /* ①Initial：实例化MediaRecorder对象 */
        if (mMediaRecorder == null)
            mMediaRecorder = new MediaRecorder();

        try {
            /* ②setAudioSource/setVedioSource */
            //            mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);// 设置麦克风
            mMediaRecorder.setAudioSource(type);// 设置麦克风

            /* ②设置音频文件的编码：AAC/AMR_NB/AMR_MB/Default 声音的（波形）的采样 */
            mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            /*
             * ②设置输出文件的格式：THREE_GPP/MPEG-4/RAW_AMR/Default THREE_GPP(3gp格式
             * ，H263视频/ARM音频编码)、MPEG-4、RAW_AMR(只支持音频且音频编码要求为AMR_NB)
             */
            //            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
            mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            filePath = FolderPath + TimeUtils.getCurrentTime() + ".aac";//文件路径=文件夹路径+文件名字

            /* ③准备 */
            mMediaRecorder.setAudioSamplingRate(44100);
            //设置音质频率
            mMediaRecorder.setAudioEncodingBitRate(9600);

            mMediaRecorder.setOutputFile(filePath);
            mMediaRecorder.setMaxDuration(MAX_LENGTH);
            mMediaRecorder.prepare();

            /* ④开始 */
            mMediaRecorder.start();
            Log.i("Onemeaning", "MIC第一次调用开启正常");
            isRecord = true;
            // AudioRecord audioRecord.
            /* 获取开始时间* */
            startTime = System.currentTimeMillis();

            updateMicStatus();

        } catch (IllegalStateException e) {
            Log.i(TAG, "call startAmr(File mRecAudioFile) failed!" + e.getMessage());
        } catch (IOException e) {
            Log.i(TAG, "call startAmr(File mRecAudioFile) failed!" + e.getMessage());
        }
    }


    /**
     * 停止录音
     */
    public long stopRecord() {

        if (mMediaRecorder == null)
            return 0L;
        endTime = System.currentTimeMillis();

        try {

            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

            audioStatusUpdateListener.onStop(filePath);
            filePath = "";

        } catch (RuntimeException e) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

            File file = new File(filePath);
            if (file.exists())
                file.delete();
            isRecord = false;
            filePath = "";

        }

        return endTime - startTime;
    }

    /**
     * 取消录音
     */
    public void cancelRecord() {

        try {

            mMediaRecorder.stop();
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;

        } catch (RuntimeException e) {
            mMediaRecorder.reset();
            mMediaRecorder.release();
            mMediaRecorder = null;
        }

        File file = new File(filePath);

        if (file.exists())
            file.delete();
        filePath = "";

    }

    private final Handler mHandler = new Handler();

    private Runnable mUpdateMicStatusTimer = new Runnable() {
        public void run() {
            updateMicStatus();
        }
    };


    private int BASE  = 1;
    private int SPACE = 100;// 间隔取样时间

    public void setOnAudioStatusUpdateListener(OnAudioStatusUpdateListener
                                                       audioStatusUpdateListener) {
        this.audioStatusUpdateListener = audioStatusUpdateListener;
    }

    //    /**
    //     * 更新麦克状态
    //     */
    //    private void updateMicStatus() {
    //
    //        if (mMediaRecorder != null) {
    //            double ratio = (double)mMediaRecorder.getMaxAmplitude() / BASE;//获取音量的大小
    //            double db = 0;// 分贝
    //            if (ratio > 1) {
    //                db = 20 * Math.log10(ratio);
    //                if(null != audioStatusUpdateListener) {
    //                    audioStatusUpdateListener.onUpdate(db,System.currentTimeMillis()
    // -startTime);
    //                }
    //            }
    //            mHandler.postDelayed(mUpdateMicStatusTimer, SPACE);
    //        }
    //    }

    /**
     * 用于实时获取手机麦克风的音量大小
     *
     * @author Meanlam
     */
    public void updateMicStatus() {

        if (AudioRecordFunc.micRecord != null) {
            Log.i("Meanlam", "OK ");
            byte[] byte_buffer = new byte[AudioRecordFunc.micbufferSizeInBytes];
            int readSize = AudioRecordFunc.micRecord.read(byte_buffer, 0, AudioRecordFunc
                    .micbufferSizeInBytes);
            long v = 0;
            for (int i = 0; i < byte_buffer.length; i++) {
                v += byte_buffer[i] * byte_buffer[i];
            }
            // 平方和除以数据总长度，得到音量大小。
            double mean = v / (double) readSize;
            double volume = 20 * Math.log10(mean);
            if (null != audioStatusUpdateListener) {
                audioStatusUpdateListener.onUpdate(volume, System.currentTimeMillis() - MainActivity.startTime);
            }
            Log.i("Onemeaning", "volum:: " + volume);

        }
        mHandler.postDelayed(mUpdateMicStatusTimer, 100);
    }


    public interface OnAudioStatusUpdateListener {
        /**
         * 录音中...
         *
         * @param db   当前声音分贝
         * @param time 录音时长
         */
        public void onUpdate(double db, long time);

        /**
         * 停止录音
         *
         * @param filePath 保存路径
         */
        public void onStop(String filePath);
    }

}
