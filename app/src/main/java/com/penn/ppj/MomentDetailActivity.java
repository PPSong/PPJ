package com.penn.ppj;

import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.content.Intent;
import android.databinding.BindingAdapter;
import android.databinding.DataBindingUtil;
import android.graphics.Bitmap;
import android.graphics.Interpolator;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.BounceInterpolator;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.JsonArray;
import com.penn.ppj.databinding.ActivityMomentDetailBinding;
import com.penn.ppj.databinding.CommentCellBinding;
import com.penn.ppj.databinding.MomentDetailHeadBinding;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.MomentDetail;
import com.penn.ppj.model.realm.Pic;
import com.penn.ppj.model.realm.UserHomePage;
import com.penn.ppj.ppEnum.MomentStatus;
import com.penn.ppj.ppEnum.PicStatus;
import com.penn.ppj.ppEnum.RelatedUserType;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import junit.framework.Test;

import java.util.List;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmList;
import io.realm.RealmModel;
import io.realm.RealmResults;
import io.realm.Sort;

import static android.R.attr.data;
import static android.R.attr.resource;
import static com.penn.ppj.PPApplication.getContext;
import static com.penn.ppj.util.PPHelper.calculateHeadHeight;
import static io.reactivex.Observable.zip;

public class MomentDetailActivity extends AppCompatActivity {
    private String momentId;

    private ActivityMomentDetailBinding binding;

    private Realm realm;

    private MomentDetail momentDetail;

    private RealmResults<Comment> comments;

    private RealmChangeListener<MomentDetail> momentDetailChangeListener;

    private OrderedRealmCollectionChangeListener<RealmResults<Comment>> commentsChangeListener;

    private PPAdapter ppAdapter;

    private LinearLayoutManager linearLayoutManager;

    private MomentDetailHeadBinding momentDetailHeadBinding;

    private final View.OnClickListener commentOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

        }
    };

    class PPAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        private static final int HEAD = 1;
        private static final int COMMENT = 2;

        private List<Comment> data;

        public class PPHoldView extends RecyclerView.ViewHolder {
            private CommentCellBinding binding;

            public PPHoldView(CommentCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        public class PPHead extends RecyclerView.ViewHolder {
            private MomentDetailHeadBinding binding;

            public PPHead(MomentDetailHeadBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                binding.setData(momentDetail);
//                final TextView tv = binding.contentTextView;
//                ViewTreeObserver vto = tv.getViewTreeObserver();
//                vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
//
//                    @Override
//                    public void onGlobalLayout() {
//                        Log.v("pplog", "height:" + tv.getHeight());
////                        ViewTreeObserver obs = tv.getViewTreeObserver();
////
////                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
////                            obs.removeOnGlobalLayoutListener(this);
////                        } else {
////                            obs.removeGlobalOnLayoutListener(this);
////                        }
//                    }
//
//                });
            }
        }

        public PPAdapter(List<Comment> data) {
            this.data = data;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            Log.v("pplog", "onCreateViewHolder");
            if (viewType == HEAD) {
                final MomentDetailHeadBinding tmpMomentDetailHeadBinding = MomentDetailHeadBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
                tmpMomentDetailHeadBinding.likesButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AnimatedVectorDrawable) tmpMomentDetailHeadBinding.likesButton.getCompoundDrawables()[1]).start();
                        //pptodo if users > 0
                        showRelatedUsers(RelatedUserType.FAN);
                    }
                });

                tmpMomentDetailHeadBinding.viewsButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AnimatedVectorDrawable) tmpMomentDetailHeadBinding.viewsButton.getCompoundDrawables()[1]).start();
                        //pptodo if users > 0
                        showRelatedUsers(RelatedUserType.FOLLOW);
                    }
                });

                tmpMomentDetailHeadBinding.shareButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        ((AnimatedVectorDrawable) tmpMomentDetailHeadBinding.shareButton.getCompoundDrawables()[1]).start();
                        //pptodo if users > 0
                        showRelatedUsers(RelatedUserType.FRIEND);
                    }
                });

                momentDetailHeadBinding = tmpMomentDetailHeadBinding;
                return new PPHead(tmpMomentDetailHeadBinding);
            } else if (viewType == COMMENT) {
                CommentCellBinding commentCellBinding = CommentCellBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

                commentCellBinding.getRoot().setOnClickListener(commentOnClickListener);

                return new PPHoldView(commentCellBinding);
            } else {
                throw new Error("MomentDetailActivity中comment没有找到viewType");
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (position == 0) {
                ((PPHead) holder).binding.setData(momentDetail);
                Log.v("pplog", "height setdata2:" + (momentDetail == null ? "null" : momentDetail.getContent()));
            } else {
                Comment comment = data.get(position - 1);
                ((PPHoldView) holder).binding.setData(comment);
            }
        }

        @Override
        public int getItemViewType(int position) {
            return position == 0 ? HEAD : COMMENT;
        }

        @Override
        public int getItemCount() {
            return data.size() + 1;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        realm = Realm.getDefaultInstance();

        Intent intent = getIntent();
        momentId = intent.getStringExtra("momentId");

        binding = DataBindingUtil.setContentView(this, R.layout.activity_moment_detail);

        momentDetailChangeListener = new RealmChangeListener<MomentDetail>() {
            @Override
            public void onChange(MomentDetail element) {
                Log.v("pplog", element.getId() + "," + element.getContent());
                binding.setData(element);
                momentDetailHeadBinding.setData(element);
            }
        };

        commentsChangeListener = new OrderedRealmCollectionChangeListener<RealmResults<Comment>>() {
            @Override
            public void onChange(RealmResults<Comment> collection, OrderedCollectionChangeSet changeSet) {
                // `null`  means the async query returns the first time.
                if (changeSet == null) {
                    ppAdapter.notifyDataSetChanged();
                    return;
                }
                // For deletions, the adapter has to be notified in reverse order.
                OrderedCollectionChangeSet.Range[] deletions = changeSet.getDeletionRanges();
                for (int i = deletions.length - 1; i >= 0; i--) {
                    OrderedCollectionChangeSet.Range range = deletions[i];
                    ppAdapter.notifyItemRangeRemoved(range.startIndex + 1, range.length);
                }

                OrderedCollectionChangeSet.Range[] insertions = changeSet.getInsertionRanges();
                for (OrderedCollectionChangeSet.Range range : insertions) {
                    ppAdapter.notifyItemRangeInserted(range.startIndex + 1, range.length);
                }

                OrderedCollectionChangeSet.Range[] modifications = changeSet.getChangeRanges();
                for (OrderedCollectionChangeSet.Range range : modifications) {
                    ppAdapter.notifyItemRangeChanged(range.startIndex + 1, range.length);
                }
            }
        };

        comments = realm.where(Comment.class).equalTo("momentId", momentId).findAllSorted("createTime", Sort.DESCENDING);
        comments.addChangeListener(commentsChangeListener);

        ppAdapter = new PPAdapter(comments);
        linearLayoutManager = new LinearLayoutManager(this);

        binding.mainRecyclerView.setLayoutManager(linearLayoutManager);
        binding.mainRecyclerView.setAdapter(ppAdapter);

        final int maxOffset = PPHelper.calculateHeadMaxOffset(MomentDetailActivity.this);
        binding.mainRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (binding.mainRecyclerView.computeVerticalScrollOffset() < maxOffset) {
                    binding.mainImageView.setTranslationY(-binding.mainRecyclerView.computeVerticalScrollOffset());
                    binding.mainImageView.setElevation(0);
                    binding.likeFloatingActionButton.setTranslationY(binding.mainImageView.getHeight() - binding.likeFloatingActionButton.getHeight() / 2 - binding.mainRecyclerView.computeVerticalScrollOffset());
                } else {
                    binding.mainImageView.setTranslationY(-maxOffset);
                    binding.mainImageView.setElevation(16);
                    binding.likeFloatingActionButton.setTranslationY(binding.mainImageView.getHeight() - binding.likeFloatingActionButton.getHeight() / 2 - maxOffset);
                }
            }
        });

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        binding.likeFloatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getMomentDetail();
            }
        });

        //由于momentDetailHeadBinding是在onCreateViewHolder中初始化的, 怀疑 ppAdapter = new PPAdapter(comments);是个异步操作
        //这里不用延时的话会导致momentDetailHeadBinding null错误
        binding.mainRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                getMomentDetail();
            }
        });
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    private void setupBindingData(MomentDetail momentDetail) {
        Log.v("pplog", "setupBindingData start");
        if (momentDetail == null) {
            Log.v("pplog", "setupBindingData cancel");
            return;
        }

        synchronized (this) {
            if (this.momentDetail == null) {
                this.momentDetail = momentDetail;
                this.momentDetail.addChangeListener(momentDetailChangeListener);
                Log.v("pplog", "setupBindingData ok");
                binding.setData(this.momentDetail);
                momentDetailHeadBinding.setData(this.momentDetail);

                //由于layout展开需要时间, 所以设置延时, 要不然获得的高度为0
                binding.mainRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        setupLikeButton();
                    }
                }, 200);
            } else {
                Log.v("pplog", "setupBindingData this.momentDetail != null");
            }
        }
    }

    private void setupLikeButton() {
        final int titleHeight = momentDetailHeadBinding.contentTextView.getHeight();
        final int headPicHeight = calculateHeadHeight(this);
        final int floatingButtonHalfHeight = binding.likeFloatingActionButton.getHeight() / 2;

        PPHelper.likeButtonAppear(this, binding.likeFloatingActionButton, titleHeight + headPicHeight - floatingButtonHalfHeight);
    }

    private void getMomentDetail() {
        //读取本地MomentDetail记录
        MomentDetail tmpMomentDetail = realm.where(MomentDetail.class).equalTo("id", momentId).findFirst();

        if (tmpMomentDetail != null) {
            try (Realm realm = Realm.getDefaultInstance()) {

                realm.beginTransaction();

                tmpMomentDetail.setLastVisitTime(System.currentTimeMillis());

                realm.commitTransaction();
            }
        }

        setupBindingData(tmpMomentDetail);

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
                                PPWarn ppWarn = PPHelper.ppWarning(result[0]);

                                if (ppWarn != null) {
                                    throw new Exception(ppWarn.msg);
                                }

                                PPWarn ppWarn2 = PPHelper.ppWarning(result[1]);

                                if (ppWarn2 != null) {
                                    throw new Exception(ppWarn2.msg);
                                }

                                processMomentDetailAndComments(result[0], result[1]);

                                if (momentDetail == null) {
                                    setupBindingData(realm.where(MomentDetail.class).equalTo("id", momentId).findFirst());
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
        //构造comment detail和comments
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

    private void showRelatedUsers(RelatedUserType relatedUserType) {
        RelatedUsersBottomSheetFragment relatedUsersBottomSheetFragment = RelatedUsersBottomSheetFragment.newInstance(relatedUserType);
        relatedUsersBottomSheetFragment.show(getSupportFragmentManager(), relatedUsersBottomSheetFragment.getTag());
    }
}
