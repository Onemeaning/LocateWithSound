package meanlam.dualmicrecord;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import meanlam.dualmicrecord.networkutils.ConnectDataBase;
import meanlam.dualmicrecord.utils.AudioRecordFunc;
import meanlam.dualmicrecord.utils.PopupWindowFactory;
import meanlam.dualmicrecord.utils.TimeUtils;

public class MainActivity extends AppCompatActivity {

    static final int VOICE_REQUEST_CODE = 66;


    private Button mButton;
    private Button mButtonclear;
    private Button mButtonCon;
    private Button mButtonCalcTdoa;
    private Button mButtonRecord;
    private TextView mTitleView;

    private static ImageView mImageView;
    private static TextView  mViewTime, tv_cancel;
    private AudioRecordFunc  micInstance;

    private Context            context;
    private PopupWindowFactory mPop;
    private RelativeLayout     rl;
    private        ListView                  liebiao       = null;
    private        SimpleAdapter             adpter        = null;

    private        List<Map<String, String>> luyinliebiao  = null;
    private        Boolean                   sdcardExist   = false;
    public       static  File                      baocunlujin   = null;
    private        File                      mPcmDirectory = null;
    private        boolean                   isDistory     = false;
    private      static   double                    volume        = 0;

    public static  long                      startTime     = 0;


    @SuppressLint("HandlerLeak")//这个handle文件专门用于处理更新录音或者删除文件后的UI的更新
    private final  Handler mHandler1 = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 1) {
                MainActivity.this.getFileList();
                Toast.makeText(MainActivity.this, baocunlujin.getName() + "文件夹中文件已删除", Toast
                        .LENGTH_SHORT).show();
                Toast.makeText(MainActivity.this, mPcmDirectory.getName() + "文件夹中文件已删除", Toast
                        .LENGTH_SHORT).show();
            } else if (msg.what == 2) {

               String length = msg.getData().getString("length");
                mTitleView.setText(length);
                MainActivity.this.getFileList();
            }

        }
    };


    @SuppressLint("HandlerLeak")
    public static final Handler mHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if(msg.what==1)
            {
                mImageView.getDrawable().setLevel((int) (3000 + 9000 * volume / 100));
                //setlevel（）中的值取值为0-10000，如果用了clip标签就是把图片剪切成10000份
                mViewTime.setText(TimeUtils.long2String((System.currentTimeMillis()-startTime)));
            }
        }
    };


    public  Runnable mUpdateMicStatusTimer = new Runnable() {
        public void run() {
            updateMicStatus();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try
                {
                    ConnectDataBase.query(ConnectDataBase.getSQLConnection());
                    mHandler1.sendEmptyMessage(2);

                }
                catch (Exception e)
                {
                    Log.i("Onemeaning", "onCreate: "+ConnectDataBase.todaList);
                    Message msg = new Message();
                    msg.what = 2;
                    Bundle bundle = new Bundle();
                    bundle.putString("length", "服务器未连接");
                    msg.setData(bundle);
                    mHandler1.sendMessage(msg);
                }



            }
        }).start();

        initViewId();
        requestPermissions();//6.0以上需要权限申请
        makeDirs();


        micInstance = AudioRecordFunc.getInstance();

        MainActivity.this.startListener();
        //用于监听，点击播放
        liebiao.setOnItemClickListener(new OnItemClickListenerImp());
        //listView长按事件,用于长按删除文件
        liebiao.setOnItemLongClickListener(new OnItemLongClickListenerImp());
        mTitleView.setText("录音文件总数目为："+updateTitleInfo());
        setOnAudioStatusUpdateListener(new OnAudioStatusUpdateListener() {
            @Override
            public void onUpdate(double db, long time) {

                mImageView.getDrawable().setLevel((int) (3000 + 9000 * volume / 100));
                //setlevel（）中的值取值为0-10000，如果用了clip标签就是把图片剪切成10000份
                mViewTime.setText(TimeUtils.long2String(time));
            }
            @Override
            public void onStop(String filePath) {

            }
        });
    }


    /**
     * 初始化布局控件
     */
    private void initViewId() {
        context = this;
        rl = findViewById(R.id.rl);
        mButton = findViewById(R.id.button);
        mButtonclear = findViewById(R.id.buttonclear);
        mButtonCon = findViewById(R.id.button_TOconnect);
        mButtonCalcTdoa = findViewById(R.id.button_calcTdoa);
        mButtonRecord = findViewById(R.id.bt_record);
        mTitleView = findViewById(R.id.titleText);

        liebiao =  this.findViewById(R.id.liebiao);

        //PopupWindow的布局文件
        final View view = View.inflate(this, R.layout.layout_microphone, null);
        mPop = new PopupWindowFactory(this, view);

        //PopupWindow布局文件里面的控件
        mImageView = (ImageView) view.findViewById(R.id.iv_recording_icon);
        mViewTime = (TextView) view.findViewById(R.id.tv_recording_time);
        tv_cancel = (TextView) view.findViewById(R.id.tv_recording_info);

    }

    // 存储媒体已经挂载，并且挂载点可读/写
    private void makeDirs() {
        if (sdcardExist = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            MainActivity.this.baocunlujin = new File(Environment.getExternalStorageDirectory() +
                    "/record/");
            MainActivity.this.mPcmDirectory = new File(Environment.getExternalStorageDirectory()
                    + "/record1/");

            if (!MainActivity.this.baocunlujin.exists()) {
                MainActivity.this.baocunlujin.mkdirs();
            }
            if (!MainActivity.this.mPcmDirectory.exists()) {
                MainActivity.this.mPcmDirectory.mkdirs();
            }
        }
    }

    /**
     * 开启扫描之前判断权限是否打开
     */
    private void requestPermissions() {
        //判断是否开启摄像头权限
        if ((ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(context,
                        Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
                ) {
            //            startListener();

            //判断是否开启语音权限
        } else {
            //请求获取摄像头权限
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission
                            .RECORD_AUDIO},
                    VOICE_REQUEST_CODE);
        }

    }

    /**
     * 请求权限回调
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[]
            grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == VOICE_REQUEST_CODE) {
            if ((grantResults[0] == PackageManager.PERMISSION_GRANTED) && (grantResults[1] ==
                    PackageManager
                            .PERMISSION_GRANTED)) {
                //                startListener();
            } else {
                Toast.makeText(context, "已拒绝权限！", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //删除录音文件
    private class OnItemLongClickListenerImp implements AdapterView.OnItemLongClickListener {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long
                id) {
            //           定义AlterDialog.Builder对象，当长按列表项时弹出确认删除对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("确定删除吗？");
            builder.setTitle("提示");

            //添加AlertDialog.Builder对象的setPositiveButton()方法
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (luyinliebiao.remove(position) != null) {
                        File[] files = baocunlujin.listFiles();
                        files[position].delete();

                        File[] files1 = mPcmDirectory.listFiles();
                        files1[position].delete();


                        Toast.makeText(getBaseContext(), "成功删除录音", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getBaseContext(), "取消删除录音", Toast.LENGTH_SHORT).show();
                    }
                    MainActivity.this.adpter.notifyDataSetChanged();

                }
            });
            //添加AlertDialog.Builder对象的setNegativeButton()方法
            builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {

                }
            });
            builder.create().show();
            return false;
        }
    }


    //播放列表音频文件
    private class OnItemClickListenerImp implements AdapterView.OnItemClickListener {

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            if (MainActivity.this.adpter.getItem(position) instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) MainActivity.this.adpter.getItem(position);
                Uri uri = Uri.fromFile(
                        new File(MainActivity.this.baocunlujin.toString() + File.separator + map
                                .get("fileName")));
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.setDataAndType(uri, "audio/*");
                MainActivity.this.startActivity(intent);
            }

        }


    }

    //获取录音文件列表,以及计算好的TDOA值
    private void getFileList() {
        luyinliebiao = new ArrayList<Map<String, String>>();
        luyinliebiao.clear();
        if (sdcardExist) {
            File[] files = MainActivity.this.baocunlujin.listFiles();
            for (int i = 0; i < ConnectDataBase.nameList.size(); i++) {
                Map<String, String> fileinfo = new HashMap<String, String>();
                fileinfo.put("fileName", ConnectDataBase.nameList.get(i) );
                fileinfo.put("tdoa", ConnectDataBase.todaList.get(i));
                this.luyinliebiao.add(fileinfo);
            }

            this.adpter = new SimpleAdapter(this, this.luyinliebiao, R.layout.recorderfiles, new
                    String[]{"fileName","tdoa"}, new int[]{R.id.fileName,R.id.TDOA});
            this.liebiao.setAdapter(this.adpter);

        }
    }

    //更新TitleText的上传提示

    private String updateTitleInfo()
    {
        File[] files = MainActivity.this.baocunlujin.listFiles();

        return String.valueOf(files.length);
    }

    Handler handler = new Handler();
    Runnable timer = new Runnable() {
        @Override
        public void run() {
           if (System.currentTimeMillis()-startTime>= AudioRecordFunc.maxTimeLength)
           {
               try{
                   new ThreadTwo().start();
                   mButtonRecord.setEnabled(true);
                   mButtonRecord.setText("点击录音");
                   mButtonRecord.setBackgroundColor(Color.BLUE);

                   //               handler.removeCallbacks(timer);
//                    Thread.sleep(1000);
//                   mButtonRecord.setEnabled(false);
//                   mButtonRecord.setText("正在录音");
//                   mButtonRecord.setBackgroundColor(Color.GRAY);
//                   new ThreadOne().start();
//                   MainActivity.startTime = System.currentTimeMillis();

                   handler.removeCallbacks(timer);
//                   handler.postDelayed(this, 10);//连续录音就打开这个
               }
               catch (Exception e)
               {
                   handler.removeCallbacks(timer);
                   mButtonRecord.setEnabled(true);
                   mButtonRecord.setText("点击录音");
                   mButtonRecord.setBackgroundColor(Color.BLUE);
               }

           }
            handler.postDelayed(this, 10);
        }

    };

    @SuppressLint("ClickableViewAccessibility")
    public void startListener() {
        //Button的touch监听
        mButtonclear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.buttonclear) {
                    try {
                        AudioRecordFunc.clearCache(baocunlujin);
                        AudioRecordFunc.clearCache(mPcmDirectory);
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                mHandler1.sendEmptyMessage(1);
                            }
                        }).start();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        mButtonCon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.button_TOconnect) {

                    startActivity(new Intent(MainActivity.this, ConnectDBActivity.class));
                }
            }
        });

        mButtonCalcTdoa.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.button_calcTdoa) {

                    new Thread(new Runnable() {
                            @Override
                            public void run() {

                            ConnectDataBase.query(ConnectDataBase.getSQLConnection());
                            mHandler1.sendEmptyMessage(2);
                            Log.i("Onemeaning", "onCreate: "+ConnectDataBase.todaList);

                        }
                    }).start();
                }
            }
        });
        mButtonRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.bt_record) {
                    Toast.makeText(MainActivity.this,"开始录音",Toast.LENGTH_SHORT).show();
                    mButtonRecord.setEnabled(false);
                    mButtonRecord.setText("正在录音");
                    mButtonRecord.setBackgroundColor(Color.GRAY);
                    new ThreadOne().start();
                   handler.postDelayed(timer,10);

                }
            }
        });

        mButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                int start_x = 0, start_y = 0, end_x, end_y, mov_x, mov_y;
                switch (event.getAction()) {

                    case MotionEvent.ACTION_DOWN:
                        tv_cancel.setTextColor(Color.parseColor("#FFFFFF"));
                        mImageView.setImageDrawable(context.getResources().getDrawable(R.drawable.record_microphone));
                        mPop.showAtLocation(rl, Gravity.CENTER, 0, 0);
                        mButton.setText("松开保存");

                        try {
                            handler.removeCallbacks(timer);
                            mButtonRecord.setEnabled(true);
                            mButtonRecord.setText("点击录音");
                            mButtonRecord.setBackgroundColor(Color.BLUE);

                            ThreadOne t1 = new ThreadOne();
                            mImageView.getDrawable().setLevel((int) (3000 + 9000 * 40 / 100));
                            //setlevel（）中的值取值为0-10000，如果用了clip标签就是把图片剪切成10000份
                            mViewTime.setText("正在录音");
                            t1.start();

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        break;

                    case MotionEvent.ACTION_UP:
                        try {
                            ThreadTwo t2 = new ThreadTwo();
                            t2.start();
                            Thread.sleep(400);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        mPop.dismiss();
                        mButton.setText("按住录音");
                        Log.i("Onemeaning", "7、手指已经离开录音键");
                        break;

                    case MotionEvent.ACTION_MOVE:
                        end_y = (int) event.getY();
                        mov_y = Math.abs(start_y - end_y);
                        if (mov_y < 300 && mov_y > 150) {
                            try {
                                new Thread()
                                {
                                    @Override
                                    public void run() {
                                        micInstance.cancelRecord();
                                    }
                                }.start();
                                mPop.dismiss();
                            }
                            catch (Exception e) {
                                e.printStackTrace();
                            }

                        }
                        if (mov_y < 100) {
                            //                            tv_cancel.setText("松开保存");
                            //                            tv_cancel.setTextColor(Color.parseColor
                            // ("#FFFFFF"));
                        }
                        break;

                }
                return true;
            }
        });
    }

    private class ThreadOne extends Thread {
        @Override
        public void run() {
            MainActivity.startTime = System.currentTimeMillis();
            micInstance.startRecordMic();
        }
    }

    private class ThreadTwo extends Thread {
        @Override
        public void run() {
            micInstance.stopMicRecordAndFile();
            Message msg = new Message();
            msg.what = 2;
            Bundle bundle = new Bundle();
            String length = updateTitleInfo();
            bundle.putString("length","录音文件的个数："+length);
            msg.setData(bundle);
            mHandler1.sendMessage(msg);
//            mHandler1.sendEmptyMessage(2);
        }

    }

    /*
     *下面是用于更新麦克风录音时动画的状态的
     */

    private OnAudioStatusUpdateListener audioStatusUpdateListener;


    public void setOnAudioStatusUpdateListener(OnAudioStatusUpdateListener
                                                       audioStatusUpdateListener) {
        this.audioStatusUpdateListener = audioStatusUpdateListener;
    }

    /**
     * 用于实时获取手机麦克风的音量大小
     *
     * @author Meanlam
     */
    private void updateMicStatus() {

        if (AudioRecordFunc.micRecord != null) {
            Log.i("Meanlam", "OK ");
            byte[] byte_buffer = new byte[AudioRecordFunc.micbufferSizeInBytes];
            int readSize = AudioRecordFunc.micRecord.read(byte_buffer, 0, AudioRecordFunc
                    .micbufferSizeInBytes);
            Log.i("Onemeaning", "readSize:: " + readSize);
            long v = 0;
            for (int i = 0; i < byte_buffer.length; i++) {
                v += byte_buffer[i] * byte_buffer[i];
            }
            // 平方和除以数据总长度，得到音量大小。
            double mean = v / (double) readSize;
            volume = 20 * Math.log10(mean);

//            if (null != audioStatusUpdateListener) {
//                audioStatusUpdateListener.onUpdate(volume, System.currentTimeMillis() - startTime);
//            }

//            Log.i("Onemeaning", "volum:: " + volume);

        }
            mHandler.sendEmptyMessage(1);
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
