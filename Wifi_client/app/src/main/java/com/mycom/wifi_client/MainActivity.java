package com.mycom.wifi_client;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.mycom.wifi_client.base.ActivityCollector;
import com.mycom.wifi_client.base.BaseActivity;
import com.mycom.wifi_client.base.PermissionListener;
import com.mycom.wifi_client.manager.ThreadPoolManager;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
    private static PermissionListener mListener;

    private EditText et_client_socket1;
    private Button sendBtn1;

//    private EditText et_client_socket2;
    private Button sendBtn2;

    private Button linkTcpServerBt;
    private TextView receiveDataTv;



    private Socket mClientSocket;
    private PrintWriter mPrintWriter;
    private SocketHandler mSocketHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendBtn1 = (Button) findViewById(R.id.send1);
        sendBtn2 = (Button) findViewById(R.id.send2);
        linkTcpServerBt = (Button) findViewById(R.id.linkTcpServerBt);
        et_client_socket1 = (EditText) findViewById(R.id.et_client_socket1);
//        et_client_socket2 = (EditText) findViewById(R.id.et_client_socket2);
        receiveDataTv = (TextView) findViewById(R.id.receiveDataTv);

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

        linkTcpServerBt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //1.调用 Socket() 创建一个流套接字，连接到服务端
                //2.调用 Socket 类的 getOutputStream() 和 getInputStream() 获取输出和输入流，进行网络数据的收发
                //3.关闭套接字
                bindSocketService();
            }
        });

        /**
         * 发送720P
         */
        sendBtn2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String msg = "19700108084745_60.mp4";    //et_client_socket2.getText().toString();
                if (!TextUtils.isEmpty(msg) && mPrintWriter != null) {
                    //发送数据，这里注意要在线程中发送，不能在主线程进行网络请求，不然就会报错
                    ThreadPoolManager.getInstance().addTask(new Runnable() {
                        @Override
                        public void run() {
                            mPrintWriter.println(msg);//向socket中写入数据，发送给服务端Arrays.toString(msg.getBytes("utf-8"))
                            Log.i(TAG, "发送数据");
                        }
                    });
                    et_client_socket1.setText("19700108084745_60.mp4");//
//                    mTvSocketMessage.setText(mTvSocketMessage.getText() + "\n" + DateUtils.getCurrentTime() + "\nclient : " + msg);
                }
            }
        });

        /**
         * 发送1080P
         */
        sendBtn1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String msg = "20180312164519_180.mp4";// et_client_socket1.getText().toString(); //mEtClientSocket.getText().toString();

//                OutputStream outputStream = null;
//                try {
//                    outputStream = mClientSocket.getOutputStream();
//                    outputStream.write(msg.getBytes());//"utf-8"
//                    outputStream.flush();
//                    outputStream.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }




                if (!TextUtils.isEmpty(msg) && mPrintWriter != null) {
                    //发送数据，这里注意要在线程中发送，不能在主线程进行网络请求，不然就会报错
                    ThreadPoolManager.getInstance().addTask(new Runnable() {
                        @Override
                        public void run() {
                            mPrintWriter.println(msg);//向socket中写入数据，发送给服务端Arrays.toString(msg.getBytes("utf-8"))
                            Log.i(TAG, "发送数据");
                        }
                    });
                    et_client_socket1.setText("20180312164519_180.mp4");//20180312093532_240.mp4
//                    mTvSocketMessage.setText(mTvSocketMessage.getText() + "\n" + DateUtils.getCurrentTime() + "\nclient : " + msg);
                }
            }
        });
    }


    private void bindSocketService() {

        mSocketHandler = new SocketHandler();
        new Thread(new Runnable() {    //新开一个线程连接、接收数据
            @Override
            public void run() {
                try {
                    connectSocketServer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }
    /**
     * 通过 Socket 连接服务端
     */
    private void connectSocketServer() throws IOException {
        Socket socket = null;
        while (socket == null) {    //选择在循环中连接是因为有时请求连接时服务端还没创建，需要重试
            try {
                socket = new Socket("192.168.43.1", 8086);
                mClientSocket = socket;
                mPrintWriter = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);//



            } catch (IOException e) {
                SystemClock.sleep(1000);
            }
        }

        //连接成功
        mSocketHandler.sendEmptyMessage(SocketHandler.CODE_SOCKET_CONNECT);

        //获取输入流
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        while (!isFinishing()) {    //死循环监听服务端发送的数据
            final String msg = in.readLine();//从服务端获取数据
            if (!TextUtils.isEmpty(msg)) {

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");// HH:mm:ss
                //获取当前时间
                Date date = new Date(System.currentTimeMillis());
                String time = simpleDateFormat.format(date);

                //数据传到 Handler 中展示
                mSocketHandler.obtainMessage(SocketHandler.CODE_SOCKET_MSG,
                        "\n" + time + "\nserver : " + msg)
                        .sendToTarget();
            }
            SystemClock.sleep(1000);
        }

        System.out.println("Client quit....");


        mPrintWriter.close();
        in.close();
        socket.close();
    }

    /**
     * 处理 Socket 线程切换
     */
    @SuppressWarnings("HandlerLeak")
    public class SocketHandler extends Handler{
        public static final int CODE_SOCKET_CONNECT = 1;
        public static final int CODE_SOCKET_MSG = 2;

        @Override
        public void handleMessage(final Message msg) {
            switch (msg.what) {
                case CODE_SOCKET_CONNECT:
                    sendBtn1.setEnabled(true);
                    break;
                case CODE_SOCKET_MSG:
//                    et_client_socket.setText(et_client_socket.getText() + (String) msg.obj);
                    receiveDataTv.setText(et_client_socket1.getText() + (String) msg.obj);
                    break;
            }
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

}
