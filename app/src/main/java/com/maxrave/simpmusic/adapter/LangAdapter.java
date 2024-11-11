package com.maxrave.simpmusic.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.maxrave.simpmusic.R;
import com.maxrave.simpmusic.ui.LangSelectionActivity;

import java.util.List;


public class LangAdapter extends RecyclerView.Adapter<LangAdapter.ViewHolder> {

    Context context;
    List<LangModel> langModelList;

    private int selectedPosition = 0;
    public LangAdapter(LangSelectionActivity langSelectionActivity, List<LangModel> langList) {

        this.langModelList = langList;
        this.context = langSelectionActivity;

    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_lang,parent,false);

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {

        LangModel langModel = langModelList.get(position);


        ColorStateList myColorStateList = new ColorStateList(
                new int[][]{
                        new int[]{context.getResources().getColor(R.color.green)}
                },
                new int[]{context.getResources().getColor(R.color.green)}
        );

        holder.btn_lang.setButtonTintList(myColorStateList);

        holder.iv_flag.setImageResource(langModel.getFlag());

        holder.tv_lang.setText(langModel.getLang());

        holder.btn_lang.setChecked(position == selectedPosition);

        if (position == selectedPosition) {
            holder.tv_lang.setTypeface(null, Typeface.BOLD);
        } else {
            holder.tv_lang.setTypeface(null, Typeface.NORMAL);
        }

        holder.btn_lang.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPosition = position;
                notifyDataSetChanged();
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedPosition = position;
                notifyDataSetChanged();
            }
        });



    }

    @Override
    public int getItemCount() {
        return langModelList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView iv_flag;
        TextView tv_lang;
        RadioButton btn_lang;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            iv_flag = itemView.findViewById(R.id.iv_flag);

            tv_lang = itemView.findViewById(R.id.tv_lang);

            btn_lang = itemView.findViewById(R.id.btn_lang);

        }
    }

    public LangModel getSelectedLang() {
        if (selectedPosition != -1) {
            return langModelList.get(selectedPosition);
        }
        return null;
    }
}
