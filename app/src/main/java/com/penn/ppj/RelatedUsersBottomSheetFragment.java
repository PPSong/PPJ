package com.penn.ppj;

import android.app.Dialog;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.penn.ppj.databinding.CommentCellBinding;
import com.penn.ppj.databinding.MomentDetailHeadBinding;
import com.penn.ppj.databinding.RelatedUserBottomSheetBinding;
import com.penn.ppj.databinding.RelatedUserCellBinding;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.RelatedUser;
import com.penn.ppj.model.realm.UserHomePage;
import com.penn.ppj.ppEnum.RelatedUserType;

import java.util.List;
import java.util.zip.Inflater;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

import static org.xmlpull.v1.XmlPullParser.COMMENT;

/**
 * Created by Obaro on 01/08/2016.
 */
public class RelatedUsersBottomSheetFragment extends BottomSheetDialogFragment {
    private RelatedUserBottomSheetBinding binding;

    private Dialog dialog;

    private RelatedUserType relatedUserType;
    private Realm realm;
    private RealmResults<RelatedUser> relatedUsers;
    private PPAdapter ppAdapter;
    private LinearLayoutManager linearLayoutManager;
    private View.OnClickListener relatedUserOnClickListener;

    class PPAdapter extends RecyclerView.Adapter<PPAdapter.PPViewHolder> {
        private List<RelatedUser> data;

        public class PPViewHolder extends RecyclerView.ViewHolder {
            private RelatedUserCellBinding binding;

            public PPViewHolder(RelatedUserCellBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }

        public PPAdapter(List<RelatedUser> data) {
            this.data = data;
        }

        @Override
        public PPViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            RelatedUserCellBinding relatedUserCellBinding = RelatedUserCellBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);

            relatedUserCellBinding.getRoot().setOnClickListener(relatedUserOnClickListener);

            return new PPViewHolder(relatedUserCellBinding);

        }

        @Override
        public void onBindViewHolder(PPViewHolder holder, int position) {

            holder.binding.setData(relatedUsers.get(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }
    }

    public static RelatedUsersBottomSheetFragment newInstance(RelatedUserType relatedUserType) {
        RelatedUsersBottomSheetFragment fragment = new RelatedUsersBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("relatedUserType", relatedUserType.toString());
        fragment.setArguments(args);

        return fragment;
    }

    private BottomSheetBehavior.BottomSheetCallback
            mBottomSheetBehaviorCallback = new BottomSheetBehavior.BottomSheetCallback() {
        @Override
        public void onStateChanged(@NonNull View bottomSheet, int newState) {
            if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                dismiss();
            }
        }

        @Override
        public void onSlide(@NonNull View bottomSheet, float slideOffset) {
        }
    };

    @Override
    public void setupDialog(final Dialog dialog, int style) {
        Log.v("pplog", "setupDialog");
        //super.setupDialog(dialog, style);
        //View contentView = View.inflate(getContext(), R.layout.related_user_bottom_sheet, null);
        this.dialog = dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        Log.v("pplog", "onCreateView");

        realm = Realm.getDefaultInstance();

        relatedUserType = RelatedUserType.valueOf(getArguments().getString("relatedUserType"));

        relatedUsers = realm.where(RelatedUser.class).equalTo("type", relatedUserType.toString()).findAllSorted("createTime", Sort.DESCENDING);

        linearLayoutManager = new LinearLayoutManager(getContext());

        relatedUserOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = binding.mainRecyclerView.getChildAdapterPosition(v);
                RelatedUser relatedUser = relatedUsers.get(position);
                Intent intent = new Intent(getContext(), UserHomePageActivity.class);
                intent.putExtra("userId", relatedUser.getUserId());
                startActivity(intent);
            }
        };

        binding = DataBindingUtil.inflate(
                inflater, R.layout.related_user_bottom_sheet, container, false);

        int tmpId = 0;
        if (relatedUserType == RelatedUserType.FOLLOW) {
            tmpId = R.string.follow;
        } else if (relatedUserType == RelatedUserType.FAN) {
            tmpId = R.string.fan;
        } else if (relatedUserType == RelatedUserType.FRIEND) {
            tmpId = R.string.friend;
        }

        binding.titleTextView.setText(getString(tmpId));

        binding.mainRecyclerView.setLayoutManager(linearLayoutManager);

        ppAdapter = new PPAdapter(relatedUsers);
        binding.mainRecyclerView.setAdapter(ppAdapter);

        dialog.setContentView(binding.getRoot());

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        Log.v("pplog", "onDestroyView");

        realm.close();

        super.onDestroyView();
    }
}
