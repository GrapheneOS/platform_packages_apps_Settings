package com.android.settings.users;

import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;

import java.util.List;

final class UserRestrictions {

    final UserManager userManager;
    final UserInfo userInfo;

    UserRestrictions(UserManager userManager, UserInfo userInfo) {
        this.userManager = userManager;
        this.userInfo = userInfo;
    }

    boolean isSet(String restrictionKey) {
        final boolean isSetFromUser = userManager.hasUserRestriction(restrictionKey, userInfo.getUserHandle());
        if (userInfo.isGuest()) {
            return isSetFromUser || userManager.getDefaultGuestRestrictions().getBoolean(restrictionKey);
        }

        return isSetFromUser;
    }

    void set(String restrictionKey, boolean enableRestriction) {
        Bundle defaultGuestRestrictions = userManager.getDefaultGuestRestrictions();
        if (userInfo.isGuest()) {
            defaultGuestRestrictions.putBoolean(restrictionKey, enableRestriction);
            userManager.setDefaultGuestRestrictions(defaultGuestRestrictions);

            List<UserInfo> users = userManager.getAliveUsers();
            for (UserInfo user : users) {
                if (user.isGuest()) {
                    UserHandle userHandle = userInfo.getUserHandle();
                    for (String key : defaultGuestRestrictions.keySet()) {
                        userManager.setUserRestriction(
                                key, defaultGuestRestrictions.getBoolean(key), userHandle);
                    }
                }
            }
        } else {
            userManager.setUserRestriction(restrictionKey, enableRestriction, userInfo.getUserHandle());
        }
    }
}
