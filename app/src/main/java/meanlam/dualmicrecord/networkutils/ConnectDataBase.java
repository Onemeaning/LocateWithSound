package meanlam.dualmicrecord.networkutils;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class ConnectDataBase {

    public static String IP ;
    public  static String PORT ;
    private static  String DBNAME = "data";
    private static  String tableName = "data";
    private static String USER = "root";
    private static String PWD = "root";
    private static Context context;
    private static String userName,tdoa;
    public static  List<String> todaList = new ArrayList<String>();
    public static  List<String> nameList = new ArrayList<String>();


    public ConnectDataBase(String IP, String port, Context context) {
        this.IP = IP;
        this.PORT = port;
        this.context = context;
    }

    /** 创建数据库对象 */
    public static Connection getSQLConnection() {
        Connection con = null;
        try {
            Class.forName("com.mysql.jdbc.Driver");//不同的数据库这里的内容将会不一样
            con = DriverManager.getConnection("jdbc:mysql://" + IP + ":" + PORT + "/" + DBNAME ,USER, PWD);//不同的数据库这里的内容将会不一样
        } catch (ClassNotFoundException e) {
            Log.i("Onemeaning", "ClassNotFoundException:: " + e.toString());
            e.printStackTrace();
        } catch (SQLException e) {
            Log.i("Onemeaning", "SQLException:: " + e.toString());
            e.printStackTrace();
        }
        return con;
    }

    public static String query(Connection newCon)
    {
        String result = "";
        try
        {
            Connection conn = newCon;
            Log.i("Onemeaning", "连接到数据库中");
            String sql = "select * from " + tableName;
            Statement stmt = conn.createStatement();
            ResultSet resultSet = stmt.executeQuery(sql);
            result += "Connect Success \n";
            todaList.clear();
            nameList.clear();

            while(resultSet.next())
            {
                userName = resultSet.getString("name");
                tdoa = String.valueOf(resultSet.getDouble("Tdoa"));
                todaList.add(tdoa);
                nameList.add(userName);
                result += "TDOA:  " + tdoa + "\n";
            }
            resultSet.close();
            stmt.close();
            conn.close();
        }
        catch (Exception e)
        {
            Log.i("Onemeaning", e.toString());
            result = "Connect Error";
        }
        return  result;
    }

    public static  boolean insert(Connection connection)
    {
        try{
            Connection con = connection;
            String sql="INSERT INTO user(userName,password,music,tdoa) VALUES (?,?,?,?)";
            PreparedStatement pps=con.prepareStatement(sql);

            pps.setString(1, "mic20191120101121.wav");
            pps.setString(2, "573854");
            pps.setDouble(2,3.451*Math.pow(10,-4));

            byte [] bytes= IOUtils.toByteArray(new FileInputStream((Environment
                    .getExternalStorageDirectory() + "/record/" + "mic20181115170629.wav")));

        }
        catch (Exception e)
        {

            Log.i("INSERT", e.toString());
            return  false;
        }
        return  true;
    }



}
