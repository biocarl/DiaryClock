package com.example.carlh.diaryclock.ui.memo.list;

import android.support.v7.widget.RecyclerView;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.RatingBar;
import android.widget.TextView;

import com.example.carlh.diaryclock.R;

import org.w3c.dom.Text;

/**
 * Created by carlh on 11.09.2017.
 */

class MemoViewHolder extends RecyclerView.ViewHolder implements View.OnCreateContextMenuListener {

    TextView memoName;
    TextView labelsBox;
    RatingBar ratingBar;
    CheckBox cachedBox;
    Boolean isCached;
    MenuInflater contextMenuInflater;

    public MemoViewHolder(View v,MenuInflater contextMenuInflater) {
        super(v);

        //For context menu in RecyclerView
        this.contextMenuInflater = contextMenuInflater;
        memoName = (TextView) v.findViewById(R.id.memo_name);
        labelsBox = (TextView) v.findViewById(R.id.labels_box);
        ratingBar = (RatingBar) v.findViewById(R.id.rating);
        cachedBox = (CheckBox) v.findViewById(R.id.checkbox);
        v.setOnCreateContextMenuListener(this);

    }

    @Override
    public void onCreateContextMenu(ContextMenu contextMenu, View view, ContextMenu.ContextMenuInfo contextMenuInfo) {
        contextMenuInflater.inflate(R.menu.db_context_menu,contextMenu);
        if(!isCached){
            contextMenu.findItem(R.id.play_memo).setEnabled(false);
        }else {
            contextMenu.findItem(R.id.play_memo).setEnabled(true);
        }
    }
}
