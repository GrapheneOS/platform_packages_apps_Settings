/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.settings.users;

import static com.android.settings.flags.Flags.showAddUsersFromSigninToggle;
import static com.android.settings.flags.Flags.showAddUsersFromSigninToggle2;
import static com.android.settings.flags.Flags.showUserDetailsSettingsForSelf;
import static com.android.settings.flags.Flags.showUserDetailsSettingsForSelf2;
import static com.android.settingslib.Utils.getColorAttrDefaultColor;

import android.Manifest;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.app.admin.DevicePolicyIdentifiers;
import android.app.admin.DevicePolicyManager;
import android.app.admin.EnforcingAdmin;
import android.app.admin.PolicyEnforcementInfo;
import android.app.admin.SystemAuthority;
import android.app.settings.SettingsEnums;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.UserInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlendMode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.multiuser.Flags;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.Trace;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManagerGlobal;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.Preference;
import androidx.preference.PreferenceGroup;
import androidx.preference.PreferenceScreen;

import com.android.internal.util.UserIcons;
import com.android.internal.widget.LockPatternUtils;
import com.android.settings.R;
import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.Utils;
import com.android.settings.core.SubSettingLauncher;
import com.android.settings.password.ChooseLockGeneric;
import com.android.settings.password.ChooseLockSettingsHelper;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.RestrictedLockUtilsInternal;
import com.android.settingslib.RestrictedPreference;
import com.android.settingslib.drawable.CircleFramedDrawable;
import com.android.settingslib.search.SearchIndexable;
import com.android.settingslib.search.SearchIndexableRaw;
import com.android.settingslib.users.CreateUserActivity;
import com.android.settingslib.users.CreateUserDialogController;
import com.android.settingslib.users.EditUserInfoController;
import com.android.settingslib.users.UserCreatingDialog;
import com.android.settingslib.utils.ThreadUtils;

import com.google.android.setupcompat.util.WizardManagerHelper;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * Screen that manages the list of users on the device.
 * Secondary users and a guest user can be created if there is no restriction.
 *
 * The first user in the list is always the current user.
 * Owner is the primary user.
 */
// LINT.IfChange
@SearchIndexable
public class UserSettings extends SettingsPreferenceFragment
        implements Preference.OnPreferenceClickListener,
        MultiUserSwitchBarController.OnMultiUserSwitchChangedListener,
        DialogInterface.OnDismissListener {

    private static final String TAG = "UserSettings";

    /** UserId of the user being removed */
    private static final String SAVE_REMOVING_USER = "removing_user";
    private static final String SAVE_CREATE_USER = "create_user";

    private static final String KEY_USER_LIST = "user_list";
    private static final String KEY_USER_ME = "user_me";
    private static final String KEY_USER_GUEST = "user_guest";
    private static final String KEY_ADD_GUEST = "guest_add";
    private static final String KEY_ADD_USER = "user_add";
    private static final String KEY_ADD_SUPERVISED_USER = "supervised_user_add";
    private static final String KEY_ADD_USER_WHEN_LOCKED = "user_settings_add_users_when_locked";
    private static final String KEY_ADD_USER_FROM_SIGNIN = "add_users_from_signin";
    private static final String KEY_ADD_GUEST_FROM_SIGNIN = "add_guests_from_signin";
    private static final String KEY_ADD_USER_SETTINGS_CATEGORY = "add_user_settings_category";
    private static final String KEY_ENABLE_GUEST_TELEPHONY = "enable_guest_calling";
    private static final String KEY_MULTIUSER_TOP_INTRO = "multiuser_top_intro";
    private static final String KEY_TIMEOUT_TO_DOCK_USER = "timeout_to_dock_user_preference";
    private static final String KEY_GUEST_CATEGORY = "guest_category";
    private static final String KEY_GUEST_RESET = "guest_reset";
    private static final String KEY_GUEST_EXIT = "guest_exit";
    private static final String KEY_REMOVE_GUEST_ON_EXIT = "remove_guest_on_exit";
    private static final String KEY_GUEST_USER_CATEGORY = "guest_user_category";
    private static final String KEY_USER_SETTINGS_SCREEN = "user_settings_screen";
    private static final String KEY_SEND_CENSORED_NOTIFICATIONS =
            "user_settings_send_censored_notifications_to_current";
    private static final String KEY_USER_SWITCH_TOGGLE = "multiple_users_main_switch";

    private static final String SETTING_GUEST_HAS_LOGGED_IN = "systemui.guest_has_logged_in";

    private static final int MENU_REMOVE_USER = Menu.FIRST;
    private static final int MENU_REORDER_USERS = Menu.FIRST + 1;

    private static final IntentFilter USER_REMOVED_INTENT_FILTER;

    @VisibleForTesting
    static final int DIALOG_CONFIRM_REMOVE = 1;
    private static final int DIALOG_ADD_USER = 2;
    // Dialogs with id 3 and 4 got removed
    private static final int DIALOG_USER_CANNOT_MANAGE = 5;
    private static final int DIALOG_CHOOSE_USER_TYPE = 6;
    private static final int DIALOG_NEED_LOCKSCREEN = 7;
    private static final int DIALOG_CONFIRM_REMOVE_GUEST = 8;
    private static final int DIALOG_USER_PROFILE_EDITOR = 9;
    private static final int DIALOG_USER_PROFILE_EDITOR_ADD_USER = 10;
    private static final int DIALOG_USER_PROFILE_EDITOR_ADD_RESTRICTED_PROFILE = 11;
    private static final int DIALOG_CONFIRM_REMOVE_GUEST_WITH_AUTO_CREATE = 12;
    private static final int DIALOG_CONFIRM_RESET_AND_RESTART_GUEST = 13;
    private static final int DIALOG_CONFIRM_EXIT_GUEST_EPHEMERAL = 14;
    private static final int DIALOG_CONFIRM_EXIT_GUEST_NON_EPHEMERAL = 15;
    private static final int DIALOG_GRANT_ADMIN = 16;

    private static final int MESSAGE_UPDATE_LIST = 1;
    private static final int MESSAGE_USER_CREATED = 2;
    static final int MESSAGE_REMOVE_GUEST_ON_EXIT_CONTROLLER_GUEST_REMOVED = 3;

    private static final int USER_TYPE_USER = 1;
    private static final int USER_TYPE_RESTRICTED_PROFILE = 2;

    private static final int REQUEST_CHOOSE_LOCK = 10;
    private static final int REQUEST_EDIT_GUEST = 11;
    private static final int REQUEST_ADD_USER = 12;
    @VisibleForTesting
    static final int REQUEST_DELETE_USER = 13;

    static final int RESULT_GUEST_REMOVED = 100;

    private static final String KEY_TITLE = "title";
    private static final String KEY_SUMMARY = "summary";

    private static final String EXTRA_OPEN_DIALOG_USER_PROFILE_EDITOR =
            "EXTRA_OPEN_DIALOG_USER_PROFILE_EDITOR";

    static {
        USER_REMOVED_INTENT_FILTER = new IntentFilter(Intent.ACTION_USER_REMOVED);
        USER_REMOVED_INTENT_FILTER.addAction(Intent.ACTION_USER_INFO_CHANGED);
    }

    @VisibleForTesting
    PreferenceGroup mUserListCategory;
    @VisibleForTesting
    PreferenceGroup mGuestUserCategory;
    @VisibleForTesting
    PreferenceGroup mGuestCategory;
    @SuppressWarnings("NullAway")
    @VisibleForTesting
    PreferenceGroup mAddUserSettingsCategory;
    @VisibleForTesting
    Preference mGuestResetPreference;
    @VisibleForTesting
    Preference mGuestExitPreference;
    @VisibleForTesting
    UserPreference mMePreference;
    @VisibleForTesting
    RestrictedPreference mAddGuest;
    @VisibleForTesting
    RestrictedPreference mAddUser;
    @VisibleForTesting
    RestrictedPreference mAddSupervisedUser;
    @VisibleForTesting
    SparseArray<Bitmap> mUserIcons = new SparseArray<>();
    private int mRemovingUserId = -1;
    private boolean mAddingUser;
    private boolean mUserRemovalCredentialConfirmationPending;
    private boolean mGuestUserAutoCreated;
    private String mConfigSupervisedUserCreationPackage;
    private String mAddingUserName;
    private UserCapabilities mUserCaps;
    private boolean mShouldUpdateUserList = true;
    private final Object mUserLock = new Object();
    private UserManager mUserManager;
    private static SparseArray<Bitmap> sDarkDefaultUserBitmapCache = new SparseArray<>();

    private MultiUserSwitchBarController mSwitchBarController;

    @NonNull
    private MultiUserMainSwitchPreferenceController mMainSwitchController;

    private EditUserInfoController mEditUserInfoController =
            new EditUserInfoController(Utils.FILE_PROVIDER_AUTHORITY);
    private CreateUserDialogController mCreateUserDialogController =
            new CreateUserDialogController(Utils.FILE_PROVIDER_AUTHORITY);
    private AddUserWhenLockedPreferenceController mAddUserWhenLockedPreferenceController;
    @SuppressWarnings("NullAway")
    private AddUserFromSignInPreferenceController mAddUserFromSignInPreferenceController;
    @SuppressWarnings("NullAway")
    private AddGuestFromSignInPreferenceController mAddGuestFromSignInPreferenceController;
    private GuestTelephonyPreferenceController mGuestTelephonyPreferenceController;
    private RemoveGuestOnExitPreferenceController mRemoveGuestOnExitPreferenceController;
    private SendCensoredNotificationsToCurrentUserPreferenceController mSendCensoredNotificationsToCurrentUserPreferenceController;
    private MultiUserTopIntroPreferenceController mMultiUserTopIntroPreferenceController;
    private TimeoutToDockUserPreferenceController mTimeoutToDockUserPreferenceController;
    private UserCreatingDialog mUserCreatingDialog;
    private final AtomicBoolean mGuestCreationScheduled = new AtomicBoolean();
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private CharSequence mPendingUserName;
    @Nullable
    private String mPendingUserIconPath;
    private Drawable mPendingUserIcon;
    private boolean mPendingUserIsAdmin;

    // A place to cache the generated default avatar
    private Drawable mDefaultIconDrawable;

    // TODO:   Replace current Handler solution to something that doesn't leak memory and works
    // TODO:   during a configuration change
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_UPDATE_LIST:
                    updateUserList();
                    break;
                case MESSAGE_REMOVE_GUEST_ON_EXIT_CONTROLLER_GUEST_REMOVED:
                    updateUserList();
                    if (mGuestUserAutoCreated) {
                        scheduleGuestCreation();
                    }
                    break;
            }
        }
    };

    private BroadcastReceiver mUserChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_USER_REMOVED)) {
                mRemovingUserId = -1;
            } else if (intent.getAction().equals(Intent.ACTION_USER_INFO_CHANGED)) {
                int userHandle = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
                if (userHandle != -1) {
                    mUserIcons.remove(userHandle);
                }
            }
            mHandler.sendEmptyMessage(MESSAGE_UPDATE_LIST);
        }
    };

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.USER;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Assume we are in a SettingsActivity. This is only safe because we currently use
        // SettingsActivity as base for all preference fragments.
        boolean openUserEditDialog = getIntent().getBooleanExtra(
                EXTRA_OPEN_DIALOG_USER_PROFILE_EDITOR, false);

        if (openUserEditDialog) {
            showDialog(DIALOG_USER_PROFILE_EDITOR);
        }
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        addPreferencesFromResource(R.xml.user_settings);
        final Activity activity = getActivity();
        if (!WizardManagerHelper.isDeviceProvisioned(activity)) {
            activity.finish();
            return;
        }

        mGuestUserAutoCreated = getPrefContext().getResources().getBoolean(
                com.android.internal.R.bool.config_guestUserAutoCreated);

        mAddUserWhenLockedPreferenceController = new AddUserWhenLockedPreferenceController(
                activity, KEY_ADD_USER_WHEN_LOCKED);

        mAddUserFromSignInPreferenceController = new AddUserFromSignInPreferenceController(
                activity, KEY_ADD_USER_FROM_SIGNIN);

        mAddGuestFromSignInPreferenceController = new AddGuestFromSignInPreferenceController(
                activity, KEY_ADD_GUEST_FROM_SIGNIN);

        mGuestTelephonyPreferenceController = new GuestTelephonyPreferenceController(
                activity, KEY_ENABLE_GUEST_TELEPHONY);

        mRemoveGuestOnExitPreferenceController = new RemoveGuestOnExitPreferenceController(
                activity, KEY_REMOVE_GUEST_ON_EXIT, this, mHandler);

        mSendCensoredNotificationsToCurrentUserPreferenceController =
                new SendCensoredNotificationsToCurrentUserPreferenceController(activity,
                        KEY_SEND_CENSORED_NOTIFICATIONS);

        mMultiUserTopIntroPreferenceController = new MultiUserTopIntroPreferenceController(activity,
                KEY_MULTIUSER_TOP_INTRO);

        mTimeoutToDockUserPreferenceController = new TimeoutToDockUserPreferenceController(
                activity, KEY_TIMEOUT_TO_DOCK_USER);

        mMainSwitchController = new MultiUserMainSwitchPreferenceController(
                activity, KEY_USER_SWITCH_TOGGLE);

        final PreferenceScreen screen = getPreferenceScreen();
        mAddUserWhenLockedPreferenceController.displayPreference(screen);
        mAddUserFromSignInPreferenceController.displayPreference(screen);
        mAddGuestFromSignInPreferenceController.displayPreference(screen);
        mGuestTelephonyPreferenceController.displayPreference(screen);
        mRemoveGuestOnExitPreferenceController.displayPreference(screen);
        mSendCensoredNotificationsToCurrentUserPreferenceController.displayPreference(screen);
        mMultiUserTopIntroPreferenceController.displayPreference(screen);
        mTimeoutToDockUserPreferenceController.displayPreference(screen);
        mMainSwitchController.displayPreference(screen);


        screen.findPreference(mAddUserWhenLockedPreferenceController.getPreferenceKey())
                .setOnPreferenceChangeListener(mAddUserWhenLockedPreferenceController);
        screen.findPreference(mSendCensoredNotificationsToCurrentUserPreferenceController.getPreferenceKey())
                .setOnPreferenceChangeListener(mSendCensoredNotificationsToCurrentUserPreferenceController);

        screen.findPreference(mAddUserFromSignInPreferenceController.getPreferenceKey())
                .setOnPreferenceChangeListener(mAddUserFromSignInPreferenceController);

        screen.findPreference(mAddGuestFromSignInPreferenceController.getPreferenceKey())
                .setOnPreferenceChangeListener(mAddGuestFromSignInPreferenceController);

        screen.findPreference(mGuestTelephonyPreferenceController.getPreferenceKey())
                .setOnPreferenceChangeListener(mGuestTelephonyPreferenceController);

        screen.findPreference(mRemoveGuestOnExitPreferenceController.getPreferenceKey())
                .setOnPreferenceChangeListener(mRemoveGuestOnExitPreferenceController);

        if (icicle != null) {
            if (icicle.containsKey(SAVE_REMOVING_USER)) {
                mRemovingUserId = icicle.getInt(SAVE_REMOVING_USER);
            }
            if (icicle.containsKey(SAVE_CREATE_USER)) {
                mCreateUserDialogController.onRestoreInstanceState(icicle);
            } else {
                mEditUserInfoController.onRestoreInstanceState(icicle);
            }
        }

        mUserCaps = UserCapabilities.create(activity);
        mUserManager = (UserManager) activity.getSystemService(Context.USER_SERVICE);
        if (!mUserCaps.mEnabled) {
            return;
        }

        final int myUserId = UserHandle.myUserId();

        mUserListCategory = (PreferenceGroup) findPreference(KEY_USER_LIST);
        mMePreference = new UserPreference(getPrefContext(), null /* attrs */, myUserId);
        mMePreference.setKey(KEY_USER_ME);
        mMePreference.setOnPreferenceClickListener(this);
        if (flagShowUserDetailsSettingsForSelf()) {
            mMePreference.setOnEditClickListener((view) -> showDialog(DIALOG_USER_PROFILE_EDITOR));
        }

        mGuestCategory = findPreference(KEY_GUEST_CATEGORY);

        mGuestResetPreference = findPreference(KEY_GUEST_RESET);
        mGuestResetPreference.setOnPreferenceClickListener(this);

        mGuestExitPreference = findPreference(KEY_GUEST_EXIT);
        mGuestExitPreference.setOnPreferenceClickListener(this);

        mGuestUserCategory = findPreference(KEY_GUEST_USER_CATEGORY);

        mAddGuest = findPreference(KEY_ADD_GUEST);
        mAddGuest.setOnPreferenceClickListener(this);

        mAddUser = findPreference(KEY_ADD_USER);
        if (!mUserCaps.mCanAddRestrictedProfile) {
            // Label should only mention adding a "user", not a "profile"
            mAddUser.setTitle(com.android.settingslib.R.string.user_add_user);
        }
        mAddUser.setOnPreferenceClickListener(this);

        setConfigSupervisedUserCreationPackage();
        mAddSupervisedUser = findPreference(KEY_ADD_SUPERVISED_USER);
        mAddSupervisedUser.setOnPreferenceClickListener(this);

        mAddUserSettingsCategory = findPreference(KEY_ADD_USER_SETTINGS_CATEGORY);

        activity.registerReceiverAsUser(
                mUserChangeReceiver, UserHandle.ALL, USER_REMOVED_INTENT_FILTER, null, mHandler,
                Context.RECEIVER_EXPORTED_UNAUDITED);

        updateUI();
        mShouldUpdateUserList = false;
    }

    @Override
    public void onResume() {
        super.onResume();

        if (!mUserCaps.mEnabled) {
            return;
        }
        final PreferenceScreen screen = getPreferenceScreen();

        mAddUserWhenLockedPreferenceController.updateState(screen.findPreference(
                mAddUserWhenLockedPreferenceController.getPreferenceKey()));
        mAddUserFromSignInPreferenceController.updateState(screen.findPreference(
                mAddUserFromSignInPreferenceController.getPreferenceKey()));
        mAddGuestFromSignInPreferenceController.updateState(screen.findPreference(
                mAddGuestFromSignInPreferenceController.getPreferenceKey()));
        mGuestTelephonyPreferenceController.updateState(screen.findPreference(
                mGuestTelephonyPreferenceController.getPreferenceKey()));
        mTimeoutToDockUserPreferenceController.updateState(screen.findPreference(
                mTimeoutToDockUserPreferenceController.getPreferenceKey()));
        mRemoveGuestOnExitPreferenceController.updateState(screen.findPreference(
                mRemoveGuestOnExitPreferenceController.getPreferenceKey()));
        mMainSwitchController.updateState();
        mSendCensoredNotificationsToCurrentUserPreferenceController.updateState(screen.findPreference(
                mSendCensoredNotificationsToCurrentUserPreferenceController.getPreferenceKey()));

        if (mShouldUpdateUserList) {
            updateUI();
        }
    }

    @Override
    public void onPause() {
        mShouldUpdateUserList = true;
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mUserCaps == null || !mUserCaps.mEnabled) {
            return;
        }

        getActivity().unregisterReceiver(mUserChangeReceiver);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mCreateUserDialogController.isActive()) {
            outState.putBoolean(SAVE_CREATE_USER, mCreateUserDialogController.isActive());
            mCreateUserDialogController.onSaveInstanceState(outState);
        } else {
            mEditUserInfoController.onSaveInstanceState(outState);
        }
        outState.putInt(SAVE_REMOVING_USER, mRemovingUserId);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void startActivityForResult(Intent intent, int requestCode) {
        mEditUserInfoController.startingActivityForResult();
        mCreateUserDialogController.startingActivityForResult();
        super.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        int pos = 0;
        if (!mUserCaps.mIsMain
                && !isCurrentUserGuest()
                && !mUserManager.isProfile()
                && !flagShowUserDetailsSettingsForSelf()) {
            String nickname = mUserManager.getUserName();
            MenuItem removeThisUser = menu.add(0, MENU_REMOVE_USER, pos++,
                    getResources().getString(R.string.user_remove_user_menu, nickname));
            removeThisUser.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
            applyRemoveUserAdminRestriction(removeThisUser);
        }
        if (UserReorderFragment.canReorderUsers(mUserCaps, mUserManager)) {
            menu.add(0, MENU_REORDER_USERS, pos++, R.string.user_reorder_title)
                    .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        }
        super.onCreateOptionsMenu(menu, inflater);
    }

    private void applyRemoveUserAdminRestriction(MenuItem removeThisUser) {
        if (android.app.admin.flags.Flags.policyTransparencyRefactorEnabled()) {
            DevicePolicyManager dpm = getContext().getSystemService(DevicePolicyManager.class);
            PolicyEnforcementInfo info = dpm.getEnforcingAdminsForPolicy(
                    DevicePolicyIdentifiers.getIdentifierForUserRestriction(
                            UserManager.DISALLOW_REMOVE_USER), UserHandle.myUserId());
            EnforcingAdmin enforcingAdmin =
                    info.isOnlyEnforcedBySystem() ? null : info.getMostImportantEnforcingAdmin();
            RestrictedLockUtilsInternal.setMenuItemAsDisabledByAdmin(
                    getContext(), removeThisUser,
                    enforcingAdmin, UserManager.DISALLOW_REMOVE_USER);
        } else {
            final EnforcedAdmin disallowRemoveUserAdmin =
                    RestrictedLockUtilsInternal.checkIfRestrictionEnforced(getContext(),
                            UserManager.DISALLOW_REMOVE_USER, UserHandle.myUserId());
            RestrictedLockUtilsInternal.setMenuItemAsDisabledByAdmin(getContext(), removeThisUser,
                    disallowRemoveUserAdmin);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        final int itemId = item.getItemId();
        if (itemId == MENU_REMOVE_USER) {
            onRemoveUserClicked(UserHandle.myUserId());
            return true;
        } else if (itemId == MENU_REORDER_USERS) {
            new SubSettingLauncher(getContext())
                    .setDestination(UserReorderFragment.class.getName())
                    .setTitleRes(R.string.user_reorder_title)
                    .setSourceMetricsCategory(getMetricsCategory())
                    .launch();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onMultiUserSwitchChanged(boolean newState) {
        updateUI();
    }

    private void updateUI() {
        mUserCaps.updateAddUserCapabilities(getActivity());
        loadProfile();
        updateUserList();
    }

    /**
     * Loads profile information for the current user.
     */
    private void loadProfile() {
        if (isCurrentUserGuest()) {
            // No need to load profile information
            mMePreference.setIcon(getEncircledDefaultIcon());
            mMePreference.setTitle(mGuestUserAutoCreated
                    ? com.android.settingslib.R.string.guest_reset_guest
                    : com.android.settingslib.R.string.guest_exit_guest);
            mMePreference.setSelectable(true);
            // removing a guest will result in switching back to the admin user
            mMePreference.setEnabled(canSwitchUserNow());
            return;
        }

        new AsyncTask<Void, Void, String>() {
            @Override
            protected void onPostExecute(String result) {
                finishLoadProfile(result);
            }

            @Override
            protected String doInBackground(Void... values) {
                UserInfo user = mUserManager.getUserInfo(UserHandle.myUserId());
                if (user.iconPath == null || user.iconPath.equals("")) {
                    // Assign profile photo.
                    copyMeProfilePhoto(getActivity(), user);
                }
                return user.name;
            }
        }.execute();
    }

    private void finishLoadProfile(String profileName) {
        if (getActivity() == null) {
            return;
        }
        mMePreference.setTitle(getString(R.string.user_you, profileName));
        int myUserId = UserHandle.myUserId();
        Bitmap b = mUserManager.getUserIcon(myUserId);
        if (b != null) {
            mMePreference.setIcon(encircleUserIcon(b));
            mUserIcons.put(myUserId, b);
        }
    }

    private boolean hasLockscreenSecurity() {
        LockPatternUtils lpu = new LockPatternUtils(getActivity());
        return lpu.isSecure(UserHandle.myUserId());
    }

    private void launchChooseLockscreen() {
        Intent chooseLockIntent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD)
                .setPackage(getContext().getPackageName());
        chooseLockIntent.putExtra(ChooseLockGeneric.ChooseLockGenericFragment.HIDE_INSECURE_OPTIONS,
                true);
        startActivityForResult(chooseLockIntent, REQUEST_CHOOSE_LOCK);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHOOSE_LOCK) {
            if (resultCode != Activity.RESULT_CANCELED && hasLockscreenSecurity()) {
                addUserNow(USER_TYPE_RESTRICTED_PROFILE);
            }
        } else if (requestCode == REQUEST_ADD_USER) {
            if (resultCode == Activity.RESULT_OK) {
                mPendingUserName = data.getStringExtra(CreateUserActivity.EXTRA_USER_NAME);
                mPendingUserIsAdmin = data.getBooleanExtra(CreateUserActivity.EXTRA_IS_ADMIN,
                        false);
                mPendingUserIconPath = data.getStringExtra(CreateUserActivity.EXTRA_USER_ICON_PATH);
                addUserNow(USER_TYPE_USER);
            }
        } else if (mGuestUserAutoCreated && requestCode == REQUEST_EDIT_GUEST
                && resultCode == RESULT_GUEST_REMOVED) {
            scheduleGuestCreation();
        } else if (requestCode == REQUEST_DELETE_USER) {
            if (resultCode == Activity.RESULT_OK) {
                removeUserNow();
            } else {
                mRemovingUserId = -1;
                mUserRemovalCredentialConfirmationPending = false;
            }
        } else {
            mCreateUserDialogController.onActivityResult(requestCode, resultCode, data);
            mEditUserInfoController.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void onAddUserClicked(int userType) {
        synchronized (mUserLock) {
            if (mRemovingUserId == -1 && !mAddingUser) {
                switch (userType) {
                    case USER_TYPE_USER:
                        showDialog(DIALOG_ADD_USER);
                        break;
                    case USER_TYPE_RESTRICTED_PROFILE:
                        if (hasLockscreenSecurity()) {
                            showDialog(DIALOG_USER_PROFILE_EDITOR_ADD_RESTRICTED_PROFILE);
                        } else {
                            showDialog(DIALOG_NEED_LOCKSCREEN);
                        }
                        break;
                }
            }
        }
    }

    private void onAddSupervisedUserClicked() {
        final Intent intent = new Intent()
                .setAction(UserManager.ACTION_CREATE_SUPERVISED_USER)
                .setPackage(mConfigSupervisedUserCreationPackage)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        startActivity(intent);
    }

    private void onAddGuestClicked() {
        Context context = getContext();
        final UserCreatingDialog guestCreatingDialog =
                new UserCreatingDialog(getActivity(), /* isGuest= */ true);
        guestCreatingDialog.show();

        ThreadUtils.postOnBackgroundThread(() -> {
            mMetricsFeatureProvider.action(getActivity(), SettingsEnums.ACTION_USER_GUEST_ADD);
            Trace.beginSection("UserSettings.addGuest");
            final UserInfo guest = mUserManager.createGuest(context);
            Trace.endSection();

            ThreadUtils.postOnMainThread(() -> {
                guestCreatingDialog.dismiss();
                if (guest == null) {
                    Toast.makeText(context,
                            com.android.settingslib.R.string.add_guest_failed,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                openUserDetails(guest, true, context);
            });
        });
    }

    private void onRemoveUserClicked(int userId) {
        synchronized (mUserLock) {
            if (mRemovingUserId == -1 && !mAddingUser) {
                mRemovingUserId = userId;
                showDialog(DIALOG_CONFIRM_REMOVE);
            }
        }
    }

    private void onUserCreated(UserInfo userInfo, Context context) {
        hideUserCreatingDialog();
        mAddingUser = false;
        if (flagShowAddUsersFromSigninToggle()
                && getPrefContext()
                        .getResources()
                        .getBoolean(
                                com.android.internal.R.bool
                                        .config_userSwitchingMustGoThroughLoginScreen)) {
            switchToUserId(userInfo.id);
        } else {
            openUserDetails(userInfo, true, context);
        }
    }

    private void hideUserCreatingDialog() {
        if (mUserCreatingDialog != null && mUserCreatingDialog.isShowing()) {
            mUserCreatingDialog.dismiss();
        }
    }

    private void onUserCreationFailed() {
        Toast.makeText(getContext(),
                com.android.settingslib.R.string.add_user_failed,
                Toast.LENGTH_SHORT).show();
        hideUserCreatingDialog();
    }

    private void openUserDetails(UserInfo userInfo, boolean newUser) {
        openUserDetails(userInfo, newUser, getContext());
    }

    private void openUserDetails(UserInfo userInfo, boolean newUser, Context context) {
        // to prevent a crash when config changes during user creation,
        // we simply ignore this redirection step
        if (context == null) {
            return;
        }

        Bundle extras = new Bundle();
        extras.putInt(UserDetailsSettings.EXTRA_USER_ID, userInfo.id);
        extras.putBoolean(AppRestrictionsFragment.EXTRA_NEW_USER, newUser);

        SubSettingLauncher launcher = new SubSettingLauncher(context)
                .setDestination(UserDetailsSettings.class.getName())
                .setArguments(extras)
                .setTitleText(userInfo.name)
                .setSourceMetricsCategory(getMetricsCategory());
        if (mGuestUserAutoCreated && userInfo.isGuest()) {
            launcher.setResultListener(this, REQUEST_EDIT_GUEST);
        }
        launcher.launch();
    }

    @Override
    public void onDialogShowing() {
        super.onDialogShowing();

        setOnDismissListener(this);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        Context context = getActivity();
        if (context == null) {
            return null;
        }
        switch (dialogId) {
            case DIALOG_CONFIRM_REMOVE: {
                Dialog dlg =
                        UserDialogs.createRemoveDialog(getActivity(), mRemovingUserId,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        if (runUserRemovalKeyguardConfirmation()) {
                                            mUserRemovalCredentialConfirmationPending = true;
                                            return;
                                        }
                                        removeUserNow();
                                    }
                                }
                        );
                return dlg;
            }
            case DIALOG_USER_CANNOT_MANAGE:
                return new AlertDialog.Builder(context)
                        .setMessage(R.string.user_cannot_manage_message)
                        .setPositiveButton(android.R.string.ok, null)
                        .create();
            case DIALOG_ADD_USER: {
                synchronized (mUserLock) {
                    mPendingUserName = getString(
                            com.android.settingslib.R.string.user_new_user_name);
                    mPendingUserIcon = null;
                }
                return buildAddUserDialog(USER_TYPE_USER);
            }
            case DIALOG_CHOOSE_USER_TYPE: {
                List<HashMap<String, String>> data = new ArrayList<HashMap<String, String>>();
                HashMap<String, String> addUserItem = new HashMap<String, String>();
                addUserItem.put(KEY_TITLE, getString(
                        com.android.settingslib.R.string.user_add_user_item_title));
                addUserItem.put(KEY_SUMMARY, getString(
                        com.android.settingslib.R.string.user_add_user_item_summary));
                HashMap<String, String> addProfileItem = new HashMap<String, String>();
                addProfileItem.put(KEY_TITLE, getString(
                        com.android.settingslib.R.string.user_add_profile_item_title));
                addProfileItem.put(KEY_SUMMARY, getString(
                        com.android.settingslib.R.string.user_add_profile_item_summary));
                data.add(addUserItem);
                data.add(addProfileItem);
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                SimpleAdapter adapter = new SimpleAdapter(builder.getContext(),
                        data, R.layout.two_line_list_item,
                        new String[]{KEY_TITLE, KEY_SUMMARY},
                        new int[]{R.id.title, R.id.summary});
                builder.setTitle(com.android.settingslib.R.string.user_add_user_type_title);
                builder.setAdapter(adapter,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                onAddUserClicked(which == 0
                                        ? USER_TYPE_USER
                                        : USER_TYPE_RESTRICTED_PROFILE);
                            }
                        });
                return builder.create();
            }
            case DIALOG_NEED_LOCKSCREEN: {
                Dialog dlg = new AlertDialog.Builder(context)
                        .setMessage(com.android.settingslib.R.string.user_need_lock_message)
                        .setPositiveButton(com.android.settingslib.R.string.user_set_lock_button,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        launchChooseLockscreen();
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                return dlg;
            }
            case DIALOG_CONFIRM_REMOVE_GUEST: {
                Dialog dlg = new AlertDialog.Builder(context)
                        .setTitle(com.android.settingslib.R.string.guest_remove_guest_dialog_title)
                        .setMessage(R.string.user_exit_guest_confirm_message)
                        .setPositiveButton(R.string.user_exit_guest_dialog_remove,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        clearAndExitGuest();
                                    }
                                })
                        .setNegativeButton(android.R.string.cancel, null)
                        .create();
                return dlg;
            }
            case DIALOG_CONFIRM_EXIT_GUEST_EPHEMERAL: {
                Dialog dlg = new AlertDialog.Builder(context)
                        .setTitle(com.android.settingslib.R.string.guest_exit_dialog_title)
                        .setMessage(com.android.settingslib.R.string.guest_exit_dialog_message)
                        .setPositiveButton(
                                com.android.settingslib.R.string.guest_exit_dialog_button,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        clearAndExitGuest();
                                    }
                                })
                        .setNeutralButton(android.R.string.cancel, null)
                        .create();
                return dlg;
            }
            case DIALOG_CONFIRM_EXIT_GUEST_NON_EPHEMERAL: {
                Dialog dlg = new AlertDialog.Builder(context)
                        .setTitle(
                                com.android.settingslib.R.string.guest_exit_dialog_title_non_ephemeral)
                        .setMessage(
                                com.android.settingslib
                                        .R.string.guest_exit_dialog_message_non_ephemeral)
                        .setPositiveButton(
                                com.android.settingslib.R.string.guest_exit_save_data_button,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        exitGuest();
                                    }
                                })
                        .setNegativeButton(
                                com.android.settingslib.R.string.guest_exit_clear_data_button,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        clearAndExitGuest();
                                    }
                                })
                        .setNeutralButton(android.R.string.cancel, null)
                        .create();
                return dlg;
            }
            case DIALOG_USER_PROFILE_EDITOR: {
                return buildEditCurrentUserDialog();
            }
            case DIALOG_USER_PROFILE_EDITOR_ADD_USER: {
                synchronized (mUserLock) {
                    mPendingUserName = getString(
                            com.android.settingslib.R.string.user_new_user_name);
                    mPendingUserIcon = null;
                }
                return buildAddUserDialog(USER_TYPE_USER);
            }
            case DIALOG_USER_PROFILE_EDITOR_ADD_RESTRICTED_PROFILE: {
                synchronized (mUserLock) {
                    mPendingUserName = getString(
                            com.android.settingslib.R.string.user_new_profile_name);
                    mPendingUserIcon = null;
                }
                return buildAddUserDialog(USER_TYPE_RESTRICTED_PROFILE);
            }
            case DIALOG_CONFIRM_REMOVE_GUEST_WITH_AUTO_CREATE: {
                return UserDialogs.createResetGuestDialog(getActivity(),
                        (dialog, which) -> clearAndExitGuest());
            }
            case DIALOG_CONFIRM_RESET_AND_RESTART_GUEST: {
                Dialog dlg = new AlertDialog.Builder(context)
                        .setTitle(
                                com.android.settingslib.R.string.guest_reset_and_restart_dialog_title)
                        .setMessage(
                                com.android.settingslib.R.string.guest_reset_and_restart_dialog_message)
                        .setPositiveButton(
                                com.android.settingslib.R.string.guest_reset_guest_confirm_button,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        resetAndRestartGuest();
                                    }
                                })
                        .setNeutralButton(android.R.string.cancel, null)
                        .create();
                return dlg;
            }
            default:
                return null;
        }
    }

    /**
     * Shows keyguard validation activity if screen lock is set for a user being deleted. If screen
     * lock is not set up, no activity is launched.
     *
     * @return true if the authentication activity has been launched.
     */
    @VisibleForTesting
    boolean runUserRemovalKeyguardConfirmation() {
        final ChooseLockSettingsHelper.Builder builder =
                new ChooseLockSettingsHelper.Builder(getActivity(), this);
        return builder
                .setRequestCode(REQUEST_DELETE_USER)
                .show();
    }

    private Dialog buildEditCurrentUserDialog() {
        final Activity activity = getActivity();
        if (activity == null) {
            return null;
        }

        UserInfo user = mUserManager.getUserInfo(Process.myUserHandle().getIdentifier());
        Drawable userIcon = Utils.getUserIcon(activity, mUserManager, user);

        return mEditUserInfoController.createDialog(
                activity,
                this::startActivityForResult,
                userIcon,
                user.name,
                (newUserName, newUserIcon) -> {
                    if (newUserIcon != userIcon) {
                        ThreadUtils.postOnBackgroundThread(() ->
                                mUserManager.setUserIcon(user.id,
                                        UserIcons.convertToBitmapAtUserIconSize(
                                                activity.getResources(), newUserIcon)));
                        mMePreference.setIcon(newUserIcon);
                        final String pkg = getString(R.string.config_avatar_picker_package);
                        final String action = pkg + ".set.confirm";
                        activity.sendBroadcast(new Intent(action).setPackage(pkg));
                    }

                    if (!TextUtils.isEmpty(newUserName) && !newUserName.equals(user.name)) {
                        mMePreference.setTitle(newUserName);
                        mUserManager.setUserName(user.id, newUserName);
                    }
                }, () -> {
                    final String pkg = getString(R.string.config_avatar_picker_package);
                    final String action = pkg + ".set.cancel";
                    activity.sendBroadcast(new Intent(action).setPackage(pkg));
                });
    }

    private Dialog buildAddUserDialog(int userType) {
        Dialog d;
        synchronized (mUserLock) {
            d = mCreateUserDialogController.createDialog(
                    getActivity(),
                    this::startActivityForResult,
                    canCreateAdminUser(),
                    canEditUserInfo(),
                    (userName, userIcon, iconPath, isAdmin) -> {
                        mPendingUserIcon = userIcon;
                        mPendingUserName = userName;
                        mPendingUserIsAdmin = isAdmin;
                        addUserNow(userType);
                    },
                    () -> {
                        synchronized (mUserLock) {
                            mPendingUserIcon = null;
                            mPendingUserName = null;
                        }
                    }
            );
        }
        return d;
    }

    /**
     * Checks if the creation of a new admin user is allowed.
     *
     * @return {@code true} if creating a new admin is allowed, {@code false} otherwise.
     */
    private boolean canCreateAdminUser() {
        if (Flags.unicornModeRefactoringForHsumReadOnly()) {
            return UserManager.isMultipleAdminEnabled()
                    && !mUserManager.hasUserRestriction(UserManager.DISALLOW_GRANT_ADMIN);
        } else {
            return UserManager.isMultipleAdminEnabled();
        }
    }

    private boolean canEditUserInfo() {
        return !flagShowAddUsersFromSigninToggle()
                || !getPrefContext()
                        .getResources()
                        .getBoolean(com.android.internal.R.bool.config_enableUserInfoSetupInSuw);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        switch (dialogId) {
            case DIALOG_CONFIRM_REMOVE:
                return SettingsEnums.DIALOG_USER_REMOVE;
            case DIALOG_USER_CANNOT_MANAGE:
                return SettingsEnums.DIALOG_USER_CANNOT_MANAGE;
            case DIALOG_GRANT_ADMIN:
                return SettingsEnums.DIALOG_GRANT_USER_ADMIN;
            case DIALOG_ADD_USER:
                return SettingsEnums.DIALOG_USER_ADD;
            case DIALOG_CHOOSE_USER_TYPE:
                return SettingsEnums.DIALOG_USER_CHOOSE_TYPE;
            case DIALOG_NEED_LOCKSCREEN:
                return SettingsEnums.DIALOG_USER_NEED_LOCKSCREEN;
            case DIALOG_CONFIRM_REMOVE_GUEST:
            case DIALOG_CONFIRM_REMOVE_GUEST_WITH_AUTO_CREATE:
            case DIALOG_CONFIRM_EXIT_GUEST_EPHEMERAL:
            case DIALOG_CONFIRM_EXIT_GUEST_NON_EPHEMERAL:
            case DIALOG_CONFIRM_RESET_AND_RESTART_GUEST:
                return SettingsEnums.DIALOG_USER_CONFIRM_EXIT_GUEST;
            case DIALOG_USER_PROFILE_EDITOR:
            case DIALOG_USER_PROFILE_EDITOR_ADD_USER:
            case DIALOG_USER_PROFILE_EDITOR_ADD_RESTRICTED_PROFILE:
                return SettingsEnums.DIALOG_USER_EDIT_PROFILE;
            default:
                return 0;
        }
    }

    private void removeUserNow() {
        if (mRemovingUserId == UserHandle.myUserId()) {
            removeThisUser();
            synchronized (mUserLock) {
                mRemovingUserId = -1;
                mUserRemovalCredentialConfirmationPending = false;
            }
            Toast.makeText(getPrefContext(), R.string.user_deleted_confirmation_toast,
                    Toast.LENGTH_LONG).show();
        }
    }

    private void removeThisUser() {
        if (!canSwitchUserNow()) {
            Log.w(TAG, "Cannot remove current user when switching is disabled");
            return;
        }
        try {
            mUserManager.removeUserWhenPossible(
                    UserHandle.of(UserHandle.myUserId()), /* overrideDevicePolicy= */ false);
            ActivityManager.getService().switchUser(
                    mUserManager.getPreviousForegroundUser().getIdentifier());
        } catch (RemoteException re) {
            Log.e(TAG, "Unable to remove self user");
        }
    }

    private void switchToUserId(int userId) {
        if (!canSwitchUserNow()) {
            Log.w(TAG, "Cannot switch current user when switching is disabled");
            return;
        }
        try {
            ActivityManager.getService().switchUser(userId);
        } catch (RemoteException re) {
            Log.e(TAG, "Unable to switch user");
        }
    }

    private void addUserNow(final int userType) {
        Trace.beginAsyncSection("UserSettings.addUserNow", 0);
        synchronized (mUserLock) {
            mAddingUser = true;
            mAddingUserName = userType == USER_TYPE_USER
                    ? (mPendingUserName != null ? mPendingUserName.toString()
                    : getString(com.android.settingslib.R.string.user_new_user_name))
                    : (mPendingUserName != null ? mPendingUserName.toString()
                            : getString(com.android.settingslib.R.string.user_new_profile_name));
        }

        mUserCreatingDialog = new UserCreatingDialog(getActivity());
        mUserCreatingDialog.show();
        createUser(userType, mAddingUserName);
    }

    @RequiresPermission(allOf = {Manifest.permission.MANAGE_USERS,
            Manifest.permission.INTERACT_ACROSS_USERS_FULL})
    @VisibleForTesting
    void createUser(final int userType, String userName) {
        Context context = getContext();
        Resources resources = getResources();
        final Drawable selectedUserIcon = mPendingUserIcon;
        Future<?> unusedCreateUserFuture = ThreadUtils.postOnBackgroundThread(() -> {
            UserInfo user;

            if (userType == USER_TYPE_USER) {
                user = mUserManager.createUser(
                        userName,
                        mUserManager.USER_TYPE_FULL_SECONDARY,
                        0);
                if (mPendingUserIsAdmin) {
                    mUserManager.setUserAdmin(user.id);
                }
            } else {
                user = mUserManager.createRestrictedProfile(userName);
            }

            ThreadUtils.postOnMainThread(() -> {
                if (user == null) {
                    mAddingUser = false;
                    mPendingUserIcon = null;
                    mPendingUserName = null;
                    mPendingUserIconPath = null;
                    onUserCreationFailed();
                    return;
                }

                Future<?> unusedSettingIconFuture = ThreadUtils.postOnBackgroundThread(() -> {
                    if (mPendingUserIconPath != null) {
                        Bitmap bitmap = BitmapFactory.decodeFile(mPendingUserIconPath);
                        mUserManager.setUserIcon(user.id, bitmap);
                        new File(mPendingUserIconPath).delete();
                        mPendingUserIconPath = null;
                    } else {
                        Drawable newUserIcon = selectedUserIcon;
                        if (newUserIcon == null) {
                            newUserIcon = UserIcons.getDefaultUserIcon(resources, user.id, false);
                        }
                        mUserManager.setUserIcon(user.id, UserIcons.convertToBitmapAtUserIconSize(
                                resources, newUserIcon));
                    }
                });

                mPendingUserIcon = null;
                mPendingUserName = null;

                onUserCreated(user, context);
            });
        });
    }


    /**
     * Erase the current user (guest) and switch to another user.
     */
    @VisibleForTesting
    void clearAndExitGuest() {
        // Just to be safe
        if (!isCurrentUserGuest()) {
            return;
        }
        mMetricsFeatureProvider.action(getActivity(),
                SettingsEnums.ACTION_USER_GUEST_EXIT_CONFIRMED);

        int guestUserId = UserHandle.myUserId();
        // Using markGuestForDeletion allows us to create a new guest before this one is
        // fully removed.
        boolean marked = mUserManager.markGuestForDeletion(guestUserId);
        if (!marked) {
            Log.w(TAG, "Couldn't mark the guest for deletion for user " + guestUserId);
            return;
        }

        removeThisUser();
        if (mGuestUserAutoCreated) {
            scheduleGuestCreation();
        }
    }

    /**
     * Switch to another user.
     */
    private void exitGuest() {
        // Just to be safe
        if (!isCurrentUserGuest()) {
            return;
        }
        mMetricsFeatureProvider.action(getActivity(),
                SettingsEnums.ACTION_USER_GUEST_EXIT_CONFIRMED);
        switchToUserId(mUserManager.getPreviousForegroundUser().getIdentifier());
    }

    private int createGuest() {
        UserInfo guest;
        Context context = getPrefContext();
        try {
            guest = mUserManager.createGuest(context);
        } catch (UserManager.UserOperationException e) {
            Log.e(TAG, "Couldn't create guest user", e);
            return UserHandle.USER_NULL;
        }
        if (guest == null) {
            Log.e(TAG, "Couldn't create guest, most likely because there already exists one");
            return UserHandle.USER_NULL;
        }
        return guest.id;
    }

    /**
     * Remove current guest and start a new guest session
     */
    private void resetAndRestartGuest() {
        // Just to be safe
        if (!isCurrentUserGuest()) {
            return;
        }
        int oldGuestUserId = UserHandle.myUserId();
        // Using markGuestForDeletion allows us to create a new guest before this one is
        // fully removed.
        boolean marked = mUserManager.markGuestForDeletion(oldGuestUserId);
        if (!marked) {
            Log.w(TAG, "Couldn't mark the guest for deletion for user " + oldGuestUserId);
            return;
        }

        try {
            // Create a new guest in the foreground, and then immediately switch to it
            int newGuestUserId = createGuest();
            if (newGuestUserId == UserHandle.USER_NULL) {
                Log.e(TAG, "Could not create new guest, switching back to previous user");
                switchToUserId(mUserManager.getPreviousForegroundUser().getIdentifier());
                mUserManager.removeUser(oldGuestUserId);
                WindowManagerGlobal.getWindowManagerService().lockNow(/* options= */ null);
                return;
            }
            switchToUserId(newGuestUserId);
            mUserManager.removeUser(oldGuestUserId);
        } catch (RemoteException e) {
            Log.e(TAG, "Couldn't remove guest because ActivityManager or WindowManager is dead");
            return;
        }
    }

    /**
     * Create a guest user in the background
     */
    @VisibleForTesting
    void scheduleGuestCreation() {
        // TODO(b/191067027): Move guest recreation to system_server
        if (mGuestCreationScheduled.compareAndSet(/* expect= */ false, /* update= */ true)) {
            // Once mGuestCreationScheduled=true, mAddGuest needs to be updated so that it shows
            // "Resetting guest..."
            mHandler.sendEmptyMessage(MESSAGE_UPDATE_LIST);
            mExecutor.execute(() -> {
                UserInfo guest = mUserManager.createGuest(getContext());
                mGuestCreationScheduled.set(false);
                if (guest == null) {
                    Log.e(TAG, "Unable to automatically recreate guest user");
                }
                // The list needs to be updated whether or not guest creation worked. If guest
                // creation failed, the list needs to update so that "Add guest" is displayed.
                // Otherwise, the UX could be stuck in a state where there is no way to switch to
                // the guest user (e.g. Guest would not be selectable, and it would be stuck
                // saying "Resetting guest...")
                mHandler.sendEmptyMessage(MESSAGE_UPDATE_LIST);
            });
        }
    }

    @VisibleForTesting
    void updateUserList() {
        final Context context = getActivity();
        if (context == null) {
            return;
        }

        List<UserInfo> users;
        // Only human users that can be UI-switched to should show up here.
        // e.g. Managed profiles appear under Accounts Settings instead
        users = mUserManager.getAliveUsers().stream()
                .filter(UserInfo::isUiSwitchableHumanUser)
                // current user is still added first and the user being created last
                .sorted(UserReorderFragment.getUserComparator(context))
                .collect(Collectors.toList());

        final ArrayList<Integer> missingIcons = new ArrayList<>();
        final ArrayList<UserPreference> userPreferences = new ArrayList<>();

        // mMePreference shows a icon for current user. However when current user is a guest, we
        // don't show the guest user icon, instead we show two preferences for guest user to
        // exit and reset itself. Hence we don't add mMePreference, i.e. guest user to the
        // list of users visible in the UI.
        if (!isCurrentUserGuest()) {
            userPreferences.add(mMePreference);
        }

        boolean canOpenUserDetails =
                isCurrentUserAdmin()
                        || (canSwitchUserNow()
                                && !mUserCaps.mDisallowSwitchUser
                                && mUserCaps.mUserSwitchingUiEnabled);

        for (UserInfo user : users) {
            if (user.isGuest()) {
                // Guest user is added to guest category via updateGuestCategory
                // and not to user list so skip guest here
                continue;
            }
            UserPreference pref;
            if (user.id == UserHandle.myUserId()) {
                pref = mMePreference;
            } else {
                pref = new UserPreference(getPrefContext(), null, user.id);
                pref.setTitle(user.name);
                userPreferences.add(pref);
                pref.setOnPreferenceClickListener(this);
                pref.setEnabled(canOpenUserDetails);
                pref.setSelectable(true);
                pref.setKey("id=" + user.id);
            }
            if (pref == null) {
                continue;
            }
            if (user.isMain()) {
                pref.setSummary(R.string.user_owner);
            } else if (user.isAdmin()) {
                pref.setSummary(R.string.user_admin);
            } else {
                pref.setSummary(null);
            }
            boolean isUserSetupComplete = Flags.upgradeUserSummaryBehaviour()
                    ? Settings.Secure.getIntForUser(getContentResolver(),
                            Settings.Secure.USER_SETUP_COMPLETE, 0, user.id) == 1
                    : user.isInitialized();
            if (user.id != UserHandle.myUserId() && !user.isGuest() && !isUserSetupComplete) {
                // sometimes after creating a guest the initialized flag isn't immediately set
                // and we don't want to show "Not set up" summary for them
                if (user.isRestricted()) {
                    pref.setSummary(R.string.user_summary_restricted_not_set_up);
                } else {
                    pref.setSummary(R.string.user_summary_not_set_up);
                    pref.setEnabled(true);
                }
            } else if (user.isRestricted()) {
                pref.setSummary(R.string.user_summary_restricted_profile);
            }
            if (user.iconPath != null) {
                if (mUserIcons.get(user.id) == null) {
                    // Icon not loaded yet, print a placeholder
                    missingIcons.add(user.id);
                    pref.setIcon(getEncircledDefaultIcon());
                } else {
                    setPhotoId(pref, user);
                }
            } else {
                // Icon not available yet, print a placeholder
                pref.setIcon(getEncircledDefaultIcon());
            }
        }

        // Add a temporary entry for the user being created
        if (mAddingUser) {
            UserPreference pref = new UserPreference(getPrefContext(), null,
                    UserPreference.USERID_UNKNOWN);
            pref.setEnabled(false);
            pref.setTitle(mAddingUserName);
            pref.setIcon(getEncircledDefaultIcon());
            userPreferences.add(pref);
        }

        getActivity().invalidateOptionsMenu();

        // Load the icons
        if (missingIcons.size() > 0) {
            loadIconsAsync(missingIcons);
        }

        // If restricted profiles are supported, mUserListCategory will have a special title
        if (mUserCaps.mCanAddRestrictedProfile) {
            mUserListCategory.setTitle(R.string.user_list_title);
        } else if (isCurrentUserGuest()) {
            mUserListCategory.setTitle(R.string.other_user_category_title);
        } else {
            mUserListCategory.setTitle(R.string.user_category_title);
        }

        // Remove everything from mUserListCategory and add new users.
        mUserListCategory.removeAll();

        final Preference addUserOnLockScreen = getPreferenceScreen().findPreference(
                mAddUserWhenLockedPreferenceController.getPreferenceKey());
        mAddUserWhenLockedPreferenceController.updateState(addUserOnLockScreen);

        final Preference addUserFromSignInScreen = getPreferenceScreen().findPreference(
                mAddUserFromSignInPreferenceController.getPreferenceKey());
        mAddUserFromSignInPreferenceController.updateState(addUserFromSignInScreen);

        final Preference guestCallPreference = getPreferenceScreen().findPreference(
                mGuestTelephonyPreferenceController.getPreferenceKey());
        mGuestTelephonyPreferenceController.updateState(guestCallPreference);

        final Preference sendCensoredNotifs = getPreferenceScreen().findPreference(
                mSendCensoredNotificationsToCurrentUserPreferenceController.getPreferenceKey());
        mSendCensoredNotificationsToCurrentUserPreferenceController.updateState(sendCensoredNotifs);

        final Preference multiUserTopIntroPreference = getPreferenceScreen().findPreference(
                mMultiUserTopIntroPreferenceController.getPreferenceKey());
        mMultiUserTopIntroPreferenceController.updateState(multiUserTopIntroPreference);
        updateGuestPreferences();
        updateGuestCategory(context, users);
        updateAddUser(context);
        updateAddSupervisedUser(context);
        updateAddUserSettingsCategory();

        for (UserPreference userPreference : userPreferences) {
            userPreference.setOrder(Preference.DEFAULT_ORDER);
            mUserListCategory.addPreference(userPreference);
        }

    }

    @VisibleForTesting
    void setConfigSupervisedUserCreationPackage() {
        mConfigSupervisedUserCreationPackage = getPrefContext().getString(
                com.android.internal.R.string.config_supervisedUserCreationPackage);
    }

    private boolean isCurrentUserGuest() {
        return mUserCaps.mIsGuest;
    }

    private boolean isCurrentUserAdmin() {
        return mUserCaps.mIsAdmin;
    }

    private boolean canSwitchUserNow() {
        return mUserManager.getUserSwitchability() == UserManager.SWITCHABILITY_STATUS_OK;
    }

    private void updateGuestPreferences() {
        // reset guest and exit guest preferences are shown only in guest mode.
        // For all other users these are not visible.
        mGuestCategory.setVisible(false);
        mGuestResetPreference.setVisible(false);
        mGuestExitPreference.setVisible(false);
        if (!isCurrentUserGuest()) {
            return;
        }
        mGuestCategory.setVisible(true);
        mGuestExitPreference.setVisible(true);
        mGuestResetPreference.setVisible(true);

        boolean isGuestFirstLogin = Settings.Secure.getIntForUser(
                getContext().getContentResolver(),
                SETTING_GUEST_HAS_LOGGED_IN,
                0,
                UserHandle.myUserId()) <= 1;
        String guestExitSummary;
        if (mUserCaps.mIsEphemeral) {
            guestExitSummary = getContext().getString(
                    com.android.settingslib.R.string.guest_notification_ephemeral);
        } else if (isGuestFirstLogin) {
            guestExitSummary = getContext().getString(
                    com.android.settingslib.R.string.guest_notification_non_ephemeral);
        } else {
            guestExitSummary = getContext().getString(
                    com.android.settingslib.R
                            .string.guest_notification_non_ephemeral_non_first_login);
        }
        mGuestExitPreference.setSummary(guestExitSummary);
    }

    private void updateGuestCategory(Context context, List<UserInfo> users) {
        // show guest category title and related guest preferences
        // - if guest is created, then show guest user preference
        // - if guest is not created and its allowed to create guest,
        //   then show "add guest" preference
        // - if allowed, show "reset guest on exit" preference
        // - if there is nothing to show, then make the guest category as not visible
        // - guest category is not visible for guest user.
        UserPreference pref = null;
        boolean isGuestAlreadyCreated = false;
        boolean canOpenUserDetails =
                isCurrentUserAdmin()
                        || (canSwitchUserNow()
                                && !mUserCaps.mDisallowSwitchUser
                                && mUserCaps.mUserSwitchingUiEnabled);

        mGuestUserCategory.removeAll();
        mGuestUserCategory.setVisible(false);
        for (UserInfo user : users) {
            if (!user.isGuest() || !user.isEnabled()) {
                // Only look at enabled, guest users
                continue;
            }
            final Context prefContext = getPrefContext();
            pref = new UserPreference(prefContext, null, user.id);
            pref.setTitle(user.name);
            pref.setOnPreferenceClickListener(this);
            pref.setEnabled(canOpenUserDetails);
            pref.setSelectable(true);
            Drawable icon = getContext().getDrawable(
                    com.android.settingslib.R.drawable.ic_account_circle_outline);
            icon.setTint(
                    getColorAttrDefaultColor(getContext(), android.R.attr.colorControlNormal));
            pref.setIcon(encircleUserIcon(
                    UserIcons.convertToBitmapAtUserIconSize(
                            getContext().getResources(), icon)));
            pref.setKey(KEY_USER_GUEST);
            pref.setOrder(Preference.DEFAULT_ORDER);

            pref.setDisabledByAdmin((EnforcingAdmin) null);
            mGuestUserCategory.addPreference(pref);
            // guest user preference is shown hence also make guest category visible
            mGuestUserCategory.setVisible(true);
            isGuestAlreadyCreated = true;
        }
        boolean isVisible = updateAddGuestPreference(context, isGuestAlreadyCreated);
        if (isVisible) {
            // "add guest" preference is shown hence also make guest category visible
            mGuestUserCategory.setVisible(true);
        }
        final Preference removeGuestOnExit = getPreferenceScreen().findPreference(
                mRemoveGuestOnExitPreferenceController.getPreferenceKey());
        mRemoveGuestOnExitPreferenceController.updateState(removeGuestOnExit);
        if (mRemoveGuestOnExitPreferenceController.isAvailable()) {
            // "reset guest on exit" preference is shown hence also make guest category visible
            mGuestUserCategory.setVisible(true);
        }
        final Preference allowAddGuests = getPreferenceScreen().findPreference(
                mAddGuestFromSignInPreferenceController.getPreferenceKey());
        mAddGuestFromSignInPreferenceController.updateState(allowAddGuests);
        if (mAddGuestFromSignInPreferenceController.isAvailable()) {
            mGuestUserCategory.setVisible(true);
        }
        if (isCurrentUserGuest()) {
            // guest category is not visible for guest user.
            mGuestUserCategory.setVisible(false);
        }
    }

    private boolean updateAddGuestPreference(Context context, boolean isGuestAlreadyCreated) {
        boolean isVisible = false;
        if (!isGuestAlreadyCreated && (mUserCaps.mCanAddGuest || mUserCaps.mDisallowAddUser)
                && mUserManager.canAddMoreUsers(UserManager.USER_TYPE_FULL_GUEST)
                && WizardManagerHelper.isDeviceProvisioned(context)) {
            Drawable icon = context.getDrawable(
                    com.android.settingslib.R.drawable.ic_account_circle);
            mAddGuest.setIcon(centerAndTint(icon));
            isVisible = true;
            mAddGuest.setVisible(true);
            mAddGuest.setSelectable(true);
            if (mGuestUserAutoCreated && mGuestCreationScheduled.get()) {
                mAddGuest.setTitle(com.android.internal.R.string.guest_name);
                mAddGuest.setSummary(com.android.settingslib.R.string.guest_resetting);
                mAddGuest.setEnabled(false);
            } else {
                mAddGuest.setTitle(com.android.settingslib.R.string.guest_new_guest);
                applyAddGuestUserRestrictions();
            }
        } else {
            mAddGuest.setVisible(false);
        }
        return isVisible;
    }

    private void applyAddGuestUserRestrictions() {
        if (android.app.admin.flags.Flags.policyTransparencyRefactorEnabled()) {
            if (!mUserCaps.mDisallowAddUser) {
                mAddGuest.setEnabled(true);
                return;
            }
            // A restriction can be enforced by the system or an admin.
            if (mUserCaps.mDisallowAddUserRestrictionEnforcementInfo.isOnlyEnforcedBySystem()) {
                mAddGuest.setDisabledByAdmin(
                        mUserCaps.mDisallowAddUserRestrictionEnforcementInfo
                                .getMostImportantEnforcingAdmin());
                mAddGuest.setSummary(getPrefContext().getString(
                        R.string.add_user_summary_action_restricted));
            } else {
                mAddGuest.setDisabledByAdmin(
                        mUserCaps.mDisallowAddUserRestrictionEnforcementInfo
                                .getMostImportantEnforcingAdmin());
            }
        } else {
            if (mUserCaps.mDisallowAddUserSetByAdmin) {
                mAddGuest.setDisabledByAdmin(mUserCaps.mEnforcedAdmin);
            } else if (mUserCaps.mDisallowAddUser) {
                final List<UserManager.EnforcingUser> enforcingUsers =
                        mUserManager.getUserRestrictionSources(UserManager.DISALLOW_ADD_USER,
                                UserHandle.of(UserHandle.myUserId()));
                if (!enforcingUsers.isEmpty()) {
                    final UserManager.EnforcingUser enforcingUser = enforcingUsers.get(0);
                    final int restrictionSource = enforcingUser.getUserRestrictionSource();
                    if (restrictionSource == UserManager.RESTRICTION_SOURCE_SYSTEM) {
                        mAddGuest.setDisabledByAdmin(new EnforcingAdmin("android",
                                new SystemAuthority("system"), UserHandle.CURRENT));
                        mAddGuest.setSummary(getPrefContext().getString(
                                R.string.add_user_summary_action_restricted));
                    } else {
                        mAddGuest.setVisible(false);
                    }
                }
            } else {
                mAddGuest.setEnabled(true);
            }
        }
    }

    private void updateAddUser(Context context) {
        updateAddUserCommon(context, mAddUser, mUserCaps.mCanAddRestrictedProfile);
        Drawable icon = context.getDrawable(
                com.android.settingslib.R.drawable.ic_account_circle_filled);
        mAddUser.setIcon(centerAndTint(icon));
    }

    private void updateAddSupervisedUser(Context context) {
        if (!TextUtils.isEmpty(mConfigSupervisedUserCreationPackage)) {
            updateAddUserCommon(context, mAddSupervisedUser, false);
            Drawable icon = context.getDrawable(
                    com.android.settingslib.R.drawable.ic_add_supervised_user);
            mAddSupervisedUser.setIcon(centerAndTint(icon));
        } else {
            mAddSupervisedUser.setVisible(false);
        }
    }

    private void updateAddUserCommon(Context context, RestrictedPreference addUser,
            boolean canAddRestrictedProfile) {
        if (android.app.admin.flags.Flags.policyTransparencyRefactorEnabled()) {
            // If mUserCaps.mCanAddUser is true, it means that adding user is not restricted by
            // any admin nor system. mUserCaps.mDisallowAddUser will be false in this case.
            if (mUserCaps.mCanAddUser && WizardManagerHelper.isDeviceProvisioned(context)) {
                addUser.setVisible(true);
                addUser.setSelectable(true);
                final boolean canAddMoreUsers =
                        mUserManager.canAddMoreUsers(UserManager.USER_TYPE_FULL_SECONDARY)
                                || (canAddRestrictedProfile
                                && mUserManager.canAddMoreUsers(
                                UserManager.USER_TYPE_FULL_RESTRICTED));
                addUser.setEnabled(canAddMoreUsers && !mAddingUser);
                addUser.setSummary(canAddMoreUsers ? null : getString(R.string.user_add_max_count));
            } else if (mUserCaps.mDisallowAddUser) {
                addUser.setVisible(true);
                // If disallow add user restriction is set, it can either be enforced by an admin
                // or the system. If it's enforced by an admin, show the admin disabled UI.
                if (mUserCaps.mDisallowAddUserSetByAdmin) {
                    addUser.setDisabledByAdmin(
                            mUserCaps.mDisallowAddUserRestrictionEnforcementInfo
                                    .getMostImportantEnforcingAdmin());
                } else {
                    addUser.setDisabledByAdmin(
                            mUserCaps.mDisallowAddUserRestrictionEnforcementInfo
                                    .getMostImportantEnforcingAdmin());
                    addUser.setSummary(context.getString(
                            R.string.add_user_summary_action_restricted));
                }
            } else {
                addUser.setVisible(false);
            }
        } else {
            if (mUserCaps.mCanAddUser && !mUserCaps.mDisallowAddUserSetByAdmin
                    && WizardManagerHelper.isDeviceProvisioned(context)) {
                addUser.setVisible(true);
                addUser.setSelectable(true);
                final boolean canAddMoreUsers =
                        mUserManager.canAddMoreUsers(UserManager.USER_TYPE_FULL_SECONDARY)
                                || (canAddRestrictedProfile
                                && mUserManager.canAddMoreUsers(UserManager.USER_TYPE_FULL_RESTRICTED));
                addUser.setEnabled(canAddMoreUsers && !mAddingUser);

                if (!canAddMoreUsers) {
                    addUser.setSummary(getString(R.string.user_add_max_count));
                } else {
                    addUser.setSummary(null);
                }
                if (addUser.isEnabled()) {
                    addUser.setDisabledByAdmin(
                            mUserCaps.mDisallowAddUser ? mUserCaps.mEnforcedAdmin : null);
                }
            } else if (mUserCaps.mDisallowAddUserSetByAdmin) {
                addUser.setVisible(true);
                addUser.setDisabledByAdmin(mUserCaps.mEnforcedAdmin);
            } else if (mUserCaps.mDisallowAddUser) {
                final List<UserManager.EnforcingUser> enforcingUsers =
                        mUserManager.getUserRestrictionSources(UserManager.DISALLOW_ADD_USER,
                                UserHandle.of(UserHandle.myUserId()));
                if (!enforcingUsers.isEmpty()) {
                    final UserManager.EnforcingUser enforcingUser = enforcingUsers.get(0);
                    final int restrictionSource = enforcingUser.getUserRestrictionSource();
                    if (restrictionSource == UserManager.RESTRICTION_SOURCE_SYSTEM) {
                        addUser.setVisible(true);
                        addUser.setDisabledByAdmin(
                                new EnforcingAdmin("android", new SystemAuthority("system"),
                                        UserHandle.CURRENT));
                        addUser.setSummary(context.getString(
                                R.string.add_user_summary_action_restricted));
                    } else {
                        addUser.setVisible(false);
                    }
                }
            } else {
                addUser.setVisible(false);
            }
        }
    }

    private void updateAddUserSettingsCategory() {
        mAddUserSettingsCategory.setVisible(mAddUserFromSignInPreferenceController.isAvailable());
    }

    private Drawable centerAndTint(Drawable icon) {
        icon.setTintBlendMode(BlendMode.SRC_IN);
        icon.setTint(getColorAttrDefaultColor(getContext(), android.R.attr.textColorPrimary));

        Drawable bg = getContext().getDrawable(com.android.settingslib.R.drawable.user_avatar_bg)
                .mutate();
        LayerDrawable ld = new LayerDrawable(new Drawable[]{bg, icon});
        int size = getContext().getResources().getDimensionPixelSize(
                R.dimen.multiple_users_avatar_size);
        int bgSize = getContext().getResources().getDimensionPixelSize(
                R.dimen.multiple_users_user_icon_size);
        ld.setLayerSize(1, size, size);
        ld.setLayerSize(0, bgSize, bgSize);
        ld.setLayerGravity(1, Gravity.CENTER);

        return ld;
    }

    /**
     * @return number of non-guest non-managed users
     */
    @VisibleForTesting
    int getRealUsersCount() {
        return (int) mUserManager.getUsers()
                .stream()
                .filter(user -> !user.isGuest() && !user.isProfile())
                .count();
    }

    private void loadIconsAsync(List<Integer> missingIcons) {
        new AsyncTask<List<Integer>, Void, Void>() {
            @Override
            protected void onPostExecute(Void result) {
                updateUserList();
            }

            @Override
            protected Void doInBackground(List<Integer>... values) {
                for (int userId : values[0]) {
                    Bitmap bitmap = mUserManager.getUserIcon(userId);
                    if (bitmap == null) {
                        bitmap = getDefaultUserIconAsBitmap(getContext().getResources(), userId);
                    }
                    mUserIcons.append(userId, bitmap);
                }
                return null;
            }
        }.execute(missingIcons);
    }

    private Drawable getEncircledDefaultIcon() {
        if (mDefaultIconDrawable == null) {
            mDefaultIconDrawable = encircleUserIcon(
                    getDefaultUserIconAsBitmap(getContext().getResources(), UserHandle.USER_NULL));
        }
        return mDefaultIconDrawable;
    }

    private void setPhotoId(Preference pref, UserInfo user) {
        Bitmap bitmap = mUserIcons.get(user.id);
        if (bitmap != null) {
            pref.setIcon(encircleUserIcon(bitmap));
        }
    }

    @Override
    public boolean onPreferenceClick(Preference pref) {
        mMetricsFeatureProvider.logSettingsTileClick(pref.getKey(), getMetricsCategory());
        if (isCurrentUserGuest()) {
            if (mGuestResetPreference != null && pref == mGuestResetPreference) {
                showDialog(DIALOG_CONFIRM_RESET_AND_RESTART_GUEST);
                return true;
            }
            if (mGuestExitPreference != null && pref == mGuestExitPreference) {
                if (mUserCaps.mIsEphemeral) {
                    showDialog(DIALOG_CONFIRM_EXIT_GUEST_EPHEMERAL);
                } else {
                    showDialog(DIALOG_CONFIRM_EXIT_GUEST_NON_EPHEMERAL);
                }
                return true;
            }
        }
        if (pref == mMePreference) {
            if (!isCurrentUserGuest()) {
                if (flagShowUserDetailsSettingsForSelf()) {
                    UserInfo userInfo = mUserManager.getUserInfo(UserHandle.myUserId());
                    openUserDetails(userInfo, false);
                } else {
                    showDialog(DIALOG_USER_PROFILE_EDITOR);
                }
                return true;
            }
        } else if (pref instanceof UserPreference) {
            UserInfo userInfo = mUserManager.getUserInfo(((UserPreference) pref).getUserId());
            openUserDetails(userInfo, false);
            return true;
        } else if (pref == mAddUser) {
            mMetricsFeatureProvider.action(getActivity(), SettingsEnums.ACTION_USER_ADD);
            // If we allow both types, show a picker, otherwise directly go to
            // flow for full user.
            if (mUserCaps.mCanAddRestrictedProfile) {
                showDialog(DIALOG_CHOOSE_USER_TYPE);
            } else {
                startActivityForResult(
                        CreateUserActivity.createIntentForStart(
                                getActivity(),
                                canCreateAdminUser(),
                                canEditUserInfo(),
                                Utils.FILE_PROVIDER_AUTHORITY),
                        REQUEST_ADD_USER);
            }
            return true;
        } else if (pref == mAddSupervisedUser) {
            mMetricsFeatureProvider.action(getActivity(), SettingsEnums.ACTION_USER_SUPERVISED_ADD);
            Trace.beginSection("UserSettings.addSupervisedUser");
            onAddSupervisedUserClicked();
            Trace.endSection();
            return true;
        } else if (pref == mAddGuest) {
            mAddGuest.setEnabled(false); // prevent multiple tap issue
            onAddGuestClicked();
            return true;
        }
        return false;
    }

    private Drawable encircleUserIcon(Bitmap icon) {
        return new CircleFramedDrawable(
                icon,
                getActivity().getResources().getDimensionPixelSize(
                        R.dimen.multiple_users_user_icon_size));
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        synchronized (mUserLock) {
            if (!mUserRemovalCredentialConfirmationPending) {
                mRemovingUserId = -1;
            }
            updateUserList();
            if (mCreateUserDialogController.isActive()) {
                mCreateUserDialogController.finish();
            }
        }
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_users;
    }

    /**
     * Returns a default user icon (as a {@link Bitmap}) for the given user.
     *
     * Note that for guest users, you should pass in {@code UserHandle.USER_NULL}.
     *
     * @param resources resources object to fetch the user icon.
     * @param userId    the user id or {@code UserHandle.USER_NULL} for a non-user specific icon
     */
    private static Bitmap getDefaultUserIconAsBitmap(Resources resources, int userId) {
        Bitmap bitmap = null;
        // Try finding the corresponding bitmap in the dark bitmap cache
        bitmap = sDarkDefaultUserBitmapCache.get(userId);
        if (bitmap == null) {
            bitmap = UserIcons.convertToBitmapAtUserIconSize(resources,
                    UserIcons.getDefaultUserIcon(resources, userId, false));
            // Save it to cache
            sDarkDefaultUserBitmapCache.put(userId, bitmap);
        }
        return bitmap;
    }

    /**
     * Assign the default photo to user with {@paramref userId}
     *
     * @param context used to get the {@link UserManager}
     * @param userId  used to get the icon bitmap
     * @return true if assign photo successfully, false if failed
     */
    @VisibleForTesting
    static boolean assignDefaultPhoto(Context context, int userId) {
        if (context == null) {
            return false;
        }
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        Bitmap bitmap = getDefaultUserIconAsBitmap(context.getResources(), userId);
        um.setUserIcon(userId, bitmap);

        return true;
    }

    @WorkerThread
    static void copyMeProfilePhoto(Context context, UserInfo user) {
        Uri contactUri = ContactsContract.Profile.CONTENT_URI;

        int userId = user != null ? user.id : UserHandle.myUserId();

        InputStream avatarDataStream = ContactsContract.Contacts.openContactPhotoInputStream(
                context.getContentResolver(),
                contactUri, true);
        // If there's no profile photo, assign a default avatar
        if (avatarDataStream == null) {
            assignDefaultPhoto(context, userId);
            return;
        }

        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        Bitmap decodedIcon = BitmapFactory.decodeStream(avatarDataStream);
        CircleFramedDrawable drawable = CircleFramedDrawable.getInstance(context, decodedIcon);
        Bitmap icon = UserIcons.convertToBitmapAtUserIconSize(context.getResources(), drawable);

        um.setUserIcon(userId, icon);
        try {
            avatarDataStream.close();
        } catch (IOException ioe) {
        }
    }

    public static final BaseSearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider(R.xml.user_settings) {

                @Override
                protected boolean isPageSearchEnabled(Context context) {
                    final UserCapabilities userCaps = UserCapabilities.create(context);
                    return userCaps.mEnabled;
                }

                @Override
                public List<SearchIndexableRaw> getRawDataToIndex(Context context,
                        boolean enabled) {
                    final List<SearchIndexableRaw> rawData = new ArrayList<>();
                    if (!UserManager.supportsMultipleUsers()) {
                        return rawData;
                    }

                    SearchIndexableRaw multipleUsersData = new SearchIndexableRaw(context);
                    multipleUsersData.key = KEY_USER_SETTINGS_SCREEN;
                    multipleUsersData.title =
                            context.getString(R.string.user_settings_title);
                    multipleUsersData.keywords =
                            context.getString(R.string.multiple_users_title_keywords);
                    multipleUsersData.screenTitle =
                            context.getString(R.string.user_settings_title);
                    rawData.add(multipleUsersData);

                    SearchIndexableRaw addUserData = new SearchIndexableRaw(context);
                    addUserData.key = KEY_ADD_USER;

                    // Dynamically set the title of addUser preference
                    final UserCapabilities userCaps = UserCapabilities.create(context);
                    if (!userCaps.mCanAddRestrictedProfile) {
                        addUserData.title = context.getString(
                                com.android.settingslib.R.string.user_add_user);
                    } else {
                        addUserData.title = context.getString(
                                R.string.user_add_user_or_profile_menu);
                    }
                    addUserData.screenTitle = context.getString(R.string.user_settings_title);
                    addUserData.iconResId = R.drawable.ic_add_40dp;

                    rawData.add(addUserData);

                    return rawData;
                }

                @Override
                public List<String> getNonIndexableKeysFromXml(Context context, int xmlResId,
                        boolean suppressAllPage) {
                    final List<String> niks = super.getNonIndexableKeysFromXml(context, xmlResId,
                            suppressAllPage);
                    if (TextUtils.isEmpty(context.getString(
                            com.android.internal.R.string.config_supervisedUserCreationPackage))) {
                        niks.add(KEY_ADD_SUPERVISED_USER);
                    }
                    AddUserWhenLockedPreferenceController controller =
                            new AddUserWhenLockedPreferenceController(
                                    context, KEY_ADD_USER_WHEN_LOCKED);
                    controller.updateNonIndexableKeys(niks);
                    new SendCensoredNotificationsToCurrentUserPreferenceController(context,
                            KEY_SEND_CENSORED_NOTIFICATIONS).updateNonIndexableKeys(niks);
                    new AutoSyncDataPreferenceController(context, null /* parent */)
                            .updateNonIndexableKeys(niks);
                    new AutoSyncPersonalDataPreferenceController(context, null /* parent */)
                            .updateNonIndexableKeys(niks);
                    new AutoSyncWorkDataPreferenceController(context, null /* parent */)
                            .updateNonIndexableKeys(niks);
                    return niks;
                }
            };

    private boolean flagShowAddUsersFromSigninToggle() {
        return showAddUsersFromSigninToggle() || showAddUsersFromSigninToggle2();
    }

    private boolean flagShowUserDetailsSettingsForSelf() {
        return showUserDetailsSettingsForSelf() || showUserDetailsSettingsForSelf2();
    }
}
// LINT.ThenChange(UserSettingsScreenApi.kt)
