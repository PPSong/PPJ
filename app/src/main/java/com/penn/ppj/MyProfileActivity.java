package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
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
import android.widget.ImageView;

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
import com.penn.ppj.messageEvent.MomentPublishEvent;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.MomentCreating;
import com.penn.ppj.model.realm.MomentDetail;
import com.penn.ppj.model.realm.MyProfile;
import com.penn.ppj.model.realm.RelatedUser;
import com.penn.ppj.ppEnum.MomentStatus;
import com.penn.ppj.ppEnum.RelatedUserType;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;

import org.greenrobot.eventbus.EventBus;
import org.json.JSONArray;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
import static com.penn.ppj.util.PPHelper.ppWarning;

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

        this.myProfile.addChangeListener(myProfileChangeListener);
        binding.setData(this.myProfile);
        setupChangeAvatar();

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

    private void setupChangeAvatar() {
        Log.v("pplog509", "setupChangeAvatar");
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
        Bitmap myBitmap = BitmapFactory.decodeFile(result.getImages().get(0).getCompressPath());

        binding.avatarImageView.setImageBitmap(myBitmap);

        //上传avatar
        //申请上传图片的token
        final String key = PPHelper.currentUserId + "_head_" + System.currentTimeMillis();
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("type", "public")
                .put("filename", key);

        final Observable<String> requestToken = PPRetrofit.getInstance().api("system.generateUploadToken", jBody.getJSONObject());

        //上传avatar
        PPJSONObject jBody1 = new PPJSONObject();
        jBody1
                .put("head", key);

        final Observable<String> apiResult1 = PPRetrofit.getInstance()
                .api("user.changeHead", jBody1.getJSONObject());


        File file = new File(Environment.getExternalStorageDirectory(), "/temp/" + "tmp.jpg");
        int size = (int) file.length();
        final byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            Log.v("pplog", "MomentCreating.setPic error:" + e.toString());
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.v("pplog", "MomentCreating.setPic error:" + e.toString());
            e.printStackTrace();
        }

        requestToken
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(
                        new Function<String, ObservableSource<String>>() {
                            @Override
                            public ObservableSource<String> apply(@NonNull String s) throws Exception {
                                PPWarn ppWarn = ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception("ppError:" + ppWarn.msg + ":" + key);
                                }
                                String token = PPHelper.ppFromString(s, "data.token").getAsString();
                                return PPHelper.uploadSingleImage(bytes, key, token);
                            }
                        }
                )
                .observeOn(Schedulers.io())
                .flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(@NonNull String s) throws Exception {
                        return apiResult1;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                PPWarn ppWarn = ppWarning(s);
                                if (ppWarn != null) {
                                    throw new Exception("ppError:" + ppWarn.msg);
                                }

                                realm.beginTransaction();

                                myProfile.setAvatar(key);

                                realm.commitTransaction();

                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                try (Realm realm = Realm.getDefaultInstance()) {
                                    binding.setData(myProfile);
                                }
                                PPHelper.error(throwable.toString());
                            }
                        }
                );

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
