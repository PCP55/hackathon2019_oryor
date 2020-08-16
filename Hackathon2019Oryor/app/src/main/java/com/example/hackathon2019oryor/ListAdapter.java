package com.example.hackathon2019oryor;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
    private static final String TAG = "RecyclerViewAdapter";

    //Add new var
    private ArrayList<Oryor> listOryor;
    //End

    private Context mContext;

    public ListAdapter(Context context, ArrayList<Oryor> list){

        //Add new var
        listOryor = list;
        //End

        mContext = context;
    }

    public class ViewHolder extends RecyclerView.ViewHolder{

        //Add new var
        TextView txtOryor;
        TextView txtName;
        TextView txtType;
        TextView txtStatus;

        RelativeLayout parentLayout;
        LinearLayout childLayout;
        LinearLayout divideLayout;

        CardView cardView;
        //End

        public ViewHolder(View itemView){
            super(itemView);

            //Add new var
            txtOryor = itemView.findViewById(R.id.txt_oryor);
            txtName = itemView.findViewById(R.id.txt_product_name);
            txtType = itemView.findViewById(R.id.txt_product_type);
            txtStatus = itemView.findViewById(R.id.txt_product_status);

            parentLayout = itemView.findViewById(R.id.parent_layout);
            childLayout = itemView.findViewById(R.id.child_layout);
            divideLayout = itemView.findViewById(R.id.divide_layout);
            cardView = itemView.findViewById(R.id.card_layout);
            //End
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.recyclerview_row, parent, false);
        ViewHolder holder = new ViewHolder(view);

        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder: called");

        //Add new var
        Log.d("DATA",listOryor.get(position).toString());
        holder.txtOryor.setText(listOryor.get(position).number);
        holder.txtName.setText(listOryor.get(position).nameth);
        holder.txtType.setText(listOryor.get(position).type);
        if(listOryor.get(position).status.equals("คงอยู่")){
            holder.txtStatus.setTextColor(Color.parseColor("#088A29"));
        }else{
            holder.txtStatus.setTextColor(Color.RED);
        }
        holder.txtStatus.setText(listOryor.get(position).status);
    }

    @Override
    public int getItemCount() {
        if(listOryor.size() <= 20){
            return listOryor.size();
        }
        else{
            return 20;
        }
    }


}
