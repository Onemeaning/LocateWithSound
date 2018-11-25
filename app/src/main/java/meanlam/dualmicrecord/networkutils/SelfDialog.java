package meanlam.dualmicrecord.networkutils;

import android.app.AlertDialog;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import meanlam.dualmicrecord.R;

public class SelfDialog extends AlertDialog {

    private Context context;
    private TextView tv_dialogTitle;
    private EditText editUserName ,editPassword;
    private CheckBox cbServiceItem;
    private AlertDialog.Builder builder;
    private View view;
    private onLoginListener loginListener = null;
    private Button btnLogin;
    private  AlertDialog dialog;

    //创建并初始化LoginDialog
    public SelfDialog(Context context) {
        super(context);

        this.context = context;
        builder = new AlertDialog.Builder(context);
        //动态加载布局
//        view = LayoutInflater.from(context).inflate(R.layout.view_selfdialog,null);

        view = getLayoutInflater().inflate(R.layout.view_selfdialog,null);

        tv_dialogTitle = (TextView) view.findViewById(R.id.tv_dialogTitle);
        editUserName = (EditText) view.findViewById(R.id.editUserName);
        editPassword = (EditText) view.findViewById(R.id.editPassword);
        cbServiceItem = (CheckBox) view.findViewById(R.id.cbServiceItem);
        builder.setView(view);


        //设置登录按钮(btnLogin)的监听事件
        view.findViewById(R.id.btnLogin).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                loginListener.onClick(v);
            }
        });

//        builder.create();
          dialog = builder.show();
    }

    //getter
    public EditText getEditPassword() {
        return editPassword;
    }

    public EditText getEditUserName() {
        return editUserName;
    }

    public CheckBox getCbServiceItem() {
        return cbServiceItem;
    }

    //设置LoginDialog的标题是否显示
    public void setShowTitle(boolean isShowTitle){
        if (isShowTitle)
            tv_dialogTitle.setVisibility(TextView.VISIBLE);
        else
            tv_dialogTitle.setVisibility(TextView.INVISIBLE);
    }

    //显示LoginDialog
    public void show(){
        builder.show();
    }


    //自定义onLoginListener接口
    public interface onLoginListener{
        void onClick(View v);
    }

    //自定义setLoginListener
    public void setLoginListener(onLoginListener loginListener){
        this.loginListener = loginListener;
    }

    @Override
    public void dismiss() {
        dialog.dismiss();
    }
}