package com.ssong.api.myparking;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.gun0912.tedpermission.PermissionListener;

import java.util.ArrayList;

/**
 * Created by ssong on 2017-09-05.
 */

public class PermissionActivity extends AppCompatActivity {

    ParkingMain beforeAct = (ParkingMain) ParkingMain.beforeAct;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        PermissionListener permissionlistener = new PermissionListener() {
            @Override
            public void onPermissionGranted() {
                // 권한 요청
                Intent intent = new Intent(PermissionActivity.this, ParkingMain.class);
                intent.putExtra("permission",true);
                startActivity(intent);
                beforeAct.finish();
            }

            @Override
            public void onPermissionDenied(ArrayList<String> deniedPermissions) {
         //       Toast.makeText(PermissionActivity.this, "권한 거부\n" + deniedPermissions.toString(), Toast.LENGTH_SHORT).show();
            }
        };

        com.gun0912.tedpermission.TedPermission.with(this)
                .setPermissionListener(permissionlistener)
                .setRationaleTitle(R.string.rationale_title)
                .setRationaleMessage(R.string.rationale_message)
                //.setDeniedTitle("권한 거부")
                //.setDeniedMessage("당신이 이 권한을 거부했다면 서비스를 이용할 수 없습니다.\n\n앱설정 권한설정으로 가서 권한을 주시기 바랍니다.")
                //.setGotoSettingButtonText("설정으로 이동")
                .setPermissions(
                          Manifest.permission.ACCESS_COARSE_LOCATION
                        , Manifest.permission.ACCESS_FINE_LOCATION
                        // , Manifest.permission.READ_EXTERNAL_STORAGE
                        // , Manifest.permission.WRITE_EXTERNAL_STORAGE

                )
                .check();
        finish();
    }

}

