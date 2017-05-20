package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.penn.ppj.databinding.FragmentDashboardBinding;
import com.penn.ppj.databinding.FragmentNotificationBinding;
import com.penn.ppj.databinding.MessageCellBinding;
import com.penn.ppj.databinding.MomentOverviewCellBinding;
import com.penn.ppj.model.realm.Message;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.util.PPHelper;

import java.util.List;

import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class NotificationFragment extends Fragment {

    public FragmentNotificationBinding binding;

    private Realm realm;

    private RealmResults<Message> data;

    private PPAdapter ppAdapter;
    private LinearLayoutManager linearLayoutManager;

    public NotificationFragment() {
        // Required empty public constructor
    }

    public static NotificationFragment newInstance() {
        NotificationFragment fragment = new NotificationFragment();

        return fragment;
    }

    private final View.OnClickListener messageOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = binding.mainRecyclerView.getChildAdapterPosition(v);
            Message message = data.get(position);
            goMessageDetail(message);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //common
        binding = DataBindingUtil.inflate(
                inflater, R.layout.fragment_notification, container, false);
        View view = binding.getRoot();
        //end common

        realm = Realm.getDefaultInstance();

        data = realm.where(Message.class).findAllSorted("createTime", Sort.DESCENDING);
        data.addChangeListener(changeListener);

        setup();

        return view;
    }

    @Override
    public void onDestroyView() {
        realm.close();
        super.onDestroyView();
    }

    private void setup() {
        linearLayoutManager = new LinearLayoutManager(getContext());
        binding.mainRecyclerView.setLayoutManager(linearLayoutManager);

        ppAdapter = new PPAdapter(data);
        binding.mainRecyclerView.setAdapter(ppAdapter);
    }

    private void goMessageDetail(Message message){

    }

    private final OrderedRealmCollectionChangeListener<RealmResults<Message>> changeListener = new OrderedRealmCollectionChangeListener<RealmResults<Message>>() {
        @Override
        public void onChange(RealmResults<Message> collection, OrderedCollectionChangeSet changeSet) {
            // `null`  means the async query returns the first time.
            if (changeSet == null) {
                ppAdapter.notifyDataSetChanged();
                return;
            }
            // For deletions, the adapter has to be notified in reverse order.
            OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
            for (int i = deletions.length - 1; i >= 0; i--) {
                OrderedCollectionChangeSet.Range range = deletions[i];
                ppAdapter.notifyItemRangeRemoved(range.startIndex, range.length);
            }

            OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
            for (OrderedCollectionChangeSet.Range range : insertions) {
                ppAdapter.notifyItemRangeInserted(range.startIndex, range.length);
            }

            OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
            for (OrderedCollectionChangeSet.Range range : modifications) {
                ppAdapter.notifyItemRangeChanged(range.startIndex, range.length);
            }
        }
    };

    class PPAdapter extends RecyclerView.Adapter<PPAdapter.PPHoldView> {
        private List<Message> data;

        public class PPHoldView extends RecyclerView.ViewHolder {
            private MessageCellBinding binding;

            public PPHoldView(MessageCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        public PPAdapter(List<Message> data) {
            this.data = data;
        }

        @Override
        public PPAdapter.PPHoldView onCreateViewHolder(ViewGroup parent, int viewType) {
            MessageCellBinding messageCellBinding = MessageCellBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

            messageCellBinding.getRoot().setOnClickListener(messageOnClickListener);

            return new PPHoldView(messageCellBinding);
        }

        @Override
        public void onBindViewHolder(PPAdapter.PPHoldView holder, int position) {
            holder.binding.setData(data.get(position));
//            Picasso.with(getContext())
//                    .load(PPHelper.get800ImageUrl(data.get(position).getPic().getKey()))
//                    //.placeholder(R.drawable.ab_gradient_dark)
//                    .into(holder.binding.mainImageView);
//
//            Picasso.with(getContext())
//                    .load(PPHelper.get80ImageUrl(data.get(position).getAvatar()))
//                    //.placeholder(R.drawable.ab_gradient_dark)
//                    .into(holder.binding.avatarCircleImageView);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }
}