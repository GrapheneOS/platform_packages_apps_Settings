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

import static com.google.common.truth.Truth.assertThat;

import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import com.android.settings.R;
import com.android.settings.testutils.shadow.ShadowAlertDialogCompat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(shadows = ShadowAlertDialogCompat.class)
public class EnterSimPinDialogFragmentTest {

    private FragmentActivity mActivity;

    @Before
    public void setUp() {
        mActivity = Robolectric.setupActivity(FragmentActivity.class);
    }

    private AlertDialog show(EnterSimPinDialogFragment fragment) {
        fragment.show(mActivity.getSupportFragmentManager(), "EnterSimPinDialogFragment");
        ShadowLooper.idleMainLooper();
        return ShadowAlertDialogCompat.getLatestAlertDialog();
    }

    private void assertTitleAndHint(EnterSimPinDialogFragment fragment, int titleRes,
            int hintRes) {
        AlertDialog dialog = show(fragment);
        assertThat(ShadowAlertDialogCompat.shadowOf(dialog).getTitle().toString())
                .isEqualTo(mActivity.getString(titleRes));
        EditText pinInput = dialog.findViewById(R.id.current_sim_pin);
        assertThat(pinInput.getHint().toString()).isEqualTo(mActivity.getString(hintRes));
    }

    @Test
    public void enterCurrentPin_hintAsksForCurrentPin() {
        assertTitleAndHint(EnterSimPinDialogFragment.newEnterCurrentPin(),
                R.string.provide_current_sim_pin_title, R.string.enter_current_sim_pin_hint);
        assertTitleAndHint(EnterSimPinDialogFragment.newEnterCurrentPinWithHint(),
                R.string.provide_current_sim_pin_title, R.string.enter_current_sim_pin_hint);
        assertTitleAndHint(EnterSimPinDialogFragment.newEnterCurrentPin(2),
                R.string.provide_current_sim_pin_title, R.string.enter_current_sim_pin_hint);
    }

    @Test
    public void enterNewPin_hintAsksForNewPin() {
        assertTitleAndHint(EnterSimPinDialogFragment.newEnterNewPin(),
                R.string.provide_new_sim_pin_title, R.string.provide_new_sim_pin_title);
        assertTitleAndHint(EnterSimPinDialogFragment.newEnterNewPinWithHint(),
                R.string.provide_new_sim_pin_title, R.string.provide_new_sim_pin_title);
    }

    @Test
    public void confirmNewPin_hintAsksToConfirmNewPin() {
        assertTitleAndHint(EnterSimPinDialogFragment.newConfirmNewPin(),
                R.string.confirm_new_sim_pin_title, R.string.confirm_new_sim_pin_title);
        assertTitleAndHint(EnterSimPinDialogFragment.newConfirmNewPinInstanceWithHint(),
                R.string.confirm_new_sim_pin_title, R.string.confirm_new_sim_pin_title);
        assertTitleAndHint(EnterSimPinDialogFragment.newConfirmNewPinAfterMismatch(),
                R.string.confirm_new_sim_pin_title, R.string.confirm_new_sim_pin_title);
    }
}
