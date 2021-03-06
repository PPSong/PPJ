package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.TransitionManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;

import com.google.gson.JsonArray;
import com.jakewharton.rxbinding2.view.RxView;
import com.penn.ppj.databinding.ActivityMomentDetailBinding;
import com.penn.ppj.databinding.ActivityUserHomePageBinding;
import com.penn.ppj.databinding.MomentOverviewCellBinding;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.MomentDetail;
import com.penn.ppj.model.realm.RelatedUser;
import com.penn.ppj.model.realm.UserHomePage;
import com.penn.ppj.ppEnum.RelatedUserType;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Function3;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.baidu.location.h.j.U;
import static com.baidu.location.h.j.s;
import static com.penn.ppj.PPApplication.getContext;
import static com.penn.ppj.R.id.liked;

public class UserHomePageActivity extends AppCompatActivity {

    private ActivityUserHomePageBinding binding;

    private Realm realm;

    private String userId;

    private UserHomePage userHomePage;

    private RealmChangeListener<UserHomePage> userHomePageChangeListener;

    private BottomSheetBehavior mBottomSheetBehavior;

    private RealmResults<Moment> moments;
    private PPAdapter ppAdapter;
    private GridLayoutManager gridLayoutManager;
    private View.OnClickListener momentOnClickListener;

    class PPAdapter extends RecyclerView.Adapter<PPAdapter.PPViewHolder> {
        private List<Moment> data;

        public class PPViewHolder extends RecyclerView.ViewHolder {
            private MomentOverviewCellBinding binding;

            public PPViewHolder(MomentOverviewCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        public PPAdapter(List<Moment> data) {
            this.data = data;
        }

        @Override
        public PPViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            MomentOverviewCellBinding momentOverviewCellBinding = MomentOverviewCellBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

            momentOverviewCellBinding.getRoot().setOnClickListener(momentOnClickListener);

            return new PPViewHolder(momentOverviewCellBinding);

        }

        @Override
        public void onBindViewHolder(PPViewHolder holder, int position) {

            holder.binding.setData(moments.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_user_home_page);

        binding.followToggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                TransitionManager.beginDelayedTransition(binding.mainLinearLayout);
            }
        });

        //like按钮监控
        Observable<Object> followButtonObservable = RxView.clicks(binding.followToggleButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        followButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                boolean followed = binding.followToggleButton.isChecked();
                                Log.v("pplog", "follow2:" + followed);
                                followOrUnfollowMoment(followed);
                            }
                        }
                );

        //back按钮监控
        Observable<Object> backButtonObservable = RxView.clicks(binding.backImageButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        backButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                               finish();
                            }
                        }
                );

        realm = Realm.getDefaultInstance();

        userId = getIntent().getStringExtra("userId");

        UserHomePage tmpUserHomePage = realm.where(UserHomePage.class).equalTo("userId", userId).findFirst();

        if (tmpUserHomePage != null) {
            try (Realm realm = Realm.getDefaultInstance()) {
                realm.beginTransaction();
                tmpUserHomePage.setLastVisitTime(System.currentTimeMillis());
                realm.commitTransaction();
            }
        }

        userHomePageChangeListener = new RealmChangeListener<UserHomePage>() {
            @Override
            public void onChange(UserHomePage element) {
                binding.setData(element);
            }
        };

        setupBindingData(tmpUserHomePage);

        getServerUserHomePage(userId);

        mBottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet1);

        mBottomSheetBehavior.setHideable(false);

        TypedValue tv = new TypedValue();
//        int actionBarHeight = 0;
//        if (getTheme().resolveAttribute(android.R.attr.actionBarSize, tv, true))
//        {
//            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,getResources().getDisplayMetrics());
//        }

        mBottomSheetBehavior.setPeekHeight(PPHelper.calculateMomentOverHeight());
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        moments = realm.where(Moment.class).equalTo("userId", userId).findAllSorted("createTime", Sort.DESCENDING);

        gridLayoutManager = new GridLayoutManager(this, PPHelper.calculateNoOfColumns(this));

        momentOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = binding.mainRecyclerView.getChildAdapterPosition(v);
                Moment moment = moments.get(position);
                Intent intent = new Intent(getContext(), MomentDetailActivity.class);
                intent.putExtra("momentId", moment.getId());
                startActivity(intent);
            }
        };

        binding.mainRecyclerView.setLayoutManager(gridLayoutManager);

        ppAdapter = new PPAdapter(moments);
        binding.mainRecyclerView.setAdapter(ppAdapter);
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    private void followOrUnfollowMoment(boolean follow) {
        String api = follow ? "friend.follow" : "friend.unFollow";

        Log.v("pplog253", "api:" + api + ",userId:" + userId);

        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("target", userId)
                .put("isFree", "true");

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api(api, jBody.getJSONObject());

        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                getServerUserHomePage(userId);
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPHelper.error(throwable.toString());
                                restoreLocalUserHomePage();
                            }
                        }
                );
    }

    private void setupButtonsText() {
        binding.meetButton.setText("" + userHomePage.getMeets() + getString(R.string.meet));
        binding.collectButton.setText("" + userHomePage.getCollects() + getString(R.string.collect));
        binding.beCollectedButton.setText("" + userHomePage.getBeCollecteds() + getString(R.string.be_collected));
    }

    private void restoreLocalUserHomePage() {
        binding.setData(userHomePage);
    }

    private void setupBindingData(UserHomePage userHomePage) {
        if (userHomePage == null) {
            return;
        }

        synchronized (this) {
            if (this.userHomePage == null) {
                this.userHomePage = userHomePage;
                this.userHomePage.addChangeListener(userHomePageChangeListener);
                binding.setData(this.userHomePage);
                setupButtonsText();
                setupChangeAvatar();
            }
        }
    }

    private void setupChangeAvatar() {
        if (userHomePage.getUserId() == PPHelper.currentUserId) {
            binding.avatarImageView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });
        }
    }

    private void getServerUserHomePage(final String userId) {
        PPJSONObject jBody1 = new PPJSONObject();
        jBody1
                .put("target", userId);

        final Observable<String> apiResult1 = PPRetrofit.getInstance().api("friend.isFollowed", jBody1.getJSONObject());

        PPJSONObject jBody2 = new PPJSONObject();
        jBody2
                .put("target", userId);

        final Observable<String> apiResult2 = PPRetrofit.getInstance().api("user.info", jBody2.getJSONObject());

        PPJSONObject jBody3 = new PPJSONObject();
        jBody3
                .put("target", userId);

        final Observable<String> apiResult3 = PPRetrofit.getInstance().api("user.view", jBody3.getJSONObject());

        Observable
                .zip(
                        apiResult1, apiResult2, apiResult3, new Function3<String, String, String, String[]>() {

                            @Override
                            public String[] apply(@NonNull String s, @NonNull String s2, @NonNull String s3) throws Exception {
                                String[] result = {s, s2, s3};

                                return result;
                            }
                        }
                )
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String[]>() {
                            @Override
                            public void accept(@NonNull String[] result) throws Exception {
                                //更新本地UserHomePage
                                PPWarn ppWarn = PPHelper.ppWarning(result[0]);

                                if (ppWarn != null) {
                                    Log.v("pplog511", "1");
                                    throw new Exception(ppWarn.msg);
                                }

                                PPWarn ppWarn2 = PPHelper.ppWarning(result[1]);

                                if (ppWarn2 != null) {
                                    Log.v("pplog511", "2");
                                    throw new Exception(ppWarn2.msg);
                                }

                                PPWarn ppWarn3 = PPHelper.ppWarning(result[2]);

                                if (ppWarn3 != null) {
                                    Log.v("pplog511", "3");
                                    throw new Exception(ppWarn3.msg);
                                }

                                processUserHomePage(result[0], result[1], result[2]);

                                if (userHomePage == null) {
                                    setupBindingData(realm.where(UserHomePage.class).equalTo("userId", userId).findFirst());
                                }
                            }
                        }
                        ,
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPHelper.error(throwable.toString());
                                Log.v("pplog", throwable.toString());
                            }
                        }
                );
    }

    private void processUserHomePage(String isFollowed, String userInfo, String userView) {
        //构造userHomePage
        long now = System.currentTimeMillis();

        UserHomePage userHomePage = new UserHomePage();
        userHomePage.setFollowed(PPHelper.ppFromString(isFollowed, "data.follow").getAsInt() == 0 ? false : true);
        userHomePage.setUserId(PPHelper.ppFromString(userInfo, "data.profile.id").getAsString());
        userHomePage.setNickname(PPHelper.ppFromString(userInfo, "data.profile.nickname").getAsString());
        userHomePage.setAvatar(PPHelper.ppFromString(userInfo, "data.profile.head").getAsString());
        userHomePage.setMeets(PPHelper.ppFromString(userView, "data.meets").getAsInt());
        userHomePage.setCollects(PPHelper.ppFromString(userView, "data.collects").getAsInt());
        userHomePage.setBeCollecteds(PPHelper.ppFromString(userView, "data.beCollecteds").getAsInt());
        userHomePage.setLastVisitTime(now);

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();

            realm.insertOrUpdate(userHomePage);

            realm.commitTransaction();
        }
    }

    private void showUserMoments(String userId) {
        FriendMomentBottomSheetFragment friendMomentBottomSheetFragment = FriendMomentBottomSheetFragment.newInstance(userId);
        friendMomentBottomSheetFragment.show(getSupportFragmentManager(), friendMomentBottomSheetFragment.getTag());
    }

    private void showRelatedUsers(RelatedUserType relatedUserType) {
        RelatedUsersBottomSheetFragment relatedUsersBottomSheetFragment = RelatedUsersBottomSheetFragment.newInstance(relatedUserType);
        relatedUsersBottomSheetFragment.show(getSupportFragmentManager(), relatedUsersBottomSheetFragment.getTag());
    }
}
