package com.example.carlh.diaryclock.activity;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.app.DiaryClock;
import com.example.carlh.diaryclock.data.AppDatabase;

import javax.inject.Inject;

/**
 * Created by carlh on 10.11.2017.
 */


public class ImportActivity extends AppCompatActivity {

    private Context context;
    private DiaryClock application;
    @Inject
    AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = this;
        //Injection
        this.application = ((DiaryClock) context.getApplicationContext());   //Get application
        application.getComponent().inject(this);
        setContentView(R.layout.activity_import);
    }
}
