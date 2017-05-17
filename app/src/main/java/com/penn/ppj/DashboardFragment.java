package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;


import com.penn.ppj.databinding.FragmentDashboardBinding;
import com.penn.ppj.databinding.MomentOverviewCellBinding;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPLoadController;
import com.penn.ppj.util.PPLoadDataAdapter;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

public class DashboardFragment extends Fragment {

    private FragmentDashboardBinding binding;

    private Realm realm;

    private RealmResults<Moment> data;

    private PPAdapter ppAdapter;
    private GridLayoutManager gridLayoutManager;

    public DashboardFragment() {
        // Required empty public constructor
    }

    public static DashboardFragment newInstance() {
        DashboardFragment fragment = new DashboardFragment();

        return fragment;
    }

    private final View.OnClickListener momentOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = binding.mainRecyclerView.getChildAdapterPosition(v);
            Moment moment = data.get(position);
            Intent intent = new Intent(getContext(), MomentDetailActivity.class);
            intent.putExtra("momentId", moment.getId());
            startActivity(intent);
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
                inflater, R.layout.fragment_dashboard, container, false);
        View view = binding.getRoot();
        //end common

        realm = Realm.getDefaultInstance();

        data = realm.where(Moment.class).findAllSorted("createTime", Sort.DESCENDING);
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
        gridLayoutManager = new GridLayoutManager(getContext(), PPHelper.calculateNoOfColumns(getContext()));
        binding.mainRecyclerView.setLayoutManager(gridLayoutManager);

        ppAdapter = new PPAdapter(data);
        binding.mainRecyclerView.setAdapter(ppAdapter);
    }

    private final OrderedRealmCollectionChangeListener<RealmResults<Moment>> changeListener = new OrderedRealmCollectionChangeListener<RealmResults<Moment>>() {
        @Override
        public void onChange(RealmResults<Moment> collection, OrderedCollectionChangeSet changeSet) {
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
        private List<Moment> data;

        public class PPHoldView extends RecyclerView.ViewHolder {
            private MomentOverviewCellBinding binding;

            public PPHoldView(MomentOverviewCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        public PPAdapter(List<Moment> data) {
            this.data = data;
        }

        @Override
        public PPAdapter.PPHoldView onCreateViewHolder(ViewGroup parent, int viewType) {
            MomentOverviewCellBinding momentOverviewCellBinding = MomentOverviewCellBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

            momentOverviewCellBinding.getRoot().setOnClickListener(momentOnClickListener);

            return new PPHoldView(momentOverviewCellBinding);
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
