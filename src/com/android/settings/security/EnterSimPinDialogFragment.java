/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.security;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import com.android.settings.R;


/**
 * Dialog for entering the current or new SIM card PIN. Used when the user wants to change
 * the SIM card's PIN.
 */
public class EnterSimPinDialogFragment extends DialogFragment {
    private static final String KEY_TITLE = "dialog_title";
    private static final String KEY_MESSAGE = "dialog_message";
    private static final String KEY_HINT = "dialog_hint";
    private static final String KEY_REMAINING_ATTEMPTS = "remaining_attempts";

    private static final String TAG = "SimPinDialog";

    public EnterSimPinDialogFragment() {
        super();
    }

    public interface SimPinEntryListener {
        /**
         * Called when the user has entered a PIN and confirmed it.
         * @param pin provided by the user.
         */
        void onPinEntered(String pin);

        /**
         * Called when the user has cancelled PIN entry.
         */
        void onEntryCancelled();
    }

    private static EnterSimPinDialogFragment createFragmentWithArgs(int dialogTitleResource,
            int dialogMessageResource, int hintResource) {
        return createFragmentWithArgs(dialogTitleResource,
                dialogMessageResource, hintResource, /* attemptsRemaining= */0);
    }

    private static EnterSimPinDialogFragment createFragmentWithArgs(int dialogTitleResource,
            int dialogMessageResource, int hintResource, int attemptsRemaining) {
        Bundle args = new Bundle();
        args.putInt(KEY_TITLE, dialogTitleResource);
        args.putInt(KEY_MESSAGE, dialogMessageResource);
        args.putInt(KEY_HINT, hintResource);
        args.putInt(KEY_REMAINING_ATTEMPTS, attemptsRemaining);

        EnterSimPinDialogFragment fragment = new EnterSimPinDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    /**
     * Returns a dialog requesting the user to enter the current SIM PIN when the user starts
     * the journey to protect the SIM with a PIN.
     * @return a dialog instance.
     */
    static EnterSimPinDialogFragment newEnterCurrentPin() {
        return createFragmentWithArgs(R.string.provide_current_sim_pin_title,
                R.string.provide_current_sim_pin, R.string.enter_current_sim_pin_hint);
    }

    /**
     * Same as {@code newEnterCurrentPin} but with a hint about the correct format of the
     * PIN.
     *
     * @return a dialog instance.
     */
    static EnterSimPinDialogFragment newEnterCurrentPinWithHint() {
        return createFragmentWithArgs(R.string.provide_current_sim_pin_title,
                R.string.sim_invalid_pin_hint, R.string.enter_current_sim_pin_hint);
    }

    /**
     * Returns a dialog requesting the user to enter the current SIM PIN when the user has already
     * tried entering a PIN but the PIN provided is wrong.
     * @return a dialog instance.
     */
    static EnterSimPinDialogFragment newEnterCurrentPin(int numAttemptsRemaining) {
        return createFragmentWithArgs(R.string.provide_current_sim_pin_title,
                R.string.enter_current_sim_pin_after_mismatch,
                R.string.enter_current_sim_pin_hint, numAttemptsRemaining);
    }

    /**
     * Returns a dialog asking the user to enter the new SIM PIN.
     * @return a dialog instance.
     */
    static EnterSimPinDialogFragment newEnterNewPin() {
        return createFragmentWithArgs(R.string.provide_new_sim_pin_title,
                R.string.sim_enter_new, R.string.provide_new_sim_pin_title);
    }

    /**
     * Same as {@code newEnterNewPin} but with a hint about the correct format of the
     * PIN.
     * @return a dialog instance.
     */
    static EnterSimPinDialogFragment newEnterNewPinWithHint() {
        return createFragmentWithArgs(R.string.provide_new_sim_pin_title,
                R.string.sim_invalid_pin_hint, R.string.provide_new_sim_pin_title);
    }

    /**
     * Returns a dialog asking the user to confirm the new SIM PIN.
     * @return a dialog instance.
     */
    static EnterSimPinDialogFragment newConfirmNewPin() {
        return createFragmentWithArgs(R.string.confirm_new_sim_pin_title,
                R.string.sim_reenter_new, R.string.confirm_new_sim_pin_title);
    }

    /**
     * Same as {@code newConfirmNewPin} but informing the user there is a mismatch between
     * the two PINs they have provided  as the new PIN.
     * @return a dialog instance.
     */
    static EnterSimPinDialogFragment newConfirmNewPinAfterMismatch() {
        return createFragmentWithArgs(R.string.confirm_new_sim_pin_title,
                R.string.confirm_new_sim_pin_after_mismatch, R.string.confirm_new_sim_pin_title);
    }

    /**
     * Same as {@code newConfirmNewPin} but with a hint about the correct format of the
     * PIN.
     * @return a dialog instance.
     */
    static EnterSimPinDialogFragment newConfirmNewPinInstanceWithHint() {
        return createFragmentWithArgs(R.string.confirm_new_sim_pin_title,
                R.string.sim_invalid_pin_hint, R.string.confirm_new_sim_pin_title);
    }

    @Override
    public @NonNull Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        //readValuesFromBundle(savedInstanceState);
        Bundle args = requireArguments();
        int dialogTitleResource = args.getInt(KEY_TITLE);
        int mDialogMessageResource = args.getInt(KEY_MESSAGE);
        int mAttemptsRemaining = args.getInt(KEY_REMAINING_ATTEMPTS);
        int hintResource = args.getInt(KEY_HINT);


        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(dialogTitleResource)
                .setCancelable(true)
                .setPositiveButton(R.string.sim_enter_ok,
                        (dialog, which) -> invokePinEnteredCallback())
                .setNegativeButton(com.android.internal.R.string.cancel,
                        (dialog, which) -> invokeCancelCallback());

        if (mAttemptsRemaining == 0) {
            builder.setMessage(mDialogMessageResource);
        } else {
            builder.setMessage(getContext().getResources().getString(mDialogMessageResource,
                    mAttemptsRemaining));
        }

        View view = LayoutInflater.from(builder.getContext()).inflate(
                R.layout.dialog_provide_sim_pin_entry, null);
        EditText pinInput = view.findViewById(R.id.current_sim_pin);
        pinInput.setHint(hintResource);
        builder.setView(view);
        return builder.create();
    }

    @Nullable
    private SimPinEntryListener getPinEntryListener() {
        Fragment parent = getParentFragment();
        if (parent == null || !(parent instanceof SimPinEntryListener)) {
            Log.w(TAG, "No or wrong parent fragment, entered PIN will have no impact.");
            return null;
        }

        return (SimPinEntryListener) parent;
    }

    private void invokePinEnteredCallback() {
        EditText pinInput = getDialog().findViewById(R.id.current_sim_pin);

        String pin = ((TextView) pinInput).getText().toString();
        dismiss();
        SimPinEntryListener listener = getPinEntryListener();
        if (listener != null) {
            listener.onPinEntered(pin);
        }
    }

    private void invokeCancelCallback() {
        dismiss();
        SimPinEntryListener listener = getPinEntryListener();
        if (listener != null) {
            listener.onEntryCancelled();
        }
    }
}
