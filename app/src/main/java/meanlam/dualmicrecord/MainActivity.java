package meanlam.dualmicrecord;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import meanlam.dualmicrecord.audioUtils.AudioFileFunc;
import meanlam.dualmicrecord.audioUtils.AudioRecordFunc;
import meanlam.dualmicrecord.networkutils.ConnectDataBase;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener{

    static final int VOICE_REQUEST_CODE = 66;

    private Button mButtonclear;
    private Button mButtonCon;
    private Button mButtonCalcTdoa;
    private Button mButtonRecord;
    private TextView mTitleView;

    private Spinner sampleSpiner;//选择采样率
    private Spinner pcmbitsSPiner;//选择编码位数
    private EditText mTime;//用于获取用户输入的定义的时间

    private List<String> pcmBitsData;
    private ArrayAdapter pcmBitsRateAdapter;

    private int SAMPLE_RATE = 44100;
    private int ENCODING_BITS = 16;

    private AudioRecordFunc  micInstance;

    private Context            context;

    private        ListView                  liebiao       = null;
    private        SimpleAdapter             adpter        = null;

    private        List<Map<String, String>> luyinliebiao  = null;
    private        Boolean                   sdcardExist   = false;
    public       static  File                      baocunlujin   = null;
    private        File                      mPcmDirectory = null;



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

        this.startListener();
        mTitleView.setText("录音文件总数目为："+updateTitleInfo());

        mTime.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
//                Log.i("Ansen","内容改变之前调用:"+s);
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
//                Log.i("Ansen","内容改变，可以去告诉服务器:"+s);
            }

            @Override
            public void afterTextChanged(Editable s) {
             if(s!=null&&!"".equals(s.toString()))
             {
                 mTitleView.setTextColor(Color.BLUE);
                 mTitleView.setText("成功设置录音时间"+s+"ms");
                AudioFileFunc.maxTimeLength = Integer.parseInt(s.toString());
             }
             else
             {
                 mTitleView.setTextColor(Color.RED);
                 mTitleView.setText("警告！！没有设置时间录制时间");
             }

            }
        });
    }


    /**
     * 初始化布局控件
     */
    private void initViewId() {
        context = this;
        mButtonclear = findViewById(R.id.buttonclear);
        mButtonCon = findViewById(R.id.button_TOconnect);
        mButtonCalcTdoa = findViewById(R.id.button_calcTdoa);
        mButtonRecord = findViewById(R.id.bt_record);
        mTitleView = findViewById(R.id.titleText);
        mTime = findViewById(R.id.ev_times);


        liebiao =  this.findViewById(R.id.liebiao);

        //PopupWindow的布局文件
        final View view = View.inflate(this, R.layout.layout_microphone, null);

        //Spiner控件
        sampleSpiner = findViewById(R.id.spiner_samplerate);
        pcmbitsSPiner = findViewById(R.id.spiner_pcmbits);


        /*
        * 采样率：音频的采样频率，每秒钟能够采样的次数，采样率越高，音质越高。
        * 给出的实例是44100、22050、11025但不限于这几个参数。
        * 例如要采集低质量的音频就可以使用4000、8000等低采样率。
        */
        ArrayAdapter<CharSequence> sequenceArrayAdapter = ArrayAdapter.createFromResource(this, R.array.sampleRate, android.R.layout.select_dialog_singlechoice);
        //3、为适配器设置下拉菜单的样式
        sequenceArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //4、将适配器配置到下拉列表上
        sampleSpiner.setAdapter(sequenceArrayAdapter);
        //5、给下拉菜单设置监听事件
        sampleSpiner.setOnItemSelectedListener(this);


//        /*
//        * 采样位数：安卓暂时支持16位和8位两种
//        * */
        pcmBitsData  = new ArrayList<String>();
        pcmBitsData.add("16");
        pcmBitsData.add("8");

        pcmBitsRateAdapter=new ArrayAdapter(this,android.R.layout.select_dialog_singlechoice,pcmBitsData);
        //3、为适配器设置下拉菜单的样式
        pcmBitsRateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        //4、将适配器配置到下拉列表上
        pcmbitsSPiner.setAdapter(pcmBitsRateAdapter);
        //5、给下拉菜单设置监听事件
        pcmbitsSPiner.setOnItemSelectedListener(this);
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


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

        switch (parent.getId())
        {
            case R.id.spiner_samplerate:
                String sampleRate = MainActivity.this.getResources().getStringArray(R.array.sampleRate)[position];
               mTitleView.setText("成功设置采样率为："+sampleRate);
                SAMPLE_RATE = Integer.parseInt(sampleRate);
                break;
            case R.id.spiner_pcmbits:
                String encodBit = pcmBitsData.get(position);
               mTitleView.setText("成功设置编码位数："+ encodBit);
                ENCODING_BITS = Integer.parseInt(encodBit);
                break;
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

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

            long diffTime = System.currentTimeMillis()-startTime;
           if (diffTime>= AudioFileFunc.maxTimeLength)
           {
               try{

                   handler.removeCallbacks(timer);

                   mTitleView.setText(diffTime+"ms");
                   new ThreadTwo().start();
                   mButtonRecord.setEnabled(true);
                   mButtonRecord.setText("点击录音");
                   mButtonRecord.setBackgroundColor(Color.BLUE);

               }
               catch (Exception e)
               {
                   mButtonRecord.setEnabled(true);
                   mButtonRecord.setText("点击录音");
                   mButtonRecord.setBackgroundColor(Color.BLUE);
                   handler.removeCallbacks(timer);
               }
           }
           else
           {
               handler.postDelayed(this, 100);
           }

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

        mButtonRecord.setOnClickListener(v ->
                {
                    Toast.makeText(MainActivity.this,"开始录音",Toast.LENGTH_SHORT).show();
                    AudioFileFunc.initParams(SAMPLE_RATE,ENCODING_BITS);
                    mButtonRecord.setEnabled(false);
                    mButtonRecord.setText("正在录音");
                    mButtonRecord.setBackgroundColor(Color.GRAY);
                    new ThreadOne().start();
                    handler.postDelayed(timer,10);
                }
                                      );

        //使用lambda表达式实现匿名内部类
        mTitleView.setOnClickListener(v ->
                {
                    Uri uri = Uri.fromFile(MainActivity.baocunlujin);
                    Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.setDataAndType(uri,"*/*");
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                }
                                  );


    }

                                // 该命用于以销毁定时器，一般可以在onStop里面调用

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
        }

    }

}

