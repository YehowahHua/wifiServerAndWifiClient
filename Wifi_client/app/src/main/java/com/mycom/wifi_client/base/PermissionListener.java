package com.mycom.wifi_client.base;

import java.util.List;

/**
 * Created by user on 2018/1/8.
 */

public interface PermissionListener {
    void onGranted();//权限申请后
    void onDenied(List<String> deniedPermission);
}
