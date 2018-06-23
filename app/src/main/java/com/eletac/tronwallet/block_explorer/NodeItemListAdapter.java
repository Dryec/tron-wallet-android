package com.eletac.tronwallet.block_explorer;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.eletac.tronwallet.R;

import org.tron.api.GrpcAPI;
import org.tron.common.utils.ByteArray;

import java.util.List;

public class NodeItemListAdapter extends RecyclerView.Adapter<NodeItemListAdapter.NodeItemViewHolder> {

    private Context mContext;
    private List<GrpcAPI.Node> mNodes;
    private List<GrpcAPI.Node> mNodesFiltered;

    private boolean showFiltered;

    public NodeItemListAdapter(Context context, List<GrpcAPI.Node> nodes, List<GrpcAPI.Node> nodesFiltered) {
        mContext = context;
        mNodes = nodes;
        mNodesFiltered = nodesFiltered;
    }

    @NonNull
    @Override
    public NodeItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_node_item, parent, false);
        NodeItemViewHolder viewHolder = new NodeItemViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull NodeItemViewHolder holder, int position) {
        if(showFiltered) {
            holder.bind(mNodesFiltered.get(position));
        } else {
            holder.bind(mNodes.get(position));
        }
    }

    @Override
    public int getItemCount() {
        if(showFiltered) {
            return mNodesFiltered != null ? mNodesFiltered.size() : 0;
        }
        else {
            return mNodes != null ? mNodes.size() : 0;
        }
    }

    public class NodeItemViewHolder extends RecyclerView.ViewHolder {
        private Context mContext;

        private TextView mIP_TextView;
        private TextView mPort_TextView;

        public NodeItemViewHolder(View itemView) {
            super(itemView);
            mContext = itemView.getContext();

            mIP_TextView = itemView.findViewById(R.id.Node_ip_textView);
            mPort_TextView = itemView.findViewById(R.id.Node_port_textView);
        }

        public void bind(GrpcAPI.Node node) {
            mIP_TextView.setText(ByteArray.toStr(node.getAddress().getHost().toByteArray()));
            mPort_TextView.setText(String.valueOf(node.getAddress().getPort()));
        }
    }

    public boolean isShowFiltered() {
        return showFiltered;
    }

    public void setShowFiltered(boolean showFiltered) {
        this.showFiltered = showFiltered;
    }
}
