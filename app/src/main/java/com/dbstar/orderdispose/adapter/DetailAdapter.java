package com.dbstar.orderdispose.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.dbstar.orderdispose.R;
import com.dbstar.orderdispose.bean.OrderDetail;

import java.util.List;

/**
 * Created by dswang on 2016/12/25.
 */

public class DetailAdapter extends RecyclerView.Adapter<DetailHolder> {


    private final Context context;
    private List<OrderDetail.OrderDetailBean> datas;


    public DetailAdapter(Context context, List<OrderDetail.OrderDetailBean> datas) {
        this.context = context;
        this.datas = datas;
    }

    @Override
    public DetailHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.detail_item, parent, false);
        return new DetailHolder(v);
    }

    @Override
    public void onBindViewHolder(DetailHolder holder, int position) {
        OrderDetail.OrderDetailBean orderDetailBean = datas.get(position);
        holder.detail_item_tv_name.setText(orderDetailBean.getGoodsName());
        holder.detail_item_tv_price.setText("" + orderDetailBean.getPrice());
        if (orderDetailBean.getBuynum() != null) {
            holder.detail_item_tv_count.setText("" + orderDetailBean.getBuynum());
        }else {
            holder.detail_item_tv_count.setText("1");
        }
    }

    @Override
    public int getItemCount() {
        if (datas == null || datas.isEmpty()) {
            return 0;
        } else {
            return datas.size();
        }
    }


}

class DetailHolder extends RecyclerView.ViewHolder {
    public TextView detail_item_tv_name;
    public TextView detail_item_tv_price;
    public TextView detail_item_tv_count;

    public DetailHolder(View itemView) {
        super(itemView);
        detail_item_tv_name = (TextView) itemView.findViewById(R.id.detail_item_tv_name);
        detail_item_tv_price = (TextView) itemView.findViewById(R.id.detail_item_tv_price);
        detail_item_tv_count = (TextView) itemView.findViewById(R.id.detail_item_tv_count);
    }
}