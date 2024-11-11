 

package com.maxrave.simpmusic.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.maxrave.simpmusic.R;

import java.util.List;


public class GuideAdapter extends RecyclerView.Adapter<GuideAdapter.GuideHolder> {

    private Context context;
    private List<GuideModel> list;

    public GuideAdapter(Context context, List<GuideModel> list) {
        this.context = context;
        this.list = list;
    }

    @NonNull
    @Override
    public GuideHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.guide_item, parent, false);
        return new GuideHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GuideHolder holder, int position) {

        holder.picture.setVisibility(View.VISIBLE);

        Glide.with(holder.picture)
                .load(list.get(position).getImage())
                .into(holder.picture);

        holder.message.setText(list.get(position).getMessage());
        holder.title.setText(list.get(position).getNumber());
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    public static class GuideHolder extends RecyclerView.ViewHolder {

        private TextView message;
        private TextView title;
        private ImageView picture;

        public GuideHolder(@NonNull View itemView) {
            super(itemView);
            message = itemView.findViewById(R.id.info_text);
            title = itemView.findViewById(R.id.info_title);
            picture = itemView.findViewById(R.id.info_image);
        }
    }

}
