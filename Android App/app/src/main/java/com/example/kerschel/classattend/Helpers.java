package com.example.kerschel.classattend;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

/**
 * Created by kersc on 4/4/2017.
 */

public class Helpers extends MainActivity{


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

    public void show() {
        refCourse.addListenerForSingleValueEvent(
                new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {

                        for (DataSnapshot alert : dataSnapshot.getChildren()) {
//                           Adds the courses to a array to be displayed
//                            mylist.add(alert.getValue().toString());
                            if(alert.child("room").getValue().toString().equals(classroom)) {
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


}
