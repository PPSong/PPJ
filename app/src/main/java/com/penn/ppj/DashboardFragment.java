package com.penn.ppj;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.penn.ppj.databinding.FragmentDashboardBinding;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPLoadController;
import com.penn.ppj.util.PPLoadDataAdapter;

import java.util.ArrayList;
import java.util.List;

public class DashboardFragment extends Fragment implements PPLoadController.LoadDataProvider {

    private FragmentDashboardBinding binding;

    private PPAdapter ppAdapter;
    private LinearLayoutManager linearLayoutManager;
    private PPLoadController ppLoadController;

    //pptodo test
    int i = 0;
    int j = 0;
    long baseLong = System.currentTimeMillis();
    //end pptodo

    public DashboardFragment() {
        // Required empty public constructor
    }

    public static DashboardFragment newInstance() {
        DashboardFragment fragment = new DashboardFragment();

        return fragment;
    }

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

        setup();

        return view;
    }

    private void setup() {
        binding.mainSwipeRefreshLayout.setProgressViewOffset(true, 0, PPHelper.getStatusBarAddActionBarHeight(getContext()));

        linearLayoutManager = new LinearLayoutManager(getContext());
        ArrayList<Long> data = new ArrayList();
        for (int k = 0; k < 10; k++) {
            data.add(baseLong + j++);
        }
        ppAdapter = new PPAdapter(data);

        ppLoadController = new PPLoadController(binding.mainSwipeRefreshLayout, binding.mainRecyclerView, ppAdapter, linearLayoutManager, this);
    }

    @Override
    public void refreshData() {
        i = 0;
        j = 0;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ArrayList<Long> data = new ArrayList();
                for (int k = 0; k < 10; k++) {
                    data.add(baseLong + j++);
                }
                //success with more
                ppLoadController.ppLoadDataAdapter.getRefreshData(data, false);
                ppLoadController.endRefreshSpinner();

                //success with no more
                //done(true)

                //failed
                //quit("test failed");
            }
        }, 100);
    }

    @Override
    public void loadMoreData() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                ArrayList<Long> data = new ArrayList();
                for (int k = 0; k < 10; k++) {
                    data.add(baseLong + j++);
                }
                //success with more
                ppLoadController.ppLoadDataAdapter.loadMoreEnd(data, ++i == 10);
                ppLoadController.removeLoadMoreSpinner();
                //success with no more
                //done(true)

                //failed
                //quit("test failed");
            }
        }, 100);
    }

    class PPAdapter extends PPLoadDataAdapter<Long, Long> {

        public class PPViewHolder extends RecyclerView.ViewHolder {
            TextView textView;

            public PPViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView.findViewById(android.R.id.text1);
            }
        }

        public PPAdapter(List<Long> data) {
            this.data = data;
        }

        @Override
        protected RecyclerView.ViewHolder ppOnCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(android.R.layout.simple_list_item_1, parent, false);

            return new PPViewHolder(v);
        }

        @Override
        protected int ppGetItemViewType(int position) {
            return 0;
        }

        @Override
        public void ppOnBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((PPViewHolder) holder).textView.setText(data.get(position).toString());
        }

        @Override
        protected void addLoadMoreData(final List data) {
            int oldSize = this.data.size();
            int size = data.size();

            this.data.addAll(data);

            notifyItemRangeInserted(oldSize, size);
        }

        @Override
        protected void addRefreshData(List data) {
            int oldSize = this.data.size();
            int size = data.size();

            this.data.clear();
            notifyItemRangeRemoved(0, oldSize);

            this.data.addAll(data);
            Log.v("pplog", "addRefreshData:" + size);
            notifyItemRangeInserted(0, size);
        }
    }
}
