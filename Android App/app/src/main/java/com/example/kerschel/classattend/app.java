package com.example.kerschel.classattend;

import android.app.Application;

import com.google.firebase.database.FirebaseDatabase;

/**
 * Created by kersc on 3/25/2017.
 */

public class app extends Application {

    @Override
    public void onCreate(){
        super.onCreate();
        FirebaseDatabase.getInstance().setPersistenceEnabled(true);

    }

}
