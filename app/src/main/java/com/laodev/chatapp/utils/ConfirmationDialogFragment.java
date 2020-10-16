package com.laodev.chatapp.utils;

import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import com.laodev.chatapp.R;

/**
 * Created by a_man on 04-12-2017.
 */

public class ConfirmationDialogFragment extends DialogFragment {
    private String title, message;
    private View.OnClickListener yesClickListener, noClickListener;

    public ConfirmationDialogFragment() {
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.fragment_confirmation, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setCanceledOnTouchOutside(false);

        ((TextView) view.findViewById(R.id.title)).setText(title);
        ((TextView) view.findViewById(R.id.message)).setText(message);
        view.findViewById(R.id.yes).setOnClickListener(view12 -> {
            dismiss();
            yesClickListener.onClick(view12);
        });
        view.findViewById(R.id.no).setOnClickListener(view1 -> {
            dismiss();
            noClickListener.onClick(view1);
        });

        return dialog;
    }

    public static ConfirmationDialogFragment newInstance(String title, String message, View.OnClickListener yesClickListener, View.OnClickListener noClickListener) {
        ConfirmationDialogFragment dialogFragment = new ConfirmationDialogFragment();
        dialogFragment.title = title;
        dialogFragment.message = message;
        dialogFragment.yesClickListener = yesClickListener;
        dialogFragment.noClickListener = noClickListener;
        return dialogFragment;
    }
}
