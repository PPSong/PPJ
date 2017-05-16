package com.penn.ppj;

import android.content.Intent;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.penn.ppj.databinding.ActivityMomentDetailBinding;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.MomentDetail;
import com.penn.ppj.model.realm.Pic;
import com.penn.ppj.ppEnum.MomentStatus;
import com.penn.ppj.ppEnum.PicStatus;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import junit.framework.Test;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.RealmResults;

import static android.R.attr.data;
import static android.R.attr.resource;
import static io.reactivex.Observable.zip;

public class MomentDetailActivity extends AppCompatActivity {
    private String momentId;

    private ActivityMomentDetailBinding binding;

    private Realm realm;

    private MomentDetail momentDetail;

    private RealmResults<Comment> comments;

    private RealmChangeListener<MomentDetail> momentDetailChangeListener;

    //private PPAdapter ppAdapter;
    private LinearLayoutManager linearLayoutManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        realm = Realm.getDefaultInstance();

        Intent intent = getIntent();
        momentId = intent.getStringExtra("momentId");

        binding = DataBindingUtil.setContentView(this, R.layout.activity_moment_detail);

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

//        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });

        momentDetailChangeListener = new RealmChangeListener<MomentDetail>() {
            @Override
            public void onChange(MomentDetail element) {
                Log.v("pplog", element.getId() + "," + element.getContent());
                binding.setData(element);
            }
        };

        getMomentDetail();
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    @BindingAdapter({"bind:imageUrl"})
    public static void setImageViewResource(final ImageView imageView, String pic) {
        if (TextUtils.isEmpty(pic)) {
            return;
        }

        Picasso.with(PPApplication.getContext())
                .load(PPHelper.get800ImageUrl(pic))
                .into(new Target() {
                    //pptodo 改进取色方案
                    @Override
                    public void onBitmapLoaded(final Bitmap bitmap, Picasso.LoadedFrom from) {
                    /* Save the bitmap or do something with it here */
                        Palette p = Palette.from(bitmap).generate();
                        //Set it in the ImageView
                        imageView.setImageBitmap(bitmap);
                        imageView.setBackground(new ColorDrawable(p.getVibrantColor(PPApplication.getContext().getResources().getColor(R.color.colorPrimaryDark))));
                    }

                    @Override
                    public void onPrepareLoad(Drawable placeHolderDrawable) {
                    }

                    @Override
                    public void onBitmapFailed(Drawable errorDrawable) {
                    }
                });


    }

    private void getMomentDetail() {
        //读取本地MomentDetail记录
        try (Realm realm = Realm.getDefaultInstance()) {
            momentDetail = realm.where(MomentDetail.class).equalTo("id", momentId).findFirst();

            if (momentDetail != null) {
                binding.setData(momentDetail);
                momentDetail.addChangeListener(momentDetailChangeListener);
                //如本地有记录, 修改lastVisitTime
                realm.beginTransaction();

                momentDetail.setLastVisitTime(System.currentTimeMillis());

                realm.commitTransaction();
            }
        }

        //请求服务器最新MomentDetail记录
        PPJSONObject jBody1 = new PPJSONObject();
        jBody1
                .put("id", momentId)
                .put("checkFollow", "1")
                .put("checkLike", "1");

        final Observable<String> apiResult1 = PPRetrofit.getInstance().api("moment.detail", jBody1.getJSONObject());

        PPJSONObject jBody2 = new PPJSONObject();
        jBody2
                .put("id", momentId)
                .put("beforeTime", "")
                .put("afterTime", "1");

        final Observable<String> apiResult2 = PPRetrofit.getInstance().api("moment.getReplies", jBody2.getJSONObject());

        Observable
                .zip(
                        apiResult1, apiResult2, new BiFunction<String, String, String[]>() {

                            @Override
                            public String[] apply(@NonNull String s, @NonNull String s2) throws Exception {
                                String[] result = {s, s2};

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
                                //更新本地最新MomentDetail记录到最新MomentDetail记录
                                processMomentDetailAndComments(result[0], result[1]);

                                if (momentDetail == null) {
                                    momentDetail = realm.where(MomentDetail.class).equalTo("id", momentId).findFirst();
                                    binding.setData(momentDetail);
                                    momentDetail.addChangeListener(momentDetailChangeListener);
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

    private void processMomentDetailAndComments(String momentDetailString, String commentsString) {
        //构造moment detail和comments
        long now = System.currentTimeMillis();

        MomentDetail momentDetail = new MomentDetail();
        momentDetail.setId(PPHelper.ppFromString(momentDetailString, "data._id").getAsString());
        momentDetail.setContent(PPHelper.ppFromString(momentDetailString, "data.content").getAsString());
        momentDetail.setCreateTime(PPHelper.ppFromString(momentDetailString, "data.createTime").getAsLong());
        momentDetail.setPic(PPHelper.ppFromString(momentDetailString, "data.pics.0").getAsString());
        momentDetail.setAvatar(PPHelper.ppFromString(momentDetailString, "data._creator.head").getAsString());
        momentDetail.setNickname(PPHelper.ppFromString(momentDetailString, "data._creator.nickname").getAsString());
        momentDetail.setLastVisitTime(now);

        JsonArray ja = PPHelper.ppFromString(commentsString, "data.list").getAsJsonArray();

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();

            for (int i = 0; i < ja.size(); i++) {
                Comment comment = new Comment();
                comment.setId(PPHelper.ppFromString(commentsString, "data.list." + i + "._id").getAsString());
                comment.setMomentId(momentId);
                comment.setContent(PPHelper.ppFromString(commentsString, "data.list." + i + ".content").getAsString());
                comment.setCreateTime(PPHelper.ppFromString(commentsString, "data.list." + i + ".createTime").getAsLong());
                comment.setNickname(PPHelper.ppFromString(commentsString, "data.list." + i + "._creator.nickname").getAsString());
                comment.setAvatar(PPHelper.ppFromString(commentsString, "data.list." + i + "._creator.head").getAsString());
                comment.setLastVisitTime(now);
                realm.insertOrUpdate(comment);
            }

            //把服务器上已删除的comment从本地删掉
            realm.where(Comment.class).equalTo("momentId", momentId).notEqualTo("lastVisitTime", now).findAll().deleteAllFromRealm();

            realm.insertOrUpdate(momentDetail);

            realm.commitTransaction();
        }
    }
}
