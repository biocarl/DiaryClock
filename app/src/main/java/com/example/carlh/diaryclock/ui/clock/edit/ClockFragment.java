package com.example.carlh.diaryclock.ui.clock.edit;

import android.app.AlarmManager;
import android.app.Fragment;
import android.app.PendingIntent;
import android.arch.lifecycle.LifecycleFragment;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.activity.MainActivity;
import com.example.carlh.diaryclock.app.DiaryClock;
import com.example.carlh.diaryclock.data.AppDatabase;
import com.example.carlh.diaryclock.data.Memo;
import com.example.carlh.diaryclock.data.Recording;
import com.example.carlh.diaryclock.data.Time;
import com.example.carlh.diaryclock.services.AlarmReceiver;
import com.example.carlh.diaryclock.ui.memo.list.MemoListFragment;
import com.example.carlh.diaryclock.utils.Typewriter;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static android.content.Context.ALARM_SERVICE;
import static java.lang.Thread.sleep;

/**
 * Created by carlh on 12.09.2017.
 */

public class ClockFragment extends LifecycleFragment {

    //Variables
    private AppDatabase appDatabase;
    private ClockFragment fragment;
    private long uIDTime;
    AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    private Intent myIntent;
    private Context context;
    private String PATH_SD;
    private ClockIntent clockIntent;
    private String message;
    private Memo lastPlayed;

    public enum ClockIntent {
        LAST_PLAYED(0),
        WELCOME(1),
        DEFAULT(-1);
        private final int value;

        ClockIntent(final int newValue) {
            value = newValue;
        }

        public int getValue() {
            return value;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        this.appDatabase = ((MainActivity) getActivity()).getDatabase();
        this.fragment = this;
        this.PATH_SD = ((DiaryClock) getActivity().getApplicationContext()).getRootPath().getAbsolutePath();   //Get application

        //get from bundle
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            this.uIDTime = bundle.getLong("uIDTime", -1);
            this.clockIntent = (ClockIntent) bundle.getSerializable("clockIntent");
            if (clockIntent == null)
                this.clockIntent = ClockIntent.DEFAULT; //pass default value directly is not possible
        }
        //services
        this.context = getContext();
        this.alarmManager = (AlarmManager) context.getSystemService(ALARM_SERVICE);
        this.myIntent = new Intent(this.context, AlarmReceiver.class);
        Time time = appDatabase.timeDao().findById(uIDTime);

        switch (clockIntent) {

            //The first ones are only for messaging
            case LAST_PLAYED: //show lastPlayedMemory
                //TODO
                break;

            case WELCOME:
                //TODO
                break;

            case DEFAULT:  //update Clock and update db

                this.message = "Default message";
                //time is not deleted and is active
                if (time != null && time.getActive()) {//set clock

                    //Remove already scheduled alarm with that ID (in case of change e.g. other label)
                    pendingIntent = PendingIntent.getBroadcast(context, Long.valueOf(time.getUid()).intValue(), myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    alarmManager.cancel(pendingIntent);
                    Date date = null;
                    SimpleDateFormat sdf = new SimpleDateFormat("HH.mm");
                    try {
                        date = sdf.parse(time.getTime());
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                    Calendar calendar = Calendar.getInstance();
                    calendar.set(Calendar.HOUR_OF_DAY, date.getHours());
                    calendar.set(Calendar.MINUTE, date.getMinutes());
                    Calendar calendarReference = Calendar.getInstance();
                    //Set date to tomorrow if time is in the past
                    if (calendarReference.getTime().getHours() > calendar.getTime().getHours() || (calendarReference.getTime().getHours() == calendar.getTime().getHours() && calendarReference.getTime().getMinutes() > calendar.getTime().getMinutes()))
                        calendar.add(Calendar.DATE, 1);

                    //Set played Memory
                    String mediaPath;

                    if (time.getLabel().isEmpty()) { //random Memory
                        lastPlayed = appDatabase.memoDao().getByRandomOffline(true);
                    } else {
                        lastPlayed = appDatabase.memoDao().getByLabelOffline(time.getLabel(), true);

                        if (lastPlayed == null) {
                            lastPlayed = appDatabase.memoDao().getByRandomOffline(true);
                        }
                    }

                    //using memo or backup
                    if (lastPlayed != null) {
                        String mediaName = appDatabase.recordingDao().findById(lastPlayed.getFileId()).getFile();
                        mediaPath = PATH_SD + "/" + mediaName;

                    } else {//using backup->standard notification
                        lastPlayed = new Memo(new Recording(""));
                        lastPlayed.setUid(-1);
                        mediaPath = RingtoneManager.getActualDefaultRingtoneUri(getActivity().getApplicationContext(), RingtoneManager.TYPE_ALARM).getPath();
                    }

                    //or use second backup
                    File mediaFile = new File(mediaPath);
                    if (!mediaFile.exists()) {
                        Toast.makeText(getContext(), "File is not cached", Toast.LENGTH_SHORT).show();
                    }

                    //extra for intent
                    myIntent.putExtra("extra", "alarm on");
                    myIntent.putExtra("soundPath", mediaFile.getAbsolutePath());
                    myIntent.putExtra("uIDTime", time.getUid());
                    myIntent.putExtra("uIDMemo", lastPlayed.getUid());
                    pendingIntent = PendingIntent.getBroadcast(context, Long.valueOf(time.getUid()).intValue(), myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    //SET ALARM
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
                    //FOR TESTING
                    //alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendarReference.getTimeInMillis() + 3000, pendingIntent);

                    //SimpleDateFormat format1 = new SimpleDateFormat("hh:mm dd/MM");
                    //String formattedDate = format1.format(calendar.getTime());
                    String formattedDate = "" + calendar.getTime();
                    this.message = "Setting clock to " + formattedDate;

                } else {
                    //This also works if Time-Entity is already deleted since the uid was passed
                    if (alarmManager != null) {
                        this.message = "Alarm canceled";
                        //remove scheduled alarm
                        pendingIntent = PendingIntent.getBroadcast(context, Long.valueOf(uIDTime).intValue(), myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                        alarmManager.cancel(pendingIntent);
                        //send intent with stops alarm
                        myIntent.putExtra("extra", "alarm off");
                        myIntent.putExtra("uIDTime", uIDTime);
                        if (lastPlayed != null) { //this happens if you stop music when the app is started and was not in background (possible because switch has function of canceling alarm and stopping alarm)
                            myIntent.putExtra("uIDMemo", lastPlayed.getUid());
                        }
                        //stop the ringtone
                        context.sendBroadcast(myIntent);
                    }
                }

                break;
        }

        //don't show fragment if to much output
        //if there is to many fragments in the box, don't show the new one and show an text that says it's to much output
        Boolean tooMuchOutput = (((ViewGroup) container.getRootView().findViewById(R.id.info_frame)).getChildCount() > 3);

        if (tooMuchOutput) {
            if (fragment != null && getActivity() != null) {
                getActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                Log.e(getClass().getName(), "[NOT showing fragment because 3 < #children]", null);
            }
            return null;
        } else {
            View view = inflater.inflate(R.layout.notification_time_changed, container, false);
            Typewriter writer = (Typewriter) view.findViewById(R.id.notification_text);
            //Add a character every 150ms
            writer.setCharacterDelay(150);
            switch (clockIntent) {
                case LAST_PLAYED:
                    DiaryClock application = ((DiaryClock) getActivity().getApplicationContext());
                    lastPlayed = appDatabase.memoDao().findById(application.getPreferences().getLong("last_played", -1));
                    //Memory was already deleted since last time
                    if(lastPlayed == null) {
                        getActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commit();
                        return null;
                    }
                    String lastPlayedString = lastPlayed.getName();
                    if (lastPlayedString.length() > 12) {
                        lastPlayedString = lastPlayedString.substring(0, 12);
                        lastPlayedString += "...";
                    }
                    this.message = "last played: " + lastPlayedString + " ~ edit";

                    //make clickable
                    writer.setOnClickListener(lastMemoClickListener);
                    writer.setTag(lastPlayed);
                    writer.setCompoundDrawablesWithIntrinsicBounds(null, null, getResources().getDrawable(R.drawable.right), null);

                    break;
                case WELCOME:
                    this.message = "Welcome! Click here to do your first tutorial!";
                    break;
                case DEFAULT:
                    message +="";//nothing to add?
                    break;
            }
            //Set message
            writer.animateText(message);
            // Inflate the layout for this fragment
            return view;
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        //to kind of notifications: permanent and temporary
        if (clockIntent != ClockIntent.LAST_PLAYED) //the only permanent for now
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (fragment != null && getActivity() != null) { //this is possible when the fragment is already replaced by the next time action
                        getActivity().getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss(); //https://stackoverflow.com/questions/7469082/getting-exception-illegalstateexception-can-not-perform-this-action-after-onsa
                    }
                }
            }, 3000);
    }

    public static ClockFragment newInstance(long uIDTime, long uIDMemo, ClockIntent clockIntent) {
        ClockFragment fragment = new ClockFragment();
        //add uid of Time entity
        Bundle args = new Bundle();
        args.putLong("uIDTime", uIDTime);
        args.putLong("uIDMemo", uIDMemo);
        args.putSerializable("clockIntent", clockIntent);
        fragment.setArguments(args);
        return fragment;
    }

    private View.OnClickListener lastMemoClickListener = new View.OnClickListener() {

        @Override
        public void onClick(View view) {
            final RecyclerView recyclerView = (RecyclerView) view.getRootView().findViewById(R.id.recycler_view_list_memos);
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.mViewPager.setCurrentItem(1); //change to memo list
            //Scroll to last played Memo if existent
            Long memoID = ((DiaryClock) getActivity().getApplicationContext()).getPreferences().getLong("last_played", -1);
            int position = 0;
            if (memoID != -1) {
                int counter = 0;
                for (Memo m : appDatabase.memoDao().getAll()) {
                    if (m.getUid() == memoID) {
                        position = counter;
                        break;
                    }
                    counter++;
                }
            }
            //recyclerView.scrollToPosition(position);
            final LinearLayoutManager layoutManager = ((LinearLayoutManager) recyclerView.getLayoutManager());
            layoutManager.scrollToPositionWithOffset(position,0);
        }
    };
}