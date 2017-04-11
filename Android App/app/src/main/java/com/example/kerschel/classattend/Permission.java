package com.example.kerschel.classattend;

import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.CountDownTimer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

/**
 * Created by kersc on 3/25/2017.
 */

public class Permission {

    public static final int RECORD_PERMISSION_REQUEST_CODE = 1;
    public static final int EXTERNAL_STORAGE_PERMISSION_REQUEST_CODE = 2;
    public static final int CAMERA_PERMISSION_REQUEST_CODE = 3;
    MainActivity activity;

    public Permission(MainActivity activity) {
        this.activity = activity;
    }


    public boolean checkPermissionForCamera(){
        int result = ContextCompat.checkSelfPermission(activity, android.Manifest.permission.CAMERA);
        if (result == PackageManager.PERMISSION_GRANTED){
            return true;
        } else {
            return false;
        }
    }




    public void requestPermissionForCamera(){

        if (ContextCompat.checkSelfPermission(activity,
                android.Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                    android.Manifest.permission.CAMERA)) {

            }else{
                ActivityCompat.requestPermissions(activity,
                        new String[]{android.Manifest.permission.CAMERA},
                        101);
            }
        }




            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, android.Manifest.permission.CAMERA)){
            ActivityCompat.requestPermissions(activity,
                    new String[]{android.Manifest.permission.CAMERA},
                    101);

            Toast.makeText(activity, "Camera permission needed. Please allow in App Settings for additional functionality.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(activity,new String[]{android.Manifest.permission.CAMERA},CAMERA_PERMISSION_REQUEST_CODE);
        }
    }



    public void display(String content, Context cont) {
        AlertDialog.Builder builder = new AlertDialog.Builder(cont);
        builder.setTitle("Scan result");
        builder.setMessage(content);
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        new CountDownTimer(950, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onFinish() {
                // TODO Auto-generated method stub

                alertDialog.dismiss();

            }
        }.start();
    }

}