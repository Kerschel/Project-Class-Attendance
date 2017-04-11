package com.example.kerschel.classattend;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/**
 * Created by kersc on 2/28/2017.
 */

public class Course {
    private String startTime;
    private String endTime;
    private String courseCode;


    public String getstartTime(){
        return this.startTime;
    }

    public String getendTime() {
        return this.endTime;
    }

    public String getcourseCode() {
        return courseCode;
    }

    public static class Barcode extends AppCompatActivity {

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.details);

        }



    }
}
