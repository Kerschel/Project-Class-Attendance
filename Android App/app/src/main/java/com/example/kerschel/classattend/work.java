package com.example.kerschel.classattend;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.Result;
import me.dm7.barcodescanner.zxing.ZXingScannerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;


/**
 * Created by Kerschel James on 2/10/2017.
 */
//


public class work extends Activity implements ZXingScannerView.ResultHandler {
    String classes,barClass,Teach;

    MediaPlayer cameraSoundWrong,cameraSound;
    TextView barCourse,deny,granted,startT,endT,shows;
    ListView listView;

    String classroom;
    Spinner roomSpin;
    ArrayAdapter<CharSequence>roomAdapter;


    EditText course;
    ArrayList<String> mylist = new ArrayList<String>();

    String[] items;
    ArrayList<String> listItems;
    ArrayAdapter<String> adapter;
    private ZXingScannerView mScannerView;

    //Times used for setting timestamp
    String time = (String) android.text.format.DateFormat.format("h:mm", new Date());
    final String hr = (String) android.text.format.DateFormat.format("h", new Date());
    String month = (String) android.text.format.DateFormat.format("MMMM", new Date());
    String year = (String) android.text.format.DateFormat.format("yyyy", new Date());
    String day = (String) android.text.format.DateFormat.format("dd", new Date());
    String loc =year + "/" + Teach + "/" + month + "/" + day  ;

    //    Firebase References
    DatabaseReference fire = FirebaseDatabase.getInstance().getReference();
    DatabaseReference refCourse = FirebaseDatabase.getInstance().getReference().child("class");



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        refCourse.keepSynced(true);



//                 Checking camera permission


        show();
        populateList();
        courseSearch();

        cameraSound = MediaPlayer.create(this, R.raw.sound);

        cameraSoundWrong = MediaPlayer.create(this, R.raw.beep);

        //        Syncs firebase data to device for offline usage


    }




    public void populateList() {
        listView = (ListView) findViewById(R.id.listview);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                course.setText(listView.getItemAtPosition(i).toString());
                Teach = course.getText().toString();
                setContentView(R.layout.details);

                times();

            }
        });

    }




    public void courseSearch() {
        course = (EditText) findViewById(R.id.txtsearch);

        course.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }


            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().equals("")) {
                    // reset listview
                    initList(mylist);
                } else {
                    // perform search

                    searchItem(s.toString());
                }
            }

            public void afterTextChanged(Editable s) {
            }
        });
    }



    public void show() {
        refCourse.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot alert : dataSnapshot.getChildren()) {
//                           Adds the courses to a array to be displayed
//                            mylist.add(alert.getValue().toString());
                            if(!mylist.contains(alert.child("courseCode").getValue().toString()))
                                mylist.add(alert.child("courseCode").getValue().toString());
                        }
                        initList(mylist);

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //handle databaseError
                    }
                });

    }

    public void searchItem(String textToSearch) {

        for (String item : items) {

            if (!item.toLowerCase().contains(textToSearch.toLowerCase())) {

                listItems.remove(item);

            }

        }

        adapter.notifyDataSetChanged();

    }


    public void initList(ArrayList<String> mylist) {// Displays the list to be seen
        System.out.println(mylist);

        items = mylist.toArray(new String[0]);
        listItems = new ArrayList<>(Arrays.asList(items));

        adapter = new ArrayAdapter<String>(this,
                R.layout.list_item, R.id.txtitem, listItems);

        listView.setAdapter(adapter);

    }


    public void display(String content) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
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

    public void onClick(View v) {
        int flag = 0;
//        try {
//                              flag=  countDown();
//                            } catch (ParseException e) {
//                                e.printStackTrace();
//                            }
//        if(flag == 1) {

        mScannerView = new ZXingScannerView(getApplicationContext());
        setContentView(mScannerView);
        LayoutInflater inflater = getLayoutInflater();
        getWindow().addContentView(inflater.inflate(R.layout.screenbar, null),
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.FILL_PARENT));
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
        barCourse = (TextView) findViewById(R.id.barCourse);
        barCourse.setText(barClass);
        granted = (TextView) findViewById(R.id.grant);
        deny = (TextView) findViewById(R.id.denied);
        granted.setVisibility(View.GONE);
        deny.setVisibility(View.GONE);


//        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result result) {
        searchFire(result.getText().toString());
        //Resume scanning
        mScannerView.resumeCameraPreview(this);
    }


    public void accessDenied(){
        deny = (TextView) findViewById(R.id.denied);
        granted = (TextView) findViewById(R.id.grant);
        deny.setVisibility(View.VISIBLE);
        new CountDownTimer(900, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onFinish() {
                // TODO Auto-generated method stub

                deny.setVisibility(View.GONE);

            }
        }.start();

    }


    public void accessGranted(){
        granted = (TextView) findViewById(R.id.grant);
        granted.setVisibility(View.VISIBLE);
        new CountDownTimer(900, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onFinish() {
                // TODO Auto-generated method stub

                granted.setVisibility(View.GONE);

            }
        }.start();

    }

    DatabaseReference item = FirebaseDatabase.getInstance().getReference().child(loc);

    public void searchFire(final String barcode) {

        time = (String) android.text.format.DateFormat.format("h:mm", new Date());
        month = (String) android.text.format.DateFormat.format("MMMM", new Date());
        year = (String) android.text.format.DateFormat.format("yyyy", new Date());
        day = (String) android.text.format.DateFormat.format("dd", new Date());
        loc =year + "/" + Teach + "/" + month + "/" + day  ;
        item = FirebaseDatabase.getInstance().getReference().child(loc);
        item.keepSynced(true);
        state =0;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("Students/" +Teach);
        ref.keepSynced(true);
        ref.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        //Get map of users in datasnapshot
                        for (DataSnapshot alert : dataSnapshot.getChildren()) {

                            String ID = alert.child("studentID").getValue().toString();
                            Log.w("Student",ID);
                            Log.w("StudentB",barcode);
                            if (ID.equals(barcode)) {
                                state =1;
                                Log.w("OPP",barcode);

                                item.addListenerForSingleValueEvent(

                                        new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                int getout =0;

                                                for (DataSnapshot data :dataSnapshot.getChildren()){

                                                    if(data.getKey().equals(barcode)){
//                                                        accessGranted();
                                                        display("Already Recorded");
                                                        state = 1;
                                                        getout=1;
                                                        return;
                                                    }
                                                }

                                                if(getout==0) {
                                                    accessGranted();
//                                                display("Registered");
                                                    cameraSound.start();
                                                    fire.child(loc + "/" + barcode).setValue(time);
                                                }
                                            }

                                            @Override
                                            public void onCancelled(DatabaseError databaseError) {
                                                //handle databaseError
                                            }
                                        });

                            }

                        }

                        if(state !=1) {
                            cameraSoundWrong.start();
                            accessDenied();
//                            display("Not in class");
                        }

                    }


                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //handle databaseError
                    }
                });

    }



    public int countDown() throws ParseException { // Counts down on how long until you can begin registration
        time = (String) android.text.format.DateFormat.format("h:mm", new Date());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        Date date1 = simpleDateFormat.parse(time);
        Date date2 = simpleDateFormat.parse(startT.getText().toString());
        long difference = date2.getTime() - date1.getTime();
        int days = (int) (difference / (1000*60*60*24));
        int hours = (int) ((difference - (1000*60*60*24*days)) / (1000*60*60));
        int  min = (int) (difference - (1000*60*60*24*days) - (1000*60*60*hours)) / (1000*60);
        int total = days*1440  + (hours * 60) + min;

        if(total > 10) {
            Toast.makeText(this,hours + "Hours" + "and "+ min + "More minutes left", Toast.LENGTH_SHORT).show();
            return 0;
        }
        return 1;
    }

    public void times() {
        DatabaseReference refs = FirebaseDatabase.getInstance().getReference().child("class");
        refs.keepSynced(true);
        refs.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        int prev = 99;
                        startT = (TextView) findViewById(R.id.start);
                        endT = (TextView) findViewById(R.id.end);
                        shows = (TextView) findViewById(R.id.ShowCourse);
                        for (DataSnapshot teach: dataSnapshot.getChildren()) {
                            classes = teach.child("courseCode").getValue().toString();

                            if(classes.equals(Teach)){

                                String start = teach.child("startTime").getValue().toString();
                                String end = teach.child("endTime").getValue().toString();
                                int s = Integer.valueOf(start.split(":")[0]);
                                int now = Integer.valueOf(hr);
                                if(Math.abs(now - s) < Math.abs(now -prev)){
                                    prev = now;
                                    startT.setText(start);
                                    endT.setText(end);
                                    shows.setText(classes);
                                    barClass = shows.getText().toString();
                                }
                            }

                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //handle databaseError
                    }
                });
    }



    public int state = 0;


}
