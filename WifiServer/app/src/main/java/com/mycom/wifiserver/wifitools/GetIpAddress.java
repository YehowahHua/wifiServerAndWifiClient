package com.mycom.wifiserver.wifitools;

import android.util.Log;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.util.Enumeration;

public class GetIpAddress {
	
	public static String IP;
	public static int PORT;
	
	public static String getIP(){
		return IP;
	}
	public static int getPort(){
		return PORT;
	}
	public static void getLocalIpAddress(ServerSocket serverSocket){

		try {
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();){
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses();	enumIpAddr.hasMoreElements();){
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    String mIP = inetAddress.getHostAddress().substring(0, 3);
                    if(mIP.equals("192")){
                        IP = inetAddress.getHostAddress();    //获取本地IP 更改更改来来来
						PORT = serverSocket.getLocalPort();    //获取本地的PORT 更改更改  幸幸
                        Log.e("IP",""+IP);
                        Log.e("PORT",""+PORT);
                    }
                }
            }
		} catch (SocketException e) {
			e.printStackTrace();
		}

	}
}
