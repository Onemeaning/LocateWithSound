package meanlam.dualmicrecord;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

import meanlam.dualmicrecord.networkutils.ConnectDataBase;
import meanlam.dualmicrecord.networkutils.SelfDialog;
import meanlam.dualmicrecord.uploadfileutil.FormFile;
import meanlam.dualmicrecord.uploadfileutil.SocketHttpRequester;
import meanlam.dualmicrecord.utils.IPAddress;

public class ConnectDBActivity extends AppCompatActivity {

    private Button   button_connect;
    private Button   button_uoload;
    private Button button_download;
    private TextView mTitleText;
    private TextView textview;
    private String   str;
    private File     file;
    private File[]  files;
    private Connection  connection;
    private static final String TAG="Meanlam_ConDB";
    private  int length = 0;
    private  int lengthOK = 0;
    private  IPAddress ipPort;


    // 消息显示到控件
    @SuppressLint("HandlerLeak")
    Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case 1001:
                    str = msg.getData().getString("result");
                    textview.setText(str);
                    break;
                case 1003:
                    str = msg.getData().getString("result");
                    textview.setTextColor(Color.GREEN);
                    textview.setText(str);
                case 1004:
                    String length = msg.getData().getString("unSendAudio");
                    mTitleText.setText(length);

                default:
                    break;
            }
        };
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_connectserver);

        ipPort = (IPAddress) this.getApplication();

        button_connect =  findViewById(R.id.connect);
        button_uoload = findViewById(R.id.bt_uploadfile);
        button_download = findViewById(R.id.bt_download);

        mTitleText = findViewById(R.id.titleText2);
        textview = findViewById(R.id.sqlContent);


        // 出现输入IP的dialog
        button_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                   openCustomerDialog();
            }
        });

        //一键上传功能
        button_uoload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadAllFile();
            }
        });

        //测试数据库是否联通
        button_download.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testSQLConnection();
            }
        });
    }

    /**
     *往服务器中传送录音文件夹下的所有的文件（按照录音文件夹下顺序上传）
     */
    private void uploadAllFile()
    {
    Runnable runnable1 =new Runnable() {
        public void run() {
            MainActivity.baocunlujin =  new File(Environment.getExternalStorageDirectory() + "/record/");
             files = MainActivity.baocunlujin.listFiles();
            length = files.length;
            lengthOK = 0;
            for (int i = 0; i < files.length; i++) {

                Message msg = new Message();
                msg.what = 1004;
                Bundle bundle = new Bundle();
                bundle.putString("unSendAudio","未发送个数："+length+"   已经发送个数："+lengthOK);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                file = new File(Environment.getExternalStorageDirectory()+"/record/", files[i].getName());
                Log.i(TAG, "文件是否存在： " + file.exists());
                uploadFile(file);

                files[i].delete();

                ++lengthOK;
                --length;
            }

            Message msg = new Message();
            msg.what = 1004;
            Bundle bundle = new Bundle();
            bundle.putString("unSendAudio","文件发送完成");
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
    };
        new Thread(runnable1).start();

    }

    /**
     * 上传音频到服务器
     *
     * @param imageFile 包含路径
     */
    public void uploadFile(File imageFile) {
        Log.i(TAG, "upload start");
        try {
            String requestUrl = "http://"+ipPort.getIP()+":"+"8080/Up_Load/UploadServlet";
            //请求普通信息
            Map<String, String> params = new HashMap<String, String>();
            params.put("username", "张三");
            params.put("pwd", "zhangsan");
            params.put("age", "21");
            params.put("WAV_NAME", imageFile.getName());
            //上传文件
            FormFile formfile = new FormFile(imageFile.getName(), imageFile, "image", "application/octet-stream");
            SocketHttpRequester.post(requestUrl, params, formfile);
            Log.i(TAG, "upload success");
        } catch (Exception e) {
            Toast.makeText(ConnectDBActivity.this,"未连接IP",Toast.LENGTH_SHORT).show();
            files=null;
            Log.i(TAG, "upload error from uploadException "+e.toString());
            e.printStackTrace();
        }
        Log.i(TAG, "upload end");
    }

    // 查询MySQL数据库中的数据
    private void testSQLConnection() {
        Runnable run = new Runnable() {
            @Override
            public void run() {
                String ret = ConnectDataBase.query(connection);
                Log.i("Onemeaning", "SQLException:: " + ret);
                Message msg = new Message();
                msg.what = 1001;
                Bundle bundle = new Bundle();
                bundle.putString("result", ret);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }
        };
        new Thread(run).start();
    }

    //自定义LoginDialog，用于输入用户的IP和PORT
    public void openCustomerDialog(){
        final SelfDialog selfDialog = new SelfDialog(getContext());
        selfDialog.setShowTitle(true);
        selfDialog.setLoginListener(new SelfDialog.onLoginListener() {
            String editUserName;
            String editPassword;
             final CheckBox cbServiceItem = selfDialog.getCbServiceItem();
             String str = "";
            @Override
            public void onClick(View v) {
                 editUserName = selfDialog.getEditUserName().getText().toString();
                 editPassword = selfDialog.getEditPassword().getText().toString();

                if (editUserName==null||editPassword==null)
                {
                    Toast.makeText(ConnectDBActivity.this, editUserName+editPassword, Toast.LENGTH_LONG).show();
                    editUserName = ipPort.getIP();
                    editPassword = ipPort.getPORT();
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run()
                                {
                                    ConnectDataBase dataBase =  new ConnectDataBase(editUserName, editPassword ,ConnectDBActivity.this);
                                   try{
                                       connection = dataBase.getSQLConnection();
                                       str = "正在连接："+editUserName+":"+editPassword+"\n";
                                   }
                                   catch (Exception e)
                                   {
                                       str = "无法连接到"+editUserName+":"+editPassword+"\n";
                                       Log.i("Onemeaning", "SQLException:: " + connection.toString());
                                   }

                                    Message msg = new Message();
                                    msg.what = 1003;
                                    Bundle bundle = new Bundle();
                                    bundle.putString("result", str);
                                    msg.setData(bundle);
                                    mHandler.sendMessage(msg);
                                }
                            }
                    ).start();

                    selfDialog.dismiss();
                }

                else
                {
                    Toast.makeText(ConnectDBActivity.this, editUserName+editPassword, Toast.LENGTH_LONG).show();
                    ipPort.setIP(editUserName);
                    ipPort.setPORT(editPassword);
                    new Thread(
                            new Runnable() {
                                @Override
                                public void run()
                                {
                                    ConnectDataBase dataBase =  new ConnectDataBase(editUserName, editPassword ,ConnectDBActivity.this);
                                    try{
                                        connection = dataBase.getSQLConnection();
                                        str = "正在连接："+editUserName+":"+editPassword+"\n";
                                    }
                                    catch (Exception e)
                                    {
                                        str = "无法连接到"+editUserName+":"+editPassword+"\n";
                                        Log.i("Onemeaning", "SQLException:: " + connection.toString());
                                    }
                                    Message msg = new Message();
                                    msg.what = 1003;
                                    Bundle bundle = new Bundle();
                                    bundle.putString("result", str);
                                    msg.setData(bundle);
                                    mHandler.sendMessage(msg);
                                }
                            }
                    ).start();

                    selfDialog.dismiss();
                }
            }
        });
    }
    //获取当前对象
    private Context getContext(){
        return this;
    }

}