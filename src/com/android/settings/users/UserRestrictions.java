package com.android.settings.users;

import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.ArraySet;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

public final class UserRestrictions {
    private static final String TAG = "UserRestrictions";

    @NonNull
    private final UserManager userManager;
    @NonNull
    public final UserInfo userInfo;

    @Nullable
    public static UserRestrictions createInstance(@NonNull Context ctx, int userId) {
        UserManager userManager = ctx.getSystemService(UserManager.class);
        if (userManager == null) {
            return null;
        }

        return createInstance(userManager, userId);
    }

    @Nullable
    public static UserRestrictions createInstance(@NonNull UserManager userManager, int userId) {
        UserInfo userInfo = userManager.getUserInfo(userId);
        if (userInfo == null) {
            return null;
        }

        return new UserRestrictions(userManager, userInfo);
    }

    @NonNull
    public static UserRestrictions createInstance(@NonNull UserManager userManager, @NonNull UserInfo userInfo) {
        return new UserRestrictions(userManager, userInfo);
    }

    private UserRestrictions(@NonNull UserManager userManager, @NonNull UserInfo userInfo) {
        this.userManager = userManager;
        this.userInfo = userInfo;
    }

    public boolean isSet(String restrictionKey) {
        final boolean isSetFromUser = userManager.hasUserRestrictionForUser(restrictionKey, userInfo.getUserHandle());
        if (userInfo.isGuest()) {
            return isSetFromUser || userManager.getDefaultGuestRestrictions().getBoolean(restrictionKey);
        }

        return isSetFromUser;
    }

    // Inherit user-controllable restrictions from parent user to its Private space.
    // Upstream intends to do the same after adding support for having profiles in secondary users,
    // see https://github.com/GrapheneOS/platform_packages_apps_Settings/commit/84bc8a918d8ae9c261dbe4b24fffab6dfd4f61ae
    public static final ArraySet<String> INHERITED_PRIVATE_SPACE_RESTRICTIONS = new ArraySet(Arrays.asList(
            UserManager.DISALLOW_INSTALL_APPS,
            UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES,
            UserManager.DISALLOW_NETWORK_ACCESS,
            UserManager.DISALLOW_OUTGOING_CALLS,
            // Private space is automatically stopped when its parent is stopped as of Android 16 QPR2.
            // Inherit the restriction in case this changes in the future.
            UserManager.DISALLOW_RUN_IN_BACKGROUND,
            UserManager.DISALLOW_SMS
    ));

    public static void initInheritedPrivateSpaceRestrictions(
            UserManager userManager, UserHandle parentUser, UserHandle privateSpaceUser) {
        Log.d(TAG, "initInheritedPrivateSpaceRestrictions: "
                + "parent id " + parentUser.getIdentifier()
                + ", Private Space id " + privateSpaceUser.getIdentifier());
        for (String restriction : INHERITED_PRIVATE_SPACE_RESTRICTIONS) {
            if (userManager.hasUserRestriction(restriction, parentUser)) {
                Log.d(TAG, "inheriting " + restriction);
                userManager.setUserRestriction(restriction, true, privateSpaceUser);
            } else {
                Log.d(TAG, "parent doesn't have " + restriction + " restriction");
            }
        }
    }

    public void set(String restrictionKey, boolean enableRestriction) {
        if (userInfo.isGuest()) {
            Bundle defaultGuestRestrictions = userManager.getDefaultGuestRestrictions();
            defaultGuestRestrictions.putBoolean(restrictionKey, enableRestriction);
            userManager.setDefaultGuestRestrictions(defaultGuestRestrictions);
        } else {
            userManager.setUserRestriction(restrictionKey, enableRestriction, userInfo.getUserHandle());
        }

        if (userInfo.isFull()) {
            if (INHERITED_PRIVATE_SPACE_RESTRICTIONS.contains(restrictionKey)) {
                for (UserInfo profileUser : userManager.getProfiles(userInfo.id)) {
                    if (profileUser.isPrivateProfile()) {
                        Log.d(TAG, "inheriting " + restrictionKey + " = " + enableRestriction
                            + " from user " + userInfo.id
                            + " to its Private space " + profileUser.id);
                        userManager.setUserRestriction(restrictionKey, enableRestriction,
                                profileUser.getUserHandle());
                    }
                }
            }
        }
    }

    public static void syncPrivateSpaceRestrictions(Context ctx) {
        Log.d(TAG, "syncPrivateSpaceRestrictions");
        var userManager = requireNonNull(ctx.getSystemService(UserManager.class));
        for (UserInfo user : userManager.getUsers()) {
            if (!user.isPrivateProfile()) {
                continue;
            }
            Log.d(TAG, "handling Private space " + user.id);
            UserHandle parent = userManager.getProfileParent(user.getUserHandle());
            if (parent == null) {
                Log.e(TAG, "Private space parent not found");
                continue;
            }
            initInheritedPrivateSpaceRestrictions(userManager, parent, user.getUserHandle());
        }
    }

    public static void fixupPrivateSpaceRestrictions(Context ctx) {
        File markerFile = new File(ctx.getFilesDir(), "private_space_restrictions_fixed_up");
        if (markerFile.isFile()) {
            return;
        }
        Log.d(TAG, "fixupPrivateSpaceRestrictions");
        syncPrivateSpaceRestrictions(ctx);
        try {
            if (markerFile.createNewFile()) {
                Log.d(TAG, "created marker file " + markerFile);
            } else {
                Log.e(TAG, "unable to create a marker file");
            }
        } catch (IOException e) {
            Log.e(TAG, "unable to create a marker file", e);
        }
    }
}
