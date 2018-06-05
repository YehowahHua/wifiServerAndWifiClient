package com.mycom.wifiserver.base;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 2018/1/8.
 */

public class BaseActivity extends AppCompatActivity {
    private static PermissionListener mListener;
    private Context mContext;
    
    
    /**
     * 获取当前Activity上下文
     * @return
     */
    public Context getContext() {
        return mContext;
    }


    //出现错误，使得崩溃，protected被修改为public
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext=this;
        ActivityCollector.addActivity(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ActivityCollector.removeActivity(this);
    }

    /**
     * 请求权限，没有得到许可的要授权
     * @param permissions 要申请的权限
     * @param listener 回调监听
     */
    public static void requestRuntimePermission(String[] permissions, PermissionListener listener){
        Activity topActivity = ActivityCollector.getTopActivity();
        if(topActivity == null){
            return;
        }
        mListener = listener;
        List<String> permissionList = new ArrayList<>();//

        for (String permission:permissions) {
            if(ContextCompat.checkSelfPermission(topActivity, permission)!= PackageManager.PERMISSION_GRANTED){//this-->>topActivity
                permissionList.add(permission);//添加权限
            }
        }
        if (!permissionList.isEmpty()){
            //请求权限
            ActivityCompat.requestPermissions(topActivity, permissionList.toArray(new String[permissionList.size()]), 1);
        }else{
            //授权了之后，没有权限限制时
            mListener.onGranted();
        }
    }

    /**
     * 监听的授权的回调方法
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
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
