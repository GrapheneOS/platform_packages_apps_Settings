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

package com.android.settings.network;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.ext.settings.ExtSettings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class AirplaneModeAuthenticationPreferenceControllerTest {
    @Mock
    private AirplaneModeAuthenticationHelper mAuthenticationHelper;

    private Context mContext;
    private AirplaneModeAuthenticationPreferenceController mController;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = ApplicationProvider.getApplicationContext();
        ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.put(mContext, false);
        mController = new AirplaneModeAuthenticationPreferenceController(
                mContext, "require_authentication_to_disable_airplane_mode");
        mController.setAuthenticationHelper(mAuthenticationHelper);
    }

    @After
    public void tearDown() {
        ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.put(mContext, false);
    }

    @Test
    public void setChecked_enabling_doesNotAuthenticate() {
        assertThat(mController.setChecked(true)).isTrue();

        assertThat(ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.get(mContext))
                .isTrue();
        verify(mAuthenticationHelper, never()).runAfterAuthentication(any());
    }

    @Test
    public void setChecked_disabling_waitsForAuthentication() {
        ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.put(mContext, true);

        assertThat(mController.setChecked(false)).isFalse();

        assertThat(ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.get(mContext))
                .isTrue();
        verify(mAuthenticationHelper).runAfterAuthentication(any());
    }

    @Test
    public void setChecked_disablingAfterAuthentication_rechecksAndDisables() {
        ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.put(mContext, true);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return null;
        }).when(mAuthenticationHelper).runAfterAuthentication(any());

        assertThat(mController.setChecked(false)).isFalse();

        assertThat(ExtSettings.REQUIRE_AUTHENTICATION_TO_DISABLE_AIRPLANE_MODE.get(mContext))
                .isFalse();
    }

    @Test
    public void onDestroy_cancelsAuthentication() {
        mController.onDestroy();

        verify(mAuthenticationHelper).cancel();
    }
}
