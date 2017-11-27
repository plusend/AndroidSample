package com.plusend.sample;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.plusend.sample.aidl.client.AidlActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private static HashMap<String, Class> categoryMap = new HashMap<>();

    static {
        categoryMap.put("AIDL", AidlActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecyclerView recyclerView = findViewById(R.id.list);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new MyAdapter(categoryMap));
    }

    private static class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {
        private List<String> nameList = new ArrayList<>();
        private List<Class> classList = new ArrayList<>();

        public MyAdapter(HashMap<String, Class> categoryMap) {
            if (categoryMap != null) {
                for (Map.Entry<String, Class> category : categoryMap.entrySet()) {
                    nameList.add(category.getKey());
                    classList.add(category.getValue());
                }
            }
        }

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new MyViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_main, null));
        }

        @Override
        public void onBindViewHolder(final MyViewHolder holder, int position) {
            holder.category.setText(nameList.get(position));
            holder.category.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.category.getContext().startActivity(new Intent(holder.category.getContext(), classList.get(holder.getAdapterPosition())));
                }
            });
        }

        @Override
        public int getItemCount() {
            return categoryMap.size();
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            private Button category;

            public MyViewHolder(View itemView) {
                super(itemView);
                category = itemView.findViewById(R.id.cate);
            }
        }
    }
}
