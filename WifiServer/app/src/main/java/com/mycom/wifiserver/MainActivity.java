package com.mycom.wifiserver;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mycom.wifiserver.base.ActivityCollector;
import com.mycom.wifiserver.base.BaseActivity;
import com.mycom.wifiserver.base.PermissionListener;
import com.mycom.wifiserver.manager.ThreadPoolManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private static final String TAG = "check_permission";

    private static PermissionListener mListener;
    private Button open,close,search,send;
    private WifiManager wifiManger;
    private TextView name;

    private ServerSocket serverSocket = null;



    private boolean mIsServiceDisconnected;
    /**
     * 热点名称
     */
    private static final String WIFI_HOTSPOT_SSID = "WifiHotSpot";
    private static final String WIFI_HOTSPOT_PASSWORD = "66668888";
    /**
     * 端口号
     */
    private static final int SERVER_PORT = 8000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        open = (Button) findViewById(R.id.createWifi);
        close = (Button) findViewById(R.id.closeWifi);
        send = (Button) findViewById(R.id.sendM);
        name = (TextView) findViewById(R.id.name);
        wifiManger = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        checkPermission(new String[]{

                //连接wifi权限
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                //创建热点权限
                //Manifest.permission.WRITE_SETTINGS,
        }, new PermissionListener() {
            @Override
            public void onGranted() {
                Toast.makeText(MainActivity.this, "权限已经同意", Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onDenied(List<String> deniedPermission) {
                for(String deniedPerm:deniedPermission) {
                    Toast.makeText(MainActivity.this, "被拒绝的权限：" + deniedPerm, Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    public void createWifi(View v)
    {
        createWifiHotspot();
    }
    public void closeWifi(View v)
    {
        closeWifiHotSpot();
    }

    public void openSocket(View v)
    {
        String ip = getlocalip();
        Log.d(TAG, "IP:" + ip);
        new Thread(new TCPServer()).start();
    }

    private class TCPServer implements Runnable{
        @Override
        public void run() {
            //1.调用 ServerSocket(int port) 创建一个 ServerSocket，绑定到指定端口
            //2.调用 accept() 监听连接请求，如果客户端请求连接则接受，返回通信套接字
            //3.调用 Socket 类的 getOutputStream() 和 getInputStream() 获取输出和输入流，进行网络数据的收发
            //4.关闭套接字
            ServerSocket serverSocket;
            try {
                serverSocket  = new ServerSocket(SERVER_PORT);
                Log.i(TAG, "TCP 服务已经创建");
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println( "TCP 创建失败");
                return;
            }

            while(!mIsServiceDisconnected){
                try {
                    Socket client = serverSocket.accept();//接受客户端消息，阻塞直到收到消息
                    //使用线程池
//                    new Thread(responseClient(client)).start();
                    ThreadPoolManager.getInstance().addTask(responseClient(client));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //在这里接受和回复客户端消息
    private Runnable responseClient(final Socket client){
        return new Runnable() {
            @Override
            public void run() {
                //接受消息
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));//接受消息
                    PrintWriter out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(client.getOutputStream())),true);//回复消息
                    out.println("服务器已连接xxx");

                    while(!mIsServiceDisconnected){
                        String inputStr = in.readLine();//得到客户端的数据
                        Log.i(TAG, "收到客户端的消息： "+inputStr);

                        if (TextUtils.isEmpty(inputStr)){
                            Log.i(TAG, "收到消息为空，客户端断开连接xxx");
                            break;
                        }
                        out.println("服务端发过来的："+inputStr+"");//服务作出回应，发送给客户端
                    }

                    out.close();
                    in.close();
                    client.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
    }




    /**
     * 或取本机的ip地址
     */
    private String getlocalip(){
        WifiInfo wifiInfo = wifiManger.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        //  Log.d(Tag, "int ip "+ipAddress);
        if(ipAddress==0)return null;
        return ((ipAddress & 0xff)+"."+(ipAddress>>8 & 0xff)+"."
                +(ipAddress>>16 & 0xff)+"."+(ipAddress>>24 & 0xff));
    }




    /*当按返回键时，关闭相应的socket资源*/
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //创建wifi
    private void createWifiHotspot()
    {
        if(wifiManger.isWifiEnabled())
        {
            //如果wifi处于打开状态，则关闭wifi,
            wifiManger.setWifiEnabled(false);
        }
        WifiConfiguration config = new WifiConfiguration();
        config.SSID = WIFI_HOTSPOT_SSID;
        config.preSharedKey = WIFI_HOTSPOT_PASSWORD;
        config.hiddenSSID = true;
        config.allowedAuthAlgorithms
                .set(WifiConfiguration.AuthAlgorithm.OPEN);//开放系统认证
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
        config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
        config.allowedPairwiseCiphers
                .set(WifiConfiguration.PairwiseCipher.TKIP);
        config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
        config.allowedPairwiseCiphers
                .set(WifiConfiguration.PairwiseCipher.CCMP);
        config.status = WifiConfiguration.Status.ENABLED;
        //通过反射调用设置热点
        try {
            Method method =wifiManger.getClass().getMethod(
                    "setWifiApEnabled", WifiConfiguration.class, Boolean.TYPE);
            boolean enable = (Boolean) method.invoke(wifiManger, config, true);
            if (enable) {
                name.setText("热点已开启 SSID:" + WIFI_HOTSPOT_SSID + " password:" + WIFI_HOTSPOT_PASSWORD);
                String ip = getlocalip();
                Log.d(TAG, "IP:" + ip);
            } else {
                name.setText("创建热点失败");
            }
        } catch (Exception e) {
            e.printStackTrace();
            name.setText("创建热点失败");
        }
    }
    //关闭wifi
    public void closeWifiHotSpot()
    {
        try {
            Method method = wifiManger.getClass().getMethod("getWifiApConfiguration");
            method.setAccessible(true);
            WifiConfiguration config = (WifiConfiguration) method.invoke(wifiManger);
            Method method2 = wifiManger.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
            method2.invoke(wifiManger, config, false);
        } catch (NoSuchMethodException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private void checkPermission(String [] permissions,PermissionListener listener) {
        Activity topActivity = ActivityCollector.getTopActivity();
        if(topActivity == null){
            return;
        }
        mListener = listener;
        List<String> permissionList = new ArrayList<>();

        for (String permission:permissions) {
            if(ContextCompat.checkSelfPermission(this, permission)!= PackageManager.PERMISSION_GRANTED){//this-->>topActivity
                permissionList.add(permission);//添加权限
                Log.d(TAG, "checkPermission");
            }
        }
        if (!permissionList.isEmpty()){
            //请求权限
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]), 1);
        }else{
            //授权了之后，没有权限限制时
            mListener.onGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        //实现回调
        switch (requestCode){
            case 1:
                if(grantResults.length>0){
                    List<String> deniedPermissions=new ArrayList<>();//存放没授权的权限
                    for (int i = 0; i <grantResults.length ; i++) {
                        int grantResult = grantResults[i];
                        String permission = permissions[i];
                        if (grantResult != PackageManager.PERMISSION_GRANTED){
                            deniedPermissions.add(permission);//得到所有的没有授权的权限
                        }
                    }
                    if(deniedPermissions.isEmpty()){
                        mListener.onGranted();//说明都授权了
                    }else {
                        mListener.onDenied(deniedPermissions);
                    }
                }
                break;
            default:
                break;
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
    }
    @Override
    protected void onDestroy() {
        mIsServiceDisconnected = true;
        super.onDestroy();
    }

}
