package com.example.carlh.diaryclock.ui.clock.list;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.data.AppDatabase;
import com.example.carlh.diaryclock.data.Time;

import java.util.List;

/**
 * Created by carlh on 11.09.2017.
 */

public class TimeAdapter extends RecyclerView.Adapter<TimeViewHolder> {


    private final Context context;
    private List<Time> items;
    private View.OnClickListener switchClickListener;
    private View.OnClickListener entryClickListenerShort;
    private View.OnClickListener deleteClickListener;

    private View.OnClickListener repeatBoxClickListener;
    private View.OnClickListener editNameOnClickListener;
    private View.OnClickListener timeEditClickListenerShort;
    private View.OnClickListener timeLabelEditClickListenerShort;
    private AppDatabase db;
    private MenuInflater contextMenuInflater;

    public TimeAdapter(List<Time> items, Context context, View.OnClickListener itemClickListenerShort, View.OnClickListener deleteClickListener,View.OnClickListener checkBoxClickListener,View.OnClickListener repeatBoxClickListener,View.OnClickListener editNameOnClickListener,View.OnClickListener timeEditClickListenerShort,View.OnClickListener timeLabelEditClickListenerShort,AppDatabase db) {
        this.items = items;
        this.context = context;
        this.switchClickListener = checkBoxClickListener;
        this.entryClickListenerShort = itemClickListenerShort;
        this.deleteClickListener = deleteClickListener;
        this.repeatBoxClickListener = repeatBoxClickListener;
        this.editNameOnClickListener = editNameOnClickListener;
        this.timeEditClickListenerShort = timeEditClickListenerShort;
        this.timeLabelEditClickListenerShort  = timeLabelEditClickListenerShort;
        this.db = db;
    }


    //In order to get the position of the clicked item when context menu is called
    private int position;

    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public TimeViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_time, parent, false);
        return new TimeViewHolder(v);
    }

    @Override
    public void onViewRecycled(TimeViewHolder holder) {
        holder.itemView.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }

    @Override
    public void onBindViewHolder(final TimeViewHolder holder, int position) {
        Time item = items.get(position);

        Log.e(getClass().getName(), "TIME:-name: "+item.getName()+"-time: "+item.getTime(),null);

        holder.isActiveBar.setChecked(item.getActive());
        holder.timeName.setText(item.getName());
        holder.time.setText(item.getTime());
        holder.timeNameEdit.setText(item.getName());
        holder.timeNameEdit.setFocusable(false);
        holder.repeatCheckBox.setChecked(item.getRepeat());

        //Connect click listeners
        holder.isActiveBar.setTag(item);
        holder.isActiveBar.setOnClickListener(switchClickListener);

        holder.deleteButton.setTag(item);
        holder.deleteButton.setOnClickListener(deleteClickListener);

        holder.repeatCheckBox.setOnClickListener(repeatBoxClickListener);
        holder.repeatCheckBox.setTag(item);
        holder.timeNameEdit.setOnClickListener(editNameOnClickListener);
        holder.timeNameEdit.setTag(item);

        holder.itemView.setOnClickListener(entryClickListenerShort);
        holder.itemView.setTag(item);

        holder.time.setOnClickListener(timeEditClickListenerShort);
        holder.time.setTag(item);

        holder.timeLabelEdit.setOnClickListener(timeLabelEditClickListenerShort);
        holder.timeLabelEdit.setTag(item);
        if(!item.getLabel().isEmpty()){
            holder.timeLabelEdit.setText(item.getLabel());
        }
        holder.timeLabelEdit.setFocusable(false);

    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    void setItems(List<Time> times) {
        this.items = times;
        notifyDataSetChanged();
    }
}
