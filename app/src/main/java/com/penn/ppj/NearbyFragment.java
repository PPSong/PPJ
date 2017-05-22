package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.penn.ppj.databinding.FragmentNearbyBinding;
import com.penn.ppj.databinding.NearbyMomentOverviewCellBinding;
import com.penn.ppj.messageEvent.InitLoadingEvent;
import com.penn.ppj.model.Geo;
import com.penn.ppj.model.realm.CurrentUser;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.NearbyMoment;
import com.penn.ppj.model.realm.Pic;
import com.penn.ppj.ppEnum.MomentStatus;
import com.penn.ppj.ppEnum.PicStatus;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPLoadController;
import com.penn.ppj.util.PPLoadDataAdapter;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.zip.Inflater;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.T;
import static com.penn.ppj.R.string.moment;

public class NearbyFragment extends Fragment implements PPLoadController.LoadDataProvider {

    private FragmentNearbyBinding binding;

    private Realm realm;

    private RealmResults<NearbyMoment> data;

    private PPAdapter ppAdapter;
    private GridLayoutManager gridLayoutManager;
    private PPLoadController ppLoadController;

    private long earliestCreateTime;
    private String geoStr;

    private final View.OnClickListener momentOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            int position = binding.mainRecyclerView.getChildAdapterPosition(v);
            NearbyMoment nearbyMoment = data.get(position);
            Intent intent = new Intent(getContext(), MomentDetailActivity.class);
            intent.putExtra("momentId", nearbyMoment.getId());
            startActivity(intent);
        }
    };

    public NearbyFragment() {
        // Required empty public constructor
    }

    public static NearbyFragment newInstance() {
        NearbyFragment fragment = new NearbyFragment();

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
                inflater, R.layout.fragment_nearby, container, false);
        View view = binding.getRoot();
        //end common

        realm = Realm.getDefaultInstance();

        data = realm.where(NearbyMoment.class).findAllSorted("createTime", Sort.DESCENDING);
        data.addChangeListener(changeListener);

        binding.mainRecyclerView.setPadding(0, PPHelper.getStatusBarAddActionBarHeight(getContext()), 0, 0);
        binding.mainRecyclerView.setHasFixedSize(true);

        geoStr = PPHelper.getLatestGeoString();

        setup();

        return view;
    }

    @Override
    public void onDestroyView() {
        realm.close();
        super.onDestroyView();
    }

    private void setup() {
        final int spanNum = PPHelper.calculateNoOfColumns(getContext());
        gridLayoutManager = new GridLayoutManager(getContext(), spanNum);

        ppAdapter = new PPAdapter(data);
        ppLoadController = new PPLoadController(binding.mainSwipeRefreshLayout, binding.mainRecyclerView, ppAdapter, gridLayoutManager, this);

        gridLayoutManager.setSpanSizeLookup(
                new GridLayoutManager.SpanSizeLookup() {
                    @Override
                    public int getSpanSize(int position) {
                        if (ppAdapter.getItemViewType(position) == PPLoadDataAdapter.LOAD_MORE_SPIN
                                || ppAdapter.getItemViewType(position) == PPLoadDataAdapter.NO_MORE) {
                            return spanNum;
                        } else {
                            return 1;
                        }

                    }
                }
        );
    }

    private final OrderedRealmCollectionChangeListener<RealmResults<NearbyMoment>> changeListener = new OrderedRealmCollectionChangeListener<RealmResults<NearbyMoment>>() {
        @Override
        public void onChange(RealmResults<NearbyMoment> collection, OrderedCollectionChangeSet changeSet) {
            // `null`  means the async query returns the first time.
//            if (changeSet == null) {
//                ppAdapter.notifyDataSetChanged();
//                return;
//            }
//            // For deletions, the adapter has to be notified in reverse order.
//            OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
//            for (int i = deletions.length - 1; i >= 0; i--) {
//                OrderedCollectionChangeSet.Range range = deletions[i];
//                ppAdapter.notifyItemRangeRemoved(range.startIndex, range.length);
//            }
//
//            OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
//            for (OrderedCollectionChangeSet.Range range : insertions) {
//                ppAdapter.notifyItemRangeInserted(range.startIndex, range.length);
//            }
//
//            OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
//            for (OrderedCollectionChangeSet.Range range : modifications) {
//                ppAdapter.notifyItemRangeChanged(range.startIndex, range.length);
//            }
            ppAdapter.notifyDataSetChanged();
        }
    };

    private boolean processMoment(String s, boolean refresh) {
        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();

            if (refresh) {
                realm.where(NearbyMoment.class).findAll().deleteAllFromRealm();
            }

            Log.v("pplog", s);
            JsonArray ja = PPHelper.ppFromString(s, "data.list").getAsJsonArray();

            int size = ja.size();

            for (int i = 0; i < size; i++) {
                long createTime = PPHelper.ppFromString(s, "data.list." + i + ".createTime").getAsLong();

                NearbyMoment nearbyMoment = new NearbyMoment();
                nearbyMoment.setKey(createTime + "_" + PPHelper.ppFromString(s, "data.list." + i + "._creator.id").getAsString());
                nearbyMoment.setId(PPHelper.ppFromString(s, "data.list." + i + "._id").getAsString());
                nearbyMoment.setCreateTime(createTime);
                nearbyMoment.setStatus(MomentStatus.NET);
                nearbyMoment.setAvatar(PPHelper.ppFromString(s, "data.list." + i + "._creator.head").getAsString());

                Pic pic = new Pic();
                pic.setKey(PPHelper.ppFromString(s, "data.list." + i + ".pics.0").getAsString());
                pic.setStatus(PicStatus.NET);
                nearbyMoment.setPic(pic);

                realm.insertOrUpdate(nearbyMoment);
                if (earliestCreateTime > createTime) {
                    earliestCreateTime = createTime;
                }
            }

            realm.commitTransaction();

            if (size < PPHelper.NEARBY_MOMENT_PAGE_SIZE) {
                Log.v("pplog500", "nomore:" + true);
                return true;
            } else {
                Log.v("pplog500", "nomore:" + false);
                return false;
            }
        }
    }

    @Override
    public void refreshData() {
        geoStr = PPHelper.getLatestGeoString();
        earliestCreateTime = Long.MAX_VALUE;

        Log.v("pplog310", geoStr + "," + earliestCreateTime);

        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("geo", geoStr)
                .put("beforeTime", "" + earliestCreateTime);

        final Observable<String> apiResult = PPRetrofit.getInstance().api("moment.search", jBody.getJSONObject());

        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                PPWarn ppWarn = PPHelper.ppWarning(s);

                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                ppLoadController.ppLoadDataAdapter.getRefreshData(data, processMoment(s, true));
                                ppLoadController.endRefreshSpinner();
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPHelper.error(throwable.toString());
                                ppLoadController.endRefreshSpinner();
                            }
                        }
                );
    }

    @Override
    public void loadMoreData() {
        Log.v("pplog370", "loadMoreData:" + geoStr + ",earliestCreateTime:" + earliestCreateTime);
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("geo", geoStr)
                .put("beforeTime", "" + earliestCreateTime);

        final Observable<String> apiResult = PPRetrofit.getInstance().api("moment.search", jBody.getJSONObject());

        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                PPWarn ppWarn = PPHelper.ppWarning(s);

                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                ppLoadController.ppLoadDataAdapter.loadMoreEnd(data, processMoment(s, false));
                                ppLoadController.removeLoadMoreSpinner();
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPHelper.error(throwable.toString());
                                ppLoadController.removeLoadMoreSpinner();
                            }
                        }
                );
    }

    public class PPAdapter extends PPLoadDataAdapter<NearbyMoment, NearbyMoment> {

        public PPAdapter(List<NearbyMoment> data) {
            this.data = data;
        }

        public class PPViewHolder extends RecyclerView.ViewHolder {
            private NearbyMomentOverviewCellBinding binding;

            public PPViewHolder(NearbyMomentOverviewCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        @Override
        protected RecyclerView.ViewHolder ppOnCreateViewHolder(ViewGroup parent, int viewType) {
            NearbyMomentOverviewCellBinding nearbyMomentOverviewCellBinding = NearbyMomentOverviewCellBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

            nearbyMomentOverviewCellBinding.getRoot().setOnClickListener(momentOnClickListener);

            return new PPViewHolder(nearbyMomentOverviewCellBinding);
        }

        @Override
        protected int ppGetItemViewType(int position) {
            return 0;
        }

        @Override
        public void ppOnBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            ((PPViewHolder) holder).binding.mainImageView.setImageResource(android.R.color.transparent);
            ((PPViewHolder) holder).binding.mainImageView.setBackgroundColor(PPHelper.getMomentOverviewBackgroundColor(position));
            ((PPViewHolder) holder).binding.setData(data.get(position));
//            if (!TextUtils.isEmpty(data.get(position).getPic().getKey())) {
//                ((PPViewHolder) holder).binding.setData(data.get(position));
////                Picasso.with(getContext())
////                        .load(PPHelper.get800ImageUrl(data.get(position).getPic().getKey()))
////                        //.placeholder(R.drawable.ab_gradient_dark)
////                        .into(((PPViewHolder) holder).binding.mainImageView);
////
////                ((PPViewHolder) holder).binding.mainImageView.setBackgroundColor(PPHelper.getMomentOverviewBackgroundColor(position));
////            }
//
////            if (!TextUtils.isEmpty(data.get(position).getAvatar())) {
////                Picasso.with(getContext())
////                        .load(PPHelper.get80ImageUrl(data.get(position).getAvatar()))
////                        //.placeholder(R.drawable.ab_gradient_dark)
////                        .into(((PPViewHolder) holder).binding.avatarCircleImageView);
//            }
        }

        @Override
        protected void addLoadMoreData(final List data) {
            //因为用了realm, 不需要做什么
        }

        @Override
        protected void addRefreshData(List data) {
            //因为用了realm, 不需要做什么
        }
    }
}
