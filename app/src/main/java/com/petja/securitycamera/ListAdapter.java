package com.petja.securitycamera;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {

    RemoteDevice[] remoteDevices;
    MainActivity mainActivity;

    public ListAdapter(RemoteDevice[] dataSet, MainActivity mainActivity) {
        remoteDevices = dataSet;
        this.mainActivity = mainActivity;
        Log.d("petjalog", "new adapter " + remoteDevices.length);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView name;
        private final TextView ip;

        public ViewHolder(View view) {
            super(view);
            name = (TextView) view.findViewById(R.id.name);
            ip = (TextView) view.findViewById(R.id.ip);
        }

        public TextView getName() {
            return name;
        }

        public TextView getIp() {
            return ip;
        }
    }

    // Create new views (invoked by the layout manager)
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {
        // Create a new view, which defines the UI of the list item
        View view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.listviewitem, viewGroup, false);

        return new ViewHolder(view);
    }

    // Replace the contents of a view (invoked by the layout manager)
    @Override
    public void onBindViewHolder(ViewHolder viewHolder, final int position) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        viewHolder.getIp().setText(remoteDevices[position].getIp());
        viewHolder.getName().setText(remoteDevices[position].getName());
        viewHolder.itemView.setOnClickListener(view -> {
            int id = remoteDevices[viewHolder.getAdapterPosition()].getId();
            mainActivity.startMonitorActivity(id);
        });
    }

    // Return the size of your dataset (invoked by the layout manager)
    @Override
    public int getItemCount() {
        return remoteDevices.length;
    }

    public void clearData() {
        remoteDevices = new RemoteDevice[0];
    }
}
