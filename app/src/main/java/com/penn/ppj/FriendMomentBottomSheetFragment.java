package com.penn.ppj;

import android.app.Dialog;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.BottomSheetDialogFragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.penn.ppj.databinding.FriendMomentBottomSheetBinding;
import com.penn.ppj.databinding.MomentOverviewCellBinding;
import com.penn.ppj.databinding.RelatedUserBottomSheetBinding;
import com.penn.ppj.databinding.RelatedUserCellBinding;
import com.penn.ppj.model.realm.Moment;
import com.penn.ppj.model.realm.MomentDetail;
import com.penn.ppj.model.realm.RelatedUser;
import com.penn.ppj.ppEnum.RelatedUserType;
import com.penn.ppj.util.PPHelper;

import java.util.List;

import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;

/**
 * Created by Obaro on 01/08/2016.
 */
public class FriendMomentBottomSheetFragment extends BottomSheetDialogFragment {
    private FriendMomentBottomSheetBinding binding;

    private Dialog dialog;

    private String userId;
    private Realm realm;
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

    public static FriendMomentBottomSheetFragment newInstance(String  userId) {
        FriendMomentBottomSheetFragment fragment = new FriendMomentBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString("userId", userId);
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

        userId = getArguments().getString("userId");

        moments = realm.where(Moment.class).equalTo("userId", userId).findAllSorted("createTime", Sort.DESCENDING);

        gridLayoutManager = new GridLayoutManager(getContext(), PPHelper.calculateNoOfColumns(getContext()));

        momentOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int position = binding.mainRecyclerView.getChildAdapterPosition(v);
                Moment moment = moments.get(position);
                Intent intent = new Intent(getContext(), MomentDetail.class);
                intent.putExtra("momentId", moment.getId());
                startActivity(intent);
            }
        };

        binding = DataBindingUtil.inflate(
                inflater, R.layout.friend_moment_bottom_sheet, container, false);

        binding.titleTextView.setText(getString(R.string.moment));

        binding.mainRecyclerView.setLayoutManager(gridLayoutManager);

        ppAdapter = new PPAdapter(moments);
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
