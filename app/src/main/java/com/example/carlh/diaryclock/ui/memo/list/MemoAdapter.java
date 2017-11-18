package com.example.carlh.diaryclock.ui.memo.list;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.carlh.diaryclock.R;
import com.example.carlh.diaryclock.data.AppDatabase;
import com.example.carlh.diaryclock.data.Memo;
import com.example.carlh.diaryclock.data.Tag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by carlh on 11.09.2017.
 */

public class MemoAdapter extends RecyclerView.Adapter<MemoViewHolder> {

    private final Context context;
    HashMap<Integer, Integer> colorMap; //Position,Color
    private List<Memo> items;
    private View.OnClickListener checkBoxClickListener;
    private View.OnClickListener viewClickListenerShort;
    private View.OnLongClickListener viewClickListenerLong;
    private View.OnClickListener tagClickListenerShort;
    private AppDatabase db;
    //Coloring views
    private Long recordingUID;
    private Integer lastUsedColor;
    private int lastPlayedPosition;

    private MenuInflater contextMenuInflater;

    public MemoAdapter(List<Memo> items, Context context, View.OnClickListener itemClickListenerShort, View.OnLongClickListener viewClickListenerLong, View.OnClickListener checkBoxClickListener, View.OnClickListener tagClickListenerShort, MenuInflater contextMenuInflater, AppDatabase db, HashMap<Integer, Integer> colorMap) {
        this.contextMenuInflater = contextMenuInflater;
        this.items = items;
        this.context = context;
        this.lastPlayedPosition = 0; //first index
        this.checkBoxClickListener = checkBoxClickListener;
        this.viewClickListenerShort = itemClickListenerShort;
        this.viewClickListenerLong = viewClickListenerLong;
        this.tagClickListenerShort = tagClickListenerShort;
        this.db = db;
        this.colorMap = colorMap;

    }


    @Override
    public MemoViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_memo, parent, false);
        return new MemoViewHolder(v, contextMenuInflater);
    }

    @Override
    public void onViewRecycled(MemoViewHolder holder) {
        holder.itemView.setOnLongClickListener(null);
        super.onViewRecycled(holder);
    }

    @Override
    public void onBindViewHolder(final MemoViewHolder holder, int position) {
        Memo item = items.get(position);

        //Coloring view
        if (position == 0) {//First view
            colorMap.put(position, context.getResources().getColor(R.color.memo_list_file1));
        } else {
            //Color memos from the same recording equally
            Integer color = colorMap.get(position - 1);
            if(color == null){//not a good solution: If you jump to the last memo, it can happen that the previous view is not filled yet and therefore no hash entry
                color = context.getResources().getColor(R.color.memo_list_file1);
            }
            //Same recording
            if (items.get(position).getFileId().equals(items.get(position - 1).getFileId())) {
                colorMap.put(position, color);
            } else {//different recording
                if (color == context.getResources().getColor(R.color.memo_list_file1)) {
                    colorMap.put(position,context.getResources().getColor( R.color.memo_list_file2));
                } else {
                    colorMap.put(position,context.getResources().getColor(R.color.memo_list_file1));
                }
            }
        }
        holder.itemView.setBackgroundColor(colorMap.get(position));

        //Other color-Codes
            //lastPlayedMemo
        if (item.getLastPlayed())
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.memo_list_last_played));

        if(!item.getCached())
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.memo_list_not_cached));

        //Sync-Status
        if (item.getNotSynced())
            holder.itemView.setBackgroundColor(context.getResources().getColor(R.color.memo_list_not_synced));

        //Setting up holder
        holder.cachedBox.setChecked(item.getCached());
        holder.ratingBar.setRating(item.getRating());
        holder.memoName.setText(item.getName());
        holder.labelsBox.setText(Html.fromHtml(createTags(Tag.getTagStringsById(item.getUid(), db))));
        holder.labelsBox.setTextColor(Color.BLACK);
        holder.labelsBox.setOnClickListener(tagClickListenerShort);
        holder.labelsBox.setTag(item);
        holder.itemView.setTag(item);
        holder.isCached = item.getCached();
        //Connect click listeners
        holder.itemView.setOnLongClickListener((viewClickListenerLong));
        holder.cachedBox.setTag(item);
        holder.cachedBox.setOnClickListener(checkBoxClickListener);
        holder.itemView.setOnClickListener(viewClickListenerShort);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    void setItems(List<Memo> memos) {
        this.items = memos;
        notifyDataSetChanged();
    }

    /*Helper function for showing tags in colors*/
    public String createTags(List<String> tags) {
        String string = "";
        int counter = 0; //generating different colors
        ArrayList<String> colors = new ArrayList<>();
        colors.add("9400D3");
        colors.add("0099ff");
        colors.add("4eed4e");
        colors.add("FFFF00");
        colors.add("FF7F00");
        colors.add("ff6363");
        colors.add("b367ea");

        for (String tag : tags) {
            counter++;

            if (tag.isEmpty())
                continue;
            //string += "<font color=" + "#"+hex + ">" + tag + "</font> ";
            string += " <span style=\"background: " + "#" + colors.get(counter % 7) + ";\">" + tag + "</span> ";
            string += " <span style=\"background: " + "fff" + ";\">" + "   " + "</span> ";

        }
        return string;
    }


    public int getLastPlayedPosition() {
        return lastPlayedPosition;
    }

}
