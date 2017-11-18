package com.example.carlh.diaryclock.ui.clock.list;

import android.app.AlertDialog;
import android.app.Fragment;
import android.arch.lifecycle.LifecycleFragment;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;

import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.activity.MainActivity;
import com.example.carlh.diaryclock.app.DiaryClock;
import com.example.carlh.diaryclock.data.AppDatabase;

import com.example.carlh.diaryclock.data.Tag;
import com.example.carlh.diaryclock.data.Time;
import com.example.carlh.diaryclock.data.TimeDao;
import com.example.carlh.diaryclock.ui.clock.edit.ClockFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static com.example.carlh.diaryclock.R.drawable.clock_01;
import static com.example.carlh.diaryclock.ui.clock.edit.ClockFragment.ClockIntent.DEFAULT;

/**
 * Created by carlh on 11.09.2017.
 */

public class TimeListFragment extends LifecycleFragment {

    //Variables
    private String PATH_SD;
    private TimeAdapter adapter;
    private AppDatabase appDatabase;
    private Time timeInContext;
    private TimeDao timeDao;

    //short click on a sub element in an entry: change to checkbox-click
    private View.OnClickListener deleteClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            LinearLayout r = (LinearLayout) ((ViewGroup) view.getParent());
            r.setVisibility(View.GONE);

            Time time = (Time) view.getTag();
            time.setActive(false);
            //Delete it afterwards
            Toast.makeText(getContext(), "Time is gone", Toast.LENGTH_SHORT).show();
            //delete time
            timeDao.delete(time);
            //If the time is already sheduled cancel it

            //start clock fragment with reference to the Id
            ClockFragment fragment = ClockFragment.newInstance(time.getUid(),-1,DEFAULT);
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.info_frame, fragment).commit();

        }
    };

    //Short click on an entry which is an area without a specific listener
    private View.OnClickListener itemClickListenerShort = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Time time = (Time) view.getTag();
            LinearLayout layout =  ((LinearLayout) view.findViewById(R.id.edit_time));
            if(layout.getVisibility() == View.VISIBLE){
                layout.setVisibility(View.GONE);
            }else {
                layout.setVisibility(View.VISIBLE);
            }
            Toast.makeText(getContext(), "Expanding clock parameters"+time.getName(), Toast.LENGTH_SHORT).show();

        }
    };


    //Short click on time for choosing time (24h-format)
    private View.OnClickListener timeEditClickListenerShort;

    {
        timeEditClickListenerShort = new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
                final Time time = (Time) view.getTag();

                //current time
                Calendar calendarReference = Calendar.getInstance();

                //Define Layout of Pickers
                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.HORIZONTAL);

                //hours
                final NumberPicker hoursPicker = new NumberPicker(getContext());
                hoursPicker.setMaxValue(24);
                hoursPicker.setMinValue(0);
                hoursPicker.setValue(calendarReference.getTime().getHours());
                hoursPicker.setFormatter(new NumberPicker.Formatter() {
                    @Override
                    public String format(int value) {
                        return String.format("%02d", value);
                    }
                });

                //Minutes
                final NumberPicker minutesPicker = new NumberPicker(getContext());
                minutesPicker.setMaxValue(59);
                minutesPicker.setMinValue(0);
                minutesPicker.setValue(calendarReference.getTime().getMinutes());
                minutesPicker.setFormatter(new NumberPicker.Formatter() {
                    @Override
                    public String format(int value) {
                        return String.format("%02d", value);
                    }
                });

                //define layout
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(50, 50);
                layoutParams.gravity = Gravity.CENTER;

                LinearLayout.LayoutParams hoursLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                hoursLayout.weight = 1;

                LinearLayout.LayoutParams minutesLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                minutesLayout.weight = 1;

                layout.setLayoutParams(layoutParams);
                layout.addView(hoursPicker, hoursLayout);
                layout.addView(minutesPicker, minutesLayout);

                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                //builder.setTitle("Type in new name");
                // Set up the input

                builder.setView(layout);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int hours = hoursPicker.getValue();
                        int minutes = minutesPicker.getValue();

                        //Correcting leading 0's
                        String timeString = String.format("%02d", hours) + "." + String.format("%02d", minutes);
                        Log.e(getClass().getName(), "[Time saved]: " + timeString, null);

                        time.setTime(timeString);
                        appDatabase.timeDao().update(time);


                        //start clock fragment with reference to the Id
                        ClockFragment fragment = ClockFragment.newInstance(time.getUid(), -1,DEFAULT);
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        transaction.add(R.id.info_frame, fragment).commit();


                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();

                Toast.makeText(getContext(), "Edit time", Toast.LENGTH_SHORT).show();

            }
        };
    }


    //Short click on time for choosing time (24h-format)
    private View.OnClickListener timeLabelEditClickListenerShort;

    {
        timeLabelEditClickListenerShort = new View.OnClickListener() {
            @Override
            public void onClick(final View view) {

                //Get database entities
                final Time time = (Time) view.getTag();
                final List<String> tags = appDatabase.tagDao().getTagsAsString();

                //Define Layout of Pickers
                LinearLayout layout = new LinearLayout(getContext());
                layout.setOrientation(LinearLayout.HORIZONTAL);

                //hours
                final NumberPicker tagPicker = new NumberPicker(getContext());
                tagPicker.setMaxValue(tags.size()-1);
                tagPicker.setMinValue(0);
                tagPicker.setDisplayedValues(tags.toArray(new String[tags.size()]));

                //define layout
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(50, 50);
                layoutParams.gravity = Gravity.CENTER;

                LinearLayout.LayoutParams tagPickerLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                tagPickerLayout.weight = 1;
                layout.setLayoutParams(layoutParams);
                layout.addView(tagPicker, tagPickerLayout);

                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle("Select Tag");
                builder.setView(layout);

                // Set up the buttons
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String labelString = tags.get(tagPicker.getValue());
                        time.setLabel(labelString);
                        appDatabase.timeDao().update(time);

                        //start clock fragment with reference to the Id
                        ClockFragment fragment = ClockFragment.newInstance(time.getUid(),-1,DEFAULT);
                        FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
                        transaction.add(R.id.info_frame, fragment).commit();


                    }
                });
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

                builder.show();

                Toast.makeText(getContext(), "Edit time", Toast.LENGTH_SHORT).show();

            }
        };
    }




    //get value of switch (on/off alarm)
    private View.OnClickListener switchClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            Time time = (Time) view.getTag();
            long pendingId = time.getUid();
            boolean isChecked = ((Switch) view.findViewById(R.id.activebar)).isChecked();
            //update db
            time.setActive(isChecked);
            appDatabase.timeDao().update(time);
            //start clock fragment with reference to the Id
            ClockFragment fragment = ClockFragment.newInstance(time.getUid(),-1,DEFAULT);
            FragmentTransaction transaction = getActivity().getSupportFragmentManager().beginTransaction();
            transaction.add(R.id.info_frame, fragment).commit();
        }
    };

    //get value of box  (on/off repeat)
    private View.OnClickListener repeatBoxClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            Time time = (Time) view.getTag();
            boolean isChecked = ((CheckBox) view.findViewById(R.id.repeat_checkbox)).isChecked();
            time.setRepeat(isChecked);
            appDatabase.timeDao().update(time);
        }
    };


    //get value clock name (on/off repeat)
    private View.OnClickListener editNameOnClickListener = new View.OnClickListener() {

        @Override
        public void onClick(final View view) {
            final Time time = (Time) view.getTag();

            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
            builder.setTitle("Type in new name");
            // Set up the input
            final EditText input = new EditText(view.getContext());
            input.setText(time.getName());
            builder.setView(input);

        // Set up the buttons
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    time.setName(input.getText().toString());
                    appDatabase.timeDao().update(time);

                    /*
                    InputMethodManager imm = (InputMethodManager)getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
                    */

                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                }
            });

            builder.show();

        }
    };


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        //get dependencies through activity
        this.appDatabase = ((MainActivity) getActivity()).getDatabase();
        this.PATH_SD = ((DiaryClock) getActivity().getApplicationContext()).getRootPath().getAbsolutePath();   //Get application
        this.timeDao = appDatabase.timeDao();

        View v = inflater.inflate(R.layout.fragment_list_times, container, false);
        setupRecyclerView(v);

        //Observe the live data and update adapter if change occured
        timeDao.getAllObservable().observe(this, new Observer<List<Time>>(){

            @Override
            public void onChanged(@Nullable List<Time> times) {
                adapter.setItems(times);
            }
        });

        return v;
    }

    private void setupRecyclerView(View v) {
        RecyclerView recyclerView = (RecyclerView) v.findViewById(R.id.recycler_view_list_times);

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);

        adapter = new TimeAdapter(new ArrayList<Time>(), getContext(), itemClickListenerShort, deleteClickListener, switchClickListener,repeatBoxClickListener, editNameOnClickListener,timeEditClickListenerShort,timeLabelEditClickListenerShort,appDatabase);
        recyclerView.setAdapter(adapter);

        final DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                layoutManager.getOrientation());
        recyclerView.addItemDecoration(dividerItemDecoration);
    }

}

