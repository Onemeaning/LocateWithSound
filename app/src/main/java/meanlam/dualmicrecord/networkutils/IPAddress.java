package meanlam.dualmicrecord.networkutils;

import android.app.Application;

public class IPAddress extends Application {
    public   String IP  ;
    public   String PORT ;


    //实现setname()方法，设置变量的值
    public  void setIP(String ip) {
        this.IP = ip;
    }

    public void setPORT(String PORT) {
        this.PORT = PORT;
    }

    //实现getname()方法，获取变量的值
    public  String getIP() {
        return this.IP;
    }

    public  String getPORT() {
        return this.PORT;
    }
}
