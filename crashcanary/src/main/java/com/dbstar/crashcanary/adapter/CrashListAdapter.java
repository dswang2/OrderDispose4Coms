package com.dbstar.crashcanary.adapter;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.dbstar.crashcanary.R;
import com.dbstar.crashcanary.model.CrashCause;

import java.util.List;


public class CrashListAdapter extends RecyclerView.Adapter<CrashListAdapter.ViewHolder> {

    private List<CrashCause> mList;

    public CrashListAdapter(@NonNull List<CrashCause> causeList) {
        mList = causeList;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int i) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_crash, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        holder.titleView.setText(mList.get(position).getStackTrace());
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView titleView;

        public ViewHolder(View itemView) {
            super(itemView);
            titleView = (TextView) itemView.findViewById(R.id.tv_trace);
        }
    }
}
