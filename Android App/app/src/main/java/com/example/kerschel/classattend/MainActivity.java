package com.example.kerschel.classattend;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
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

/***
 * This function is the main activity class it performs actions when activity created
 * ***/
public class MainActivity extends Activity implements ZXingScannerView.ResultHandler {
    String classes, barClass, selectedCourse;
    Button btnExit, backCourse;
    MediaPlayer cameraSoundWrong, cameraSound;
    TextView barCourse, deny, granted, startT, endT, shows,dayofWeek;
    ListView listView;
    String classroom;
    Spinner roomSpin;
    ArrayAdapter<CharSequence> roomAdapter;
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
    String loc = year + "/" + selectedCourse + "/" + month + "/" + day;

    //    Firebase References
    DatabaseReference fire = FirebaseDatabase.getInstance().getReference();
    DatabaseReference refCourse = FirebaseDatabase.getInstance().getReference().child("class");

    @Override
    public void onBackPressed() {
        Toast.makeText(getActivity(), "Click the Arrow!", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        classPage();
        //        Syncs firebase data to device for offline usage
        refCourse.keepSynced(true);


//                 Checking camera permission
        Permission permissionCheck = new Permission(this);
        if (!permissionCheck.checkPermissionForCamera())
            permissionCheck.requestPermissionForCamera();

//        Sounds for when accept or deny ID card
        cameraSound = MediaPlayer.create(this, R.raw.sound);
        cameraSoundWrong = MediaPlayer.create(this, R.raw.beep);

    }


/*=====================================ClassRoom page fucntions============================================*/
    // Brings classroom.xml page to front (Homescreen)
//    This design had to be used as Zxing does not support using Intent with the device tested on :Huawei P9
    public void classPage() {
        LayoutInflater inflater = getLayoutInflater();
        getWindow().addContentView(inflater.inflate(R.layout.classroom, null),
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.FILL_PARENT,
                        ViewGroup.LayoutParams.FILL_PARENT));

        btnExit = (Button) findViewById(R.id.btnExit);
        btnExit.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        throw new RuntimeException("Stub!");
                    }
                }
        );


//        Gives drop down menu to select room for the course
        roomSpin = (Spinner) findViewById(R.id.roomSpin);
        roomAdapter = ArrayAdapter.createFromResource(this, R.array.classes, android.R.layout.simple_spinner_item);
        roomAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roomSpin.setAdapter(roomAdapter);
        roomSpin.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                classroom = roomSpin.getItemAtPosition(i).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

//        This is how we used the alternative to Intent to switch pages
        Button mainpage = (Button) findViewById(R.id.buttonCourse);
        mainpage.setOnClickListener(
                new Button.OnClickListener() {
                    public void onClick(View v) {
                        setContentView(R.layout.activity_main);
                        populateList();// initializes the listview with the courses
                        courseSearch();
                        //This button takes user back to classroom select page
                        backCourse = (Button) findViewById(R.id.backCourse);
                        backCourse.setOnClickListener(
                                new Button.OnClickListener() {
                                    public void onClick(View v) {
                                        ViewGroup vg = (ViewGroup) (v.getParent());
                                        vg.removeView(v);
                                        classPage();

                                    }
                                }
                        );

                    }
                }
        );
    }




/*=============================Course Selection page functions===========================================*/

    public void populateList() {
        addCourse();
        listView = (ListView) findViewById(R.id.listview);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            course.setText(listView.getItemAtPosition(i).toString());
            selectedCourse = course.getText().toString();
            setContentView(R.layout.details);
            times();
            }
        });
    }


    //The search bar allows filtering of data
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


    //   Populates an array of coursecodes to be selected for the class
    public void addCourse() {
        refCourse.addListenerForSingleValueEvent(
            new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    for (DataSnapshot alert : dataSnapshot.getChildren()) {
    //                           Adds the courses to a array to be displayed
                        if (alert.child("room").getValue().toString().equals(classroom)) {
                            if (!mylist.contains(alert.child("courseCode").getValue().toString()))
                                mylist.add(alert.child("courseCode").getValue().toString());
                        }
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

        adapter = new ArrayAdapter<String>(this, R.layout.list_item, R.id.txtitem, listItems);
        listView.setAdapter(adapter);

    }

/*==================Course Information Page===========================*/
    //    This is for the Zxing scanner activity when we are ready to start scanning barcodes
    @Override
    protected void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }

    @Override
    public void handleResult(Result result) {// result is the barcode
        searchForStudent(result.getText().toString());
        //Resume scanning
        mScannerView.resumeCameraPreview(this);
    }

    DatabaseReference item = FirebaseDatabase.getInstance().getReference().child(loc);
    public int state = 0;

    //    On the detais.xml page the class information is displayed
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
                        dayofWeek = (TextView)findViewById(R.id.Day);
                        shows = (TextView) findViewById(R.id.ShowCourse);
                        for (DataSnapshot teach : dataSnapshot.getChildren()) {
                            classes = teach.child("courseCode").getValue().toString();

                            if (classes.equals(selectedCourse)) {
                                String start = teach.child("startTime").getValue().toString();
                                String end = teach.child("endTime").getValue().toString();
                                String day = teach.child("dayOfWeek").getValue().toString();
                                // Splits the time to get closest hour to current hour
                                int classHr = Integer.valueOf(start.split(":")[0]);
                                int nowhr = Integer.valueOf(hr);
                                // gets the closest class start time for a course to display
                                if (Math.abs(nowhr - classHr) < Math.abs(nowhr - prev)) {
                                    prev = nowhr;
                                    startT.setText(start);
                                    endT.setText(end);
                                    dayofWeek.setText(day);
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


    public void accessDenied() {
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


    public void accessGranted() {
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

//This was implemented only for COMP 3150 to enable better testing of the other courses
    public int countDown() throws ParseException { // Counts down on how long until you can begin registration
        time = (String) android.text.format.DateFormat.format("h:mm", new Date());
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");
        Date date1 = simpleDateFormat.parse(time);
        Date date2 = simpleDateFormat.parse(startT.getText().toString());
        startT.getText().toString().split(":");
        long difference = date2.getTime() - date1.getTime();
        int days = (int) (difference / (1000 * 60 * 60 * 24));
        int hours = (int) ((difference - (1000 * 60 * 60 * 24 * days)) / (1000 * 60 * 60));
        int min = (int) (difference - (1000 * 60 * 60 * 24 * days) - (1000 * 60 * 60 * hours)) / (1000 * 60);
        int total = days * 1440 + (hours * 60) + min;

        if (total > 10) {
            Toast.makeText(this, min + " More minutes left", Toast.LENGTH_SHORT).show();
            return 0;
        }
        return 1;
    }

/*=====================================Barcode scanner Page============================================*/
//This function searches the database to see if the barcode/student belongs to that class
    public void searchForStudent(final String barcode) {

        time = (String) android.text.format.DateFormat.format("h:mm", new Date());
        month = (String) android.text.format.DateFormat.format("MMMM", new Date());
        year = (String) android.text.format.DateFormat.format("yyyy", new Date());
        day = (String) android.text.format.DateFormat.format("dd", new Date());
        loc = year + "/" + selectedCourse + "/" + month + "/" + day;
        item = FirebaseDatabase.getInstance().getReference().child(loc);
        item.keepSynced(true);
        state = 0;

        DatabaseReference StudentList = FirebaseDatabase.getInstance().getReference().child("Students/" + selectedCourse);
        StudentList.keepSynced(true);
        StudentList.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        //Get map of users in datasnapshot
                        for (DataSnapshot alert : dataSnapshot.getChildren()) {

                            String ID = alert.child("studentID").getValue().toString();

                            if (ID.equals(barcode)) {
                                state = 1;
                                item.addListenerForSingleValueEvent(

                                        new ValueEventListener() {
                                            @Override
                                            public void onDataChange(DataSnapshot dataSnapshot) {
                                                int getout = 0;

                                                for (DataSnapshot data : dataSnapshot.getChildren()) {

                                                    if (data.getKey().equals(barcode)) {
                                                        accessGranted();
//                                                display("Already Recorded");
                                                        state = 1;
                                                        getout = 1;
                                                        return;
                                                    }
                                                }
                                                // if user not already in
                                                if (getout == 0) {
                                                    accessGranted();
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
                        
                        // if not found in class
                        if (state != 1) {
                            cameraSoundWrong.start();
                            accessDenied();

                        }
                    }


                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        //handle databaseError
                    }
                });

    }


    public void onClick(View v) {
        int flag = 1;
//        Determines if class is ready to be started
        if(selectedCourse.equals("COMP 3150")){
        try {
            flag = countDown();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        }
        if (flag == 1) {
            mScannerView = new ZXingScannerView(getApplicationContext());
            setContentView(mScannerView);
            LayoutInflater inflater = getLayoutInflater();
            getWindow().addContentView(inflater.inflate(R.layout.screenbar, null),
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.FILL_PARENT,
                            ViewGroup.LayoutParams.FILL_PARENT)
            );
            mScannerView.setResultHandler(this);
            mScannerView.startCamera();
            barCourse = (TextView) findViewById(R.id.barCourse);
            barCourse.setText(barClass);
//          Displays the Access denied of granted on barcode scanner screen
            granted = (TextView) findViewById(R.id.grant);
            deny = (TextView) findViewById(R.id.denied);
            granted.setVisibility(View.GONE);
            deny.setVisibility(View.GONE);

        }
    }






    public Context getActivity() {
        return this;
    }
}
