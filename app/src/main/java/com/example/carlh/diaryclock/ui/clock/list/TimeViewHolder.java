package com.example.carlh.diaryclock.ui.clock.list;

import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.Switch;
import android.widget.TextView;

import com.example.carlh.diaryclock.R;

/**
 * Created by carlh on 11.09.2017.
 */

class TimeViewHolder extends RecyclerView.ViewHolder {

    TextView timeName;
    TextView time;
    Switch isActiveBar;
    Button deleteButton;
    LinearLayout editView;
    EditText timeNameEdit;
    EditText timeLabelEdit;
    CheckBox repeatCheckBox;

    public TimeViewHolder(View v) {
        super(v);
        timeName = (TextView) v.findViewById(R.id.time_name);
        time = (TextView) v.findViewById(R.id.time_time);
        isActiveBar = (Switch) v.findViewById(R.id.activebar);
        deleteButton  = (Button) v.findViewById(R.id.delete_time);
        editView=  ((LinearLayout) v.findViewById(R.id.edit_time));
        timeNameEdit=  ((EditText) v.findViewById(R.id.time_name_edit));
        timeLabelEdit = ((EditText) v.findViewById(R.id.time_tag_edit));
        repeatCheckBox=  ((CheckBox) v.findViewById(R.id.repeat_checkbox));
    }
}
