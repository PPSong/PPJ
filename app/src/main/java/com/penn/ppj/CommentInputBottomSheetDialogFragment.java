package com.penn.ppj;

import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetDialogFragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.google.gson.Gson;
import com.jakewharton.rxbinding2.view.RxView;
import com.jakewharton.rxbinding2.widget.RxCompoundButton;
import com.jakewharton.rxbinding2.widget.RxRadioGroup;
import com.jakewharton.rxbinding2.widget.RxTextView;
import com.penn.ppj.databinding.FragmentCommentInputBottomSheetDialogBinding;
import com.penn.ppj.model.realm.Comment;
import com.penn.ppj.util.PPHelper;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;

public class CommentInputBottomSheetDialogFragment extends BottomSheetDialogFragment {

    private FragmentCommentInputBottomSheetDialogBinding binding;
    public CommentViewModel commentViewModel;

    private BottomSheetDialogFragmentListener bottomSheetDialogFragmentListener;

    public interface BottomSheetDialogFragmentListener {
        public void setCommentViewModel(CommentViewModel commentViewModel);

        public void sendComment();
    }

    public static class CommentViewModel {
        public String targetUserId = "";
        public String targetNickname = "";
        public String content;
        public boolean bePrivate;

        public String getHint() {
            String targetStr = "";
            if (!TextUtils.isEmpty(targetNickname)) {
                targetStr = "@" + targetNickname + " ";
            }

            return targetStr;
        }

        public boolean validate() {
            return !TextUtils.isEmpty(content);
        }

        public void reset() {
            targetUserId = "";
            targetNickname = "";
            content = "";
            bePrivate = false;
        }
    }

    public static CommentInputBottomSheetDialogFragment newInstance(CommentViewModel commentViewModel) {
        CommentInputBottomSheetDialogFragment fragment = new CommentInputBottomSheetDialogFragment();
        Bundle bundle = new Bundle();
        String tmpStr = new Gson().toJson(commentViewModel);
        bundle.putString("commentViewModel", tmpStr);
        Log.v("pplog555", "newInstance:" + tmpStr);
        fragment.setArguments(bundle);

        return fragment;
    }

    public void setBottomSheetDialogFragmentListener(BottomSheetDialogFragmentListener bottomSheetDialogFragmentListener) {
        this.bottomSheetDialogFragmentListener = bottomSheetDialogFragmentListener;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String commentViewModelStr = getArguments().getString("commentViewModel");
        Log.v("pplog555", "onCreate:" + commentViewModelStr);
        commentViewModel = new Gson().fromJson(commentViewModelStr, CommentViewModel.class);
        Log.v("pplog555", "onCreate: commentViewModel:" + commentViewModel.content);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.v("pplog555", "onCreateView:" + commentViewModel.content);
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_comment_input_bottom_sheet_dialog, container, false);
        binding.setData(commentViewModel);

        //这里要用delay, 怀疑binding.setData(commentViewModel);是个异步操作, 需要等这个异步操作完成后定位光标才有意义
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                //Do something after 100ms
                binding.contentTextInputEditText.setSelection(binding.contentTextInputEditText.getText().length());
            }
        }, 100);

        View view = binding.getRoot();
        //set to adjust screen height automatically, when soft keyboard appears on screen
        getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        //content输入监控
        RxTextView.textChanges(binding.contentTextInputEditText)
                .skip(1)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<CharSequence>() {
                            @Override
                            public void accept(@NonNull CharSequence charSequence) throws Exception {
                                commentViewModel.content = charSequence.toString();
                                binding.setData(commentViewModel);
                                bottomSheetDialogFragmentListener.setCommentViewModel(commentViewModel);
                            }
                        }
                );

        //bePrivate监控
        RxCompoundButton.checkedChanges(binding.privateCheckBox)
                .skip(1)
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(Schedulers.io())
                .subscribe(
                        new Consumer<Boolean>() {
                            @Override
                            public void accept(@NonNull Boolean aBoolean) throws Exception {
                                commentViewModel.bePrivate = aBoolean;
                                binding.setData(commentViewModel);
                                bottomSheetDialogFragmentListener.setCommentViewModel(commentViewModel);
                            }
                        }
                );

        //send comment按钮监控
        Observable<Object> commentButtonObservable = RxView.clicks(binding.sendButton)
                .debounce(200, TimeUnit.MILLISECONDS);

        commentButtonObservable
                .subscribeOn(AndroidSchedulers.mainThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                        new Consumer<Object>() {
                            public void accept(Object o) {
                                bottomSheetDialogFragmentListener.sendComment();
                                dismiss();
                            }
                        }
                );

        binding.contentTextInputEditText.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                Log.v("pplog333", "onEditorAction");
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    bottomSheetDialogFragmentListener.sendComment();
                    dismiss();
                    return true;
                }
                return false;
            }
        });

        return view;
    }
}
