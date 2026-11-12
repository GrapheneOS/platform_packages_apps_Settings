package com.android.settings.users;

import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.pm.UserInfo;
import android.os.Bundle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.settings.R;
import com.android.settings.core.InstrumentedFragment;
import com.android.settingslib.Utils;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Enables main user to change the order of the other users in the user list and the user switcher.
 * The main user is always first and the guest user is always last.
 */
public class UserReorderFragment extends InstrumentedFragment {
    private static final int MENU_RESET = Menu.FIRST;

    private UserManager mUserManager;
    private final ArrayList<UserInfo> mUsers = new ArrayList<>();
    private final UserAdapter mAdapter = new UserAdapter();
    private ItemTouchHelper mItemTouchHelper;
    private boolean mOrderChanged;

    static boolean canReorderUsers(UserCapabilities caps, UserManager userManager) {
        return caps.mIsMain && getMovableUsers(userManager).size() >= 2;
    }

    static Comparator<UserInfo> getUserComparator(Context context) {
        Map<Integer, Integer> order = getUserOrder(context);
        return Comparator.comparing((UserInfo user) -> !user.isMain())
                .thenComparingInt(user -> order.getOrDefault(user.serialNumber, Integer.MAX_VALUE))
                .thenComparing(user -> user.name == null ? "" : user.name, Collator.getInstance());
    }

    private static Map<Integer, Integer> getUserOrder(Context context) {
        Map<Integer, Integer> order = new HashMap<>();
        String value = Settings.Global.getString(context.getContentResolver(),
                Settings.Global.USER_SWITCHER_ORDER);
        if (value == null) {
            return order;
        }
        for (String item : value.split(",")) {
            try {
                order.putIfAbsent(Integer.parseInt(item), order.size());
            } catch (NumberFormatException e) {
            }
        }
        return order;
    }

    private static List<UserInfo> getMovableUsers(UserManager userManager) {
        return userManager.getAliveUsers().stream()
                .filter(user -> user.isUiSwitchableHumanUser() && !user.isMain() && !user.isGuest())
                .collect(Collectors.toList());
    }

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.USER;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mUserManager = getContext().getSystemService(UserManager.class);
        if (!mUserManager.isMainUser()) {
            getActivity().finish();
            return;
        }
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        RecyclerView list = new RecyclerView(inflater.getContext());
        list.setLayoutManager(new LinearLayoutManager(inflater.getContext()));
        list.setAdapter(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(new DragCallback());
        mItemTouchHelper.attachToRecyclerView(list);
        return list;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUsers();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.user_reorder_reset)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == MENU_RESET) {
            Settings.Global.putString(getContext().getContentResolver(),
                    Settings.Global.USER_SWITCHER_ORDER, null);
            loadUsers();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void loadUsers() {
        mUsers.clear();
        mUsers.addAll(getMovableUsers(mUserManager));
        mUsers.sort(getUserComparator(getContext()));
        mAdapter.notifyDataSetChanged();
    }

    private void saveOrder() {
        String value = mUsers.stream()
                .map(user -> String.valueOf(user.serialNumber))
                .collect(Collectors.joining(","));
        Settings.Global.putString(getContext().getContentResolver(),
                Settings.Global.USER_SWITCHER_ORDER, value);
    }

    private boolean moveUser(int from, int to) {
        if (from == RecyclerView.NO_POSITION || to < 0 || to >= mUsers.size()) {
            return false;
        }
        mUsers.add(to, mUsers.remove(from));
        mAdapter.notifyItemMoved(from, to);
        return true;
    }

    private class DragCallback extends ItemTouchHelper.SimpleCallback {
        DragCallback() {
            super(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0);
        }

        @Override
        public boolean onMove(@NonNull RecyclerView list, @NonNull RecyclerView.ViewHolder source,
                @NonNull RecyclerView.ViewHolder target) {
            boolean moved = moveUser(source.getBindingAdapterPosition(),
                    target.getBindingAdapterPosition());
            mOrderChanged |= moved;
            return moved;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
        }

        @Override
        public void clearView(@NonNull RecyclerView list,
                @NonNull RecyclerView.ViewHolder holder) {
            super.clearView(list, holder);
            if (mOrderChanged) {
                mOrderChanged = false;
                saveOrder();
            }
        }
    }

    private class UserAdapter extends RecyclerView.Adapter<UserViewHolder> {
        @NonNull
        @Override
        public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new UserViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.user_reorder_item, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
            holder.bind(mUsers.get(position));
        }

        @Override
        public int getItemCount() {
            return mUsers.size();
        }
    }

    private class UserViewHolder extends RecyclerView.ViewHolder {
        private final ImageView mIcon;
        private final TextView mName;

        UserViewHolder(View view) {
            super(view);
            mIcon = view.findViewById(R.id.user_icon);
            mName = view.findViewById(R.id.user_name);
            view.findViewById(R.id.drag_handle).setOnTouchListener((v, event) -> {
                if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
                    mItemTouchHelper.startDrag(this);
                }
                return false;
            });

            // Dragging doesn't work with accessibility services, so offer actions instead
            ViewCompat.addAccessibilityAction(view,
                    getString(R.string.action_drag_label_move_up), (v, args) -> moveBy(-1));
            ViewCompat.addAccessibilityAction(view,
                    getString(R.string.action_drag_label_move_down), (v, args) -> moveBy(1));
        }

        void bind(UserInfo user) {
            mIcon.setImageDrawable(Utils.getUserIcon(itemView.getContext(), mUserManager, user));
            mName.setText(user.name);
        }

        private boolean moveBy(int offset) {
            int from = getBindingAdapterPosition();
            if (!moveUser(from, from + offset)) {
                return false;
            }
            saveOrder();
            return true;
        }
    }
}
