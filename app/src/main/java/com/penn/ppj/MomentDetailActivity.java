package com.penn.ppj;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.gson.JsonArray;
import com.jakewharton.rxbinding2.view.RxView;
import com.penn.ppj.databinding.ActivityMomentDetailBinding;
import com.penn.ppj.databinding.CommentCellBinding;
import com.penn.ppj.databinding.MomentDetailHeadBinding;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.model.realm.MomentDetail;
import com.penn.ppj.ppEnum.CommentStatus;
import com.penn.ppj.ppEnum.RelatedUserType;
import com.penn.ppj.util.PPHelper;
import com.penn.ppj.util.PPJSONObject;
import com.penn.ppj.util.PPRetrofit;
import com.penn.ppj.util.PPWarn;
import com.squareup.picasso.Picasso;

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
import io.realm.OrderedCollectionChangeSet;
import io.realm.OrderedRealmCollectionChangeListener;
import io.realm.Realm;
import io.realm.RealmChangeListener;
import io.realm.RealmResults;
import io.realm.Sort;

import static com.penn.ppj.util.PPHelper.calculateHeadHeight;
import static com.penn.ppj.util.PPHelper.hideKeyboard;
import static com.penn.ppj.util.PPHelper.ppFromString;
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

    private int likeButtonMaxOffset;

    private int mainImageMaxOffset;

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

        binding.mainRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (binding.mainRecyclerView.computeVerticalScrollOffset() < likeButtonMaxOffset) {
                    binding.likeFabToggle.setTranslationY(-binding.mainRecyclerView.computeVerticalScrollOffset());
                } else {
                    binding.likeFabToggle.setTranslationY(-likeButtonMaxOffset);
                }

                if (binding.mainRecyclerView.computeVerticalScrollOffset() < mainImageMaxOffset) {
                    binding.mainImageView.setTranslationY(-binding.mainRecyclerView.computeVerticalScrollOffset());
                    binding.mainImageView.setElevation(0);
                } else {
                    binding.mainImageView.setTranslationY(-mainImageMaxOffset);
                    Log.v("pplog", "setElevation");
                    binding.mainImageView.setElevation(16);
                }
            }
        });

//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        setSupportActionBar(toolbar);

        binding.commentTextInputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    binding.commentTextInputEditText.clearFocus();
                    hideKeyboard(MomentDetailActivity.this);
                }
            }
        });

        //like按钮监控
        Observable<Object> likeButtonObservable = RxView.clicks(binding.likeFabToggle)
                .debounce(200, TimeUnit.MILLISECONDS);

        likeButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                boolean liked = binding.likeFabToggle.isChecked();
                                binding.likeFabToggle.setChecked(!liked);
                                likeOrUnlikeMoment(!liked);
                            }
                        }
                );

        //send comment按钮监控
        Observable<Object> commentButtonObservable = RxView.clicks(binding.sendCommentImageButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        commentButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                sendComment();
                            }
                        }
                );

        mainImageMaxOffset = PPHelper.calculateHeadMaxOffset(this);

        //由于momentDetailHeadBinding是在onCreateViewHolder中初始化的, 怀疑 ppAdapter = new PPAdapter(comments);是个异步操作
        //这里不用延时的话会导致momentDetailHeadBinding null错误
        binding.mainRecyclerView.post(new Runnable() {
            @Override
            public void run() {
                getMomentDetail();
            }
        });

        //容错 currentUserAvatar为空
        if (TextUtils.isEmpty(PPHelper.currentUserAvatar)) {
            Log.v("pplog", "currentUserAvatar不应该为空");
            return;
        }

        Picasso.with(this).load(PPHelper.get80ImageUrl(PPHelper.currentUserAvatar)).into(binding.commentAvatarCircleImageView);
    }

    private void sendComment() {
        String content = binding.commentTextInputEditText.getText().toString();
        if (TextUtils.isEmpty(content)) {
            return;
        }

        //插入到本地数据库
        final long now = System.currentTimeMillis();

        final Comment comment = new Comment();
        comment.setKey(now + "_" + PPHelper.currentUserId);
        comment.setUserId(PPHelper.currentUserId);
        comment.setMomentId(momentId);
        comment.setCreateTime(now);
        comment.setNickname(PPHelper.currentUserNickname);
        comment.setAvatar(PPHelper.currentUserAvatar);
        comment.setContent(content);
        comment.setStatus(CommentStatus.LOCAL);
        comment.setLastVisitTime(now);

        realm.beginTransaction();

        realm.copyToRealm(comment);

        realm.commitTransaction();

        //清空输入框
        binding.commentTextInputEditText.setText("");
        binding.commentTextInputEditText.clearFocus();
        binding.mainRecyclerView.smoothScrollToPosition(0);
        PPHelper.hideKeyboard(this);

        //发送comment
        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("id", momentId)
                .put("content", content)
                .put("isPrivate", "false");

        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api("moment.reply", jBody.getJSONObject());

        final Comment comment2 = realm.where(Comment.class).equalTo("key", now + "_" + PPHelper.currentUserId).findFirst();

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

                                Log.v("pplog", "in:" + realm.isInTransaction());
                                realm.beginTransaction();
                                comment2.setStatus(CommentStatus.NET);
                                realm.copyToRealm(comment2);

                                realm.commitTransaction();
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                realm.beginTransaction();
                                comment2.setStatus(CommentStatus.FAILED);
                                realm.copyToRealm(comment2);

                                realm.commitTransaction();
                                PPHelper.error(throwable.toString());
                            }
                        }
                );
    }

    @Override
    protected void onDestroy() {
        realm.close();
        super.onDestroy();
    }

    private void likeOrUnlikeMoment(boolean like) {
        String api = like ? "moment.like" : "moment.unLike";

        PPJSONObject jBody = new PPJSONObject();
        jBody
                .put("id", momentId);

        Log.v("pplog252", api);
        final Observable<String> apiResult = PPRetrofit.getInstance()
                .api(api, jBody.getJSONObject());

        apiResult
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMap(new Function<String, ObservableSource<String>>() {
                    @Override
                    public ObservableSource<String> apply(@NonNull String s) throws Exception {
                        PPWarn ppWarn = PPHelper.ppWarning(s);

                        if (ppWarn != null) {
                            throw new Exception(ppWarn.msg);
                        }

                        //请求服务器最新MomentDetail记录
                        PPJSONObject jBody1 = new PPJSONObject();
                        jBody1
                                .put("id", momentId)
                                .put("checkFollow", "1")
                                .put("checkLike", "1");

                        final Observable<String> apiResult1 = PPRetrofit.getInstance().api("moment.detail", jBody1.getJSONObject());

                        return apiResult1;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<String>() {
                            @Override
                            public void accept(@NonNull String s) throws Exception {
                                updateLocalMomentDetail(s);
                            }
                        },
                        new Consumer<Throwable>() {
                            @Override
                            public void accept(@NonNull Throwable throwable) throws Exception {
                                PPHelper.error(throwable.toString());
                                restoreLocalMomentDetail();
                            }
                        }
                );
    }

    private void updateLocalMomentDetail(String momentDetailString) {
        String momentId = ppFromString(momentDetailString, "data._id").getAsString();

        realm.beginTransaction();

        long now = System.currentTimeMillis();

        MomentDetail momentDetail = realm.where(MomentDetail.class).equalTo("id", momentId).findFirst();
        momentDetail.setLiked(ppFromString(momentDetailString, "data.like").getAsInt() == 1 ? true : false);
        momentDetail.setLastVisitTime(now);

        realm.commitTransaction();

        binding.setData(momentDetail);
    }

    private void restoreLocalMomentDetail() {
        binding.setData(momentDetail);
    }

    private void setupBindingData(final MomentDetail momentDetail) {
        Log.v("pplog", "setupBindingData start");
        if (momentDetail == null) {
            Log.v("pplog", "setupBindingData cancel");
            return;
        }

        synchronized (this) {
            if (this.momentDetail == null) {
                Log.v("pplog255", "setVisibility:" + (momentDetail.getUserId().equals(PPHelper.currentUserId)));
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
        final int floatingButtonHalfHeight = binding.likeFabToggle.getHeight() / 2;

        likeButtonMaxOffset = PPHelper.calculateHeadMaxOffset(MomentDetailActivity.this) + titleHeight;

        Log.v("pplog250", "" + titleHeight + "," + headPicHeight + "," + floatingButtonHalfHeight);

        PPHelper.likeButtonAppear(this, binding.likeFabToggle, titleHeight + headPicHeight - floatingButtonHalfHeight);
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
        momentDetail.setId(ppFromString(momentDetailString, "data._id").getAsString());
        momentDetail.setGeo(ppFromString(momentDetailString, "data.location.geo.0").getAsString() + "," + ppFromString(momentDetailString, "data.location.geo.1").getAsString());
        momentDetail.setAddress(ppFromString(momentDetailString, "data.location.detail").getAsString());
        momentDetail.setCity(ppFromString(momentDetailString, "data.location.city").getAsString());
        momentDetail.setUserId(ppFromString(momentDetailString, "data._creator.id").getAsString());
        momentDetail.setContent(ppFromString(momentDetailString, "data.content").getAsString());
        momentDetail.setLiked(ppFromString(momentDetailString, "data.like").getAsInt() == 1 ? true : false);
        momentDetail.setCreateTime(ppFromString(momentDetailString, "data.createTime").getAsLong());
        momentDetail.setPic(ppFromString(momentDetailString, "data.pics.0").getAsString());
        momentDetail.setAvatar(ppFromString(momentDetailString, "data._creator.head").getAsString());
        momentDetail.setNickname(ppFromString(momentDetailString, "data._creator.nickname").getAsString());
        momentDetail.setLastVisitTime(now);

        JsonArray ja = ppFromString(commentsString, "data.list").getAsJsonArray();

        try (Realm realm = Realm.getDefaultInstance()) {
            realm.beginTransaction();

            for (int i = 0; i < ja.size(); i++) {

                long createTime = PPHelper.ppFromString(commentsString, "data.list." + i + ".createTime").getAsLong();
                String userId = PPHelper.ppFromString(commentsString, "data.list." + i + "._creator.id").getAsString();

                Comment comment = new Comment();
                comment.setKey(createTime + "_" + userId);
                comment.setId(PPHelper.ppFromString(commentsString, "data.list." + i + "._id").getAsString());
                comment.setUserId(PPHelper.ppFromString(commentsString, "data.list." + i + "._creator.id").getAsString());
                comment.setMomentId(momentId);
                comment.setContent(PPHelper.ppFromString(commentsString, "data.list." + i + ".content").getAsString());
                comment.setCreateTime(createTime);
                comment.setNickname(PPHelper.ppFromString(commentsString, "data.list." + i + "._creator.nickname").getAsString());
                comment.setAvatar(PPHelper.ppFromString(commentsString, "data.list." + i + "._creator.head").getAsString());
                comment.setStatus(CommentStatus.NET);
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
