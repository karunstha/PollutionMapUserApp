package com.halo.pmapu.data;

import android.content.Context;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.halo.pmapu.R;

import java.util.List;

/**
 * Created by karsk on 10/03/2017.
 */

public class AdapterPred extends RecyclerView.Adapter<AdapterPred.MyViewHolder> {

    public List<ModelPred> modelPredList;
    RecyclerView recyclerView;
    private Context context;

    public AdapterPred(List<ModelPred> modelPredList, Context context, RecyclerView recyclerView) {
        this.context = context;
        this.modelPredList = modelPredList;
        this.recyclerView = recyclerView;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_data, parent, false);
        MyViewHolder viewHolder = new MyViewHolder(itemView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final ModelPred modelPred = modelPredList.get(position);

        holder.title.setText(modelPred.getTime());
        holder.address.setText(modelPred.getValue());

    }

    @Override
    public int getItemCount() {
        return modelPredList.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView title, address;
        CardView cardView;

        public MyViewHolder(View itemView) {
            super(itemView);
            title = (TextView) itemView.findViewById(R.id.tv_time);
            address = (TextView) itemView.findViewById(R.id.tv_value);
            cardView = (CardView) itemView.findViewById(R.id.cardView);
        }
    }

}
