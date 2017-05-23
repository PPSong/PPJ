package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.net.Uri;
import android.os.Environment;
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
import com.jph.takephoto.app.TakePhoto;
import com.jph.takephoto.app.TakePhotoActivity;
import com.jph.takephoto.app.TakePhotoFragmentActivity;
import com.jph.takephoto.compress.CompressConfig;
import com.jph.takephoto.model.TResult;
import com.penn.ppj.databinding.ActivityMomentDetailBinding;
import com.penn.ppj.databinding.ActivityMyProfileBinding;
import com.penn.ppj.databinding.MomentOverviewCellBinding;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.MomentDetail;
import com.penn.ppj.model.realm.MyProfile;
import com.penn.ppj.model.realm.RelatedUser;
import com.penn.ppj.ppEnum.RelatedUserType;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.baidu.location.h.j.U;
import static com.baidu.location.h.j.m;
import static com.penn.ppj.PPApplication.getContext;
import static com.penn.ppj.R.id.liked;

public class MyProfileActivity extends TakePhotoFragmentActivity {

    private ActivityMyProfileBinding binding;

    private Realm realm;

    private MyProfile myProfile;

    private RealmChangeListener<MyProfile> myProfileChangeListener;

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

        binding = DataBindingUtil.setContentView(this, R.layout.activity_my_profile);

        //fans按钮监控
        Observable<Object> fansButtonObservable = RxView.clicks(binding.fansButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        fansButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                ((AnimatedVectorDrawable) binding.fansButton.getCompoundDrawables()[1]).start();
                               // showRelatedUsers(RelatedUserType.FAN);
                            }
                        }
                );

        //follows按钮监控
        Observable<Object> followsButtonObservable = RxView.clicks(binding.followsButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        followsButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                ((AnimatedVectorDrawable) binding.followsButton.getCompoundDrawables()[1]).start();
                               // showRelatedUsers(RelatedUserType.FOLLOW);
                            }
                        }
                );

        //friends按钮监控
        Observable<Object> friendsButtonObservable = RxView.clicks(binding.friendsButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        friendsButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                ((AnimatedVectorDrawable) binding.friendsButton.getCompoundDrawables()[1]).start();
                               // showRelatedUsers(RelatedUserType.FRIEND);
                            }
                        }
                );

        realm = Realm.getDefaultInstance();

        myProfile = realm.where(MyProfile.class).equalTo("userId", PPHelper.currentUserId).findFirst();

        myProfileChangeListener = new RealmChangeListener<MyProfile>() {
            @Override
            public void onChange(MyProfile element) {
                binding.setData(element);
            }
        };

        setupBindingData(myProfile);

        PPHelper.refreshMyProfile();

        mBottomSheetBehavior = BottomSheetBehavior.from(binding.bottomSheet1);

        mBottomSheetBehavior.setHideable(false);

        mBottomSheetBehavior.setPeekHeight(PPHelper.calculateMomentOverHeight());
        mBottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

        moments = realm.where(Moment.class).equalTo("userId", PPHelper.currentUserId).findAllSorted("createTime", Sort.DESCENDING);

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

        setupButtonsText();
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    private void setupButtonsText() {
        long fansNum = realm.where(RelatedUser.class).equalTo("type", RelatedUserType.FAN.toString()).count();
        long followsNum = realm.where(RelatedUser.class).equalTo("type", RelatedUserType.FOLLOW.toString()).count();
        long friendsNum = realm.where(RelatedUser.class).equalTo("type", RelatedUserType.FRIEND.toString()).count();

        binding.fansButton.setText("" + fansNum + getString(R.string.fan));
        binding.followsButton.setText("" + followsNum + getString(R.string.follow));
        binding.friendsButton.setText("" + friendsNum + getString(R.string.friend));
    }

    private void restoreLocalMyProfile() {
        binding.setData(myProfile);
    }

    private void setupBindingData(MyProfile myProfile) {

        synchronized (this) {
            if (this.myProfile == null) {
                this.myProfile = myProfile;
                this.myProfile.addChangeListener(myProfileChangeListener);
                binding.setData(this.myProfile);
                setupChangeAvatar();
            }
        }
    }

    private void setupChangeAvatar() {
        binding.avatarImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto();
            }
        });
    }

    private void takePhoto() {
        File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + "tmp.jpg");
        if (!file.getParentFile().exists()) file.getParentFile().mkdirs();
        Uri imageUri = Uri.fromFile(file);

        CompressConfig config = new CompressConfig.Builder()
                .setMaxSize(1024 * 1024)
                .setMaxPixel(1024)
                .create();

        TakePhoto takePhoto = getTakePhoto();

        takePhoto.onEnableCompress(config, true);

        takePhoto.onPickFromCapture(imageUri);
    }

    @Override
    public void takeFail(TResult result, String msg) {
        PPHelper.error(msg);
    }

    @Override
    public void takeSuccess(TResult result) {
        super.takeSuccess(result);

        //修改页面上的avatar
        realm.beginTransaction();
        myProfile.setAvatar(result.getImages().get(0).getCompressPath());
        realm.commitTransaction();

        //上传avatar

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
