/*
 * Copyright (C) 2014 Slimroms
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

package com.android.settings.slim;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.preference.SwitchPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import com.android.internal.util.slim.AppHelper;
import com.android.internal.util.slim.ActionConstants;
import com.android.internal.util.slim.DeviceUtils;
import com.android.internal.util.slim.DeviceUtils.FilteredDeviceFeaturesArray;
import com.android.internal.util.slim.VKeyHelper;

import com.android.settings.SettingsPreferenceFragment;
import com.android.settings.R;
import com.android.settings.slim.util.ShortcutPickerHelper;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class VirtualKeysSettings extends SettingsPreferenceFragment implements
        OnPreferenceChangeListener, OnPreferenceClickListener,
        ShortcutPickerHelper.OnPickListener {

    private static final String TAG = "VirtualKeys";

    private static final String CATEGORY_BACK = "vbutton_keys_back";
    private static final String CATEGORY_CAMERA = "vbutton_keys_camera";
    private static final String CATEGORY_HOME = "vbutton_keys_home";
    private static final String CATEGORY_ASSIST = "vbutton_keys_assist";
    private static final String CATEGORY_APPSWITCH = "vbutton_keys_appSwitch";

    private static final String KEYS_ENABLE_CUSTOM = "enable_virtual_rebind";
    private static final String KEYS_BACK_LONG_PRESS = "vkeys_back_long_press";
    private static final String KEYS_CAMERA_PRESS = "vkeys_camera_press";
    private static final String KEYS_CAMERA_LONG_PRESS = "vkeys_camera_long_press";
    private static final String KEYS_CAMERA_DOUBLE_TAP = "vkeys_camera_double_tap";
    private static final String KEYS_HOME_DOUBLE_TAP = "vkeys_home_double_tap";
    private static final String KEYS_ASSIST_PRESS = "vkeys_assist_press";
    private static final String KEYS_ASSIST_LONG_PRESS = "vkeys_assist_long_press";
    private static final String KEYS_ASSIST_DOUBLE_TAP = "vkeys_assist_double_tap";
    private static final String KEYS_APP_SWITCH_LONG_PRESS = "vkeys_app_switch_long_press";
    private static final String KEYS_APP_SWITCH_DOUBLE_TAP = "vkeys_app_switch_double_tap";

    private static final int DLG_SHOW_WARNING_DIALOG = 0;
    private static final int DLG_SHOW_ACTION_DIALOG  = 1;
    private static final int DLG_RESET_TO_DEFAULT    = 2;

    private static final int MENU_RESET = Menu.FIRST;

    private SwitchPreference mEnableCustomBindings;
    private Preference mBackLongPressAction;
    private Preference mCameraPressAction;
    private Preference mCameraLongPressAction;
    private Preference mCameraDoubleTapAction;
    private Preference mHomeDoubleTapAction;
    private Preference mAssistPressAction;
    private Preference mAssistLongPressAction;
    private Preference mAssistDoubleTapAction;
    private Preference mAppSwitchLongPressAction;
    private Preference mAppSwitchDoubleTapAction;

    private boolean mCheckPreferences;
    private Map<String, String> mKeySettings = new HashMap<String, String>();

    private ShortcutPickerHelper mPicker;
    private String mPendingSettingsKey;
    private static FilteredDeviceFeaturesArray sFinalActionDialogArray;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPicker = new ShortcutPickerHelper(getActivity(), this);

        // Before we start filter out unsupported options on the
        // ListPreference values and entries
        Resources res = getResources();
        sFinalActionDialogArray = new FilteredDeviceFeaturesArray();
        sFinalActionDialogArray = DeviceUtils.filterUnsupportedDeviceFeatures(getActivity(),
                res.getStringArray(res.getIdentifier(
                        "shortcut_action_hwkey_values", "array", "com.android.settings")),
                res.getStringArray(res.getIdentifier(
                        "shortcut_action_hwkey_entries", "array", "com.android.settings")));


        // Attach final settings screen.
        reloadSettings();

        setHasOptionsMenu(true);
    }

    private PreferenceScreen reloadSettings() {
        mCheckPreferences = false;
        PreferenceScreen prefs = getPreferenceScreen();
        if (prefs != null) {
            prefs.removeAll();
        }

        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.virtualkeys_settings);
        prefs = getPreferenceScreen();

        PreferenceCategory keysBackCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_BACK);
        PreferenceCategory keysCameraCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_CAMERA);
        PreferenceCategory keysHomeCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_HOME);
        PreferenceCategory keysAssistCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_ASSIST);
        PreferenceCategory keysAppSwitchCategory =
                (PreferenceCategory) prefs.findPreference(CATEGORY_APPSWITCH);

        mEnableCustomBindings = (SwitchPreference) prefs.findPreference(
                KEYS_ENABLE_CUSTOM);
        mBackLongPressAction = (Preference) prefs.findPreference(
                KEYS_BACK_LONG_PRESS);
        mCameraPressAction = (Preference) prefs.findPreference(
                KEYS_CAMERA_PRESS);
        mCameraLongPressAction = (Preference) prefs.findPreference(
                KEYS_CAMERA_LONG_PRESS);
        mCameraDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_CAMERA_DOUBLE_TAP);
        mHomeDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_HOME_DOUBLE_TAP);
        mAssistPressAction = (Preference) prefs.findPreference(
                KEYS_ASSIST_PRESS);
        mAssistLongPressAction = (Preference) prefs.findPreference(
                KEYS_ASSIST_LONG_PRESS);
        mAssistDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_ASSIST_DOUBLE_TAP);
        mAppSwitchLongPressAction = (Preference) prefs.findPreference(
                KEYS_APP_SWITCH_LONG_PRESS);
        mAppSwitchDoubleTapAction = (Preference) prefs.findPreference(
                KEYS_APP_SWITCH_DOUBLE_TAP);

            // Back key longpress
            setupOrUpdatePreference(mBackLongPressAction,
                    VKeyHelper.getLongPressOnBackBehavior(getActivity(), false),
                    Settings.System.VKEY_BACK_LONG_PRESS_ACTION);


            // Camera key
            setupOrUpdatePreference(mCameraPressAction,
                    VKeyHelper.getPressOnCameraBehavior(getActivity(), false),
                    Settings.System.VKEY_CAMERA_ACTION);

            // Camera key longpress
            setupOrUpdatePreference(mCameraLongPressAction,
                    VKeyHelper.getLongPressOnCameraBehavior(getActivity(), false),
                    Settings.System.VKEY_CAMERA_LONG_PRESS_ACTION);

            // Camera key double tap
            setupOrUpdatePreference(mCameraDoubleTapAction,
                    VKeyHelper.getDoubleTapOnCameraBehavior(getActivity(), false),
                    Settings.System.VKEY_CAMERA_DOUBLE_TAP_ACTION);


            // Home key double tap
            setupOrUpdatePreference(mHomeDoubleTapAction,
                    VKeyHelper.getDoubleTapOnHomeBehavior(getActivity(), false),
                    Settings.System.VKEY_HOME_DOUBLE_TAP_ACTION);


            // Assistant key
            setupOrUpdatePreference(mAssistPressAction,
                    VKeyHelper.getPressOnAssistBehavior(getActivity(), false),
                    Settings.System.VKEY_ASSIST_ACTION);

            // Assistant key longpress
            setupOrUpdatePreference(mAssistLongPressAction,
                    VKeyHelper.getLongPressOnAssistBehavior(getActivity(), false),
                    Settings.System.VKEY_ASSIST_LONG_PRESS_ACTION);

            // Assistant key double tap
            setupOrUpdatePreference(mAssistDoubleTapAction,
                    VKeyHelper.getDoubleTapOnAssistBehavior(getActivity(), false),
                    Settings.System.VKEY_ASSIST_DOUBLE_TAP_ACTION);


            // App switch key longpress
            setupOrUpdatePreference(mAppSwitchLongPressAction,
                    VKeyHelper.getLongPressOnAppSwitchBehavior(getActivity(), false),
                    Settings.System.VKEY_APP_SWITCH_LONG_PRESS_ACTION);

            // App switch key double tap
            setupOrUpdatePreference(mAppSwitchDoubleTapAction,
                    VKeyHelper.getDoubleTapOnAppSwitchBehavior(getActivity(), false),
                    Settings.System.VKEY_APP_SWITCH_DOUBLE_TAP_ACTION);

        boolean enableVirtualRebind = Settings.System.getInt(getContentResolver(),
                Settings.System.VIRTUAL_KEY_REBINDING, 0) == 1;
        mEnableCustomBindings = (SwitchPreference) findPreference(KEYS_ENABLE_CUSTOM);
        mEnableCustomBindings.setChecked(enableVirtualRebind);
        mEnableCustomBindings.setOnPreferenceChangeListener(this);


        mCheckPreferences = true;
        return prefs;
    }

    private void setupOrUpdatePreference(
            Preference preference, String action, String settingsKey) {
        if (preference == null || action == null) {
            return;
        }

        if (action.startsWith("**")) {
            preference.setSummary(getDescription(action));
        } else {
            preference.setSummary(AppHelper.getFriendlyNameForUri(
                    getActivity(), getActivity().getPackageManager(), action));
        }

        preference.setOnPreferenceClickListener(this);
        mKeySettings.put(settingsKey, action);
    }

    private String getDescription(String action) {
        if (sFinalActionDialogArray == null || action == null) {
            return null;
        }
        int i = 0;
        for (String actionValue : sFinalActionDialogArray.values) {
            if (action.equals(actionValue)) {
                return sFinalActionDialogArray.entries[i];
            }
            i++;
        }
        return null;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String settingsKey = null;
        int dialogTitle = 0;
        if (preference == mBackLongPressAction) {
            settingsKey = Settings.System.VKEY_BACK_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_back_long_press_title;
        } else if (preference == mCameraPressAction) {
            settingsKey = Settings.System.VKEY_CAMERA_ACTION;
            dialogTitle = R.string.keys_camera_press_title;
        } else if (preference == mCameraLongPressAction) {
            settingsKey = Settings.System.VKEY_CAMERA_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_camera_long_press_title;
        } else if (preference == mCameraDoubleTapAction) {
            settingsKey = Settings.System.VKEY_CAMERA_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_camera_double_tap_title;
        } else if (preference == mHomeDoubleTapAction) {
            settingsKey = Settings.System.VKEY_HOME_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_home_double_tap_title;
        } else if (preference == mAssistPressAction) {
            settingsKey = Settings.System.VKEY_ASSIST_ACTION;
            dialogTitle = R.string.keys_assist_press_title;
        } else if (preference == mAssistLongPressAction) {
            settingsKey = Settings.System.VKEY_ASSIST_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_assist_long_press_title;
        } else if (preference == mAssistDoubleTapAction) {
            settingsKey = Settings.System.VKEY_ASSIST_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_assist_double_tap_title;
        } else if (preference == mAppSwitchLongPressAction) {
            settingsKey = Settings.System.VKEY_APP_SWITCH_LONG_PRESS_ACTION;
            dialogTitle = R.string.keys_app_switch_long_press_title;
        } else if (preference == mAppSwitchDoubleTapAction) {
            settingsKey = Settings.System.VKEY_APP_SWITCH_DOUBLE_TAP_ACTION;
            dialogTitle = R.string.keys_app_switch_double_tap_title;
        }

        if (settingsKey != null) {
            showDialogInner(DLG_SHOW_ACTION_DIALOG, settingsKey, dialogTitle);
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (!mCheckPreferences) {
            return false;
        }
        if (preference == mEnableCustomBindings) {
            boolean value = (Boolean) newValue;
            Settings.System.putInt(getContentResolver(), Settings.System.VIRTUAL_KEY_REBINDING,
                    value ? 1 : 0);
            return true;
        }
        return false;
    }

    private void resetToDefault() {
        for (String settingsKey : mKeySettings.keySet()) {
            if (settingsKey != null) {
                Settings.System.putString(getActivity().getContentResolver(),
                settingsKey, null);
            }
        }
        Settings.System.putInt(getContentResolver(),
                Settings.System.VIRTUAL_KEY_REBINDING, 1);
        reloadSettings();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void shortcutPicked(String action,
                String description, Bitmap b, boolean isApplication) {
        if (mPendingSettingsKey == null || action == null) {
            return;
        }
        Settings.System.putString(getContentResolver(), mPendingSettingsKey, action);
        reloadSettings();
        mPendingSettingsKey = null;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ShortcutPickerHelper.REQUEST_PICK_SHORTCUT
                    || requestCode == ShortcutPickerHelper.REQUEST_PICK_APPLICATION
                    || requestCode == ShortcutPickerHelper.REQUEST_CREATE_SHORTCUT) {
                mPicker.onActivityResult(requestCode, resultCode, data);

            }
        } else {
            mPendingSettingsKey = null;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_RESET:
                    showDialogInner(DLG_RESET_TO_DEFAULT, null, 0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_RESET, 0, R.string.shortcut_action_reset)
                .setIcon(R.drawable.ic_settings_backup) // use the backup icon
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
    }

    private void showDialogInner(int id, String settingsKey, int dialogTitle) {
        DialogFragment newFragment =
                MyAlertDialogFragment.newInstance(id, settingsKey, dialogTitle);
        newFragment.setTargetFragment(this, 0);
        newFragment.show(getFragmentManager(), "dialog " + id);
    }

    public static class MyAlertDialogFragment extends DialogFragment {

        public static MyAlertDialogFragment newInstance(
                int id, String settingsKey, int dialogTitle) {
            MyAlertDialogFragment frag = new MyAlertDialogFragment();
            Bundle args = new Bundle();
            args.putInt("id", id);
            args.putString("settingsKey", settingsKey);
            args.putInt("dialogTitle", dialogTitle);
            frag.setArguments(args);
            return frag;
        }

        VirtualKeysSettings getOwner() {
            return (VirtualKeysSettings) getTargetFragment();
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            int id = getArguments().getInt("id");
            final String settingsKey = getArguments().getString("settingsKey");
            int dialogTitle = getArguments().getInt("dialogTitle");
            switch (id) {
                case DLG_SHOW_ACTION_DIALOG:
                    if (sFinalActionDialogArray == null) {
                        return null;
                    }
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(dialogTitle)
                    .setNegativeButton(R.string.cancel, null)
                    .setItems(getOwner().sFinalActionDialogArray.entries,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int item) {
                            if (getOwner().sFinalActionDialogArray.values[item]
                                    .equals(ActionConstants.ACTION_APP)) {
                                if (getOwner().mPicker != null) {
                                    getOwner().mPendingSettingsKey = settingsKey;
                                    getOwner().mPicker.pickShortcut(getOwner().getId());
                                }
                            } else {
                                Settings.System.putString(getActivity().getContentResolver(),
                                        settingsKey,
                                        getOwner().sFinalActionDialogArray.values[item]);
                                getOwner().reloadSettings();
                            }
                        }
                    })
                    .create();
                case DLG_RESET_TO_DEFAULT:
                    return new AlertDialog.Builder(getActivity())
                    .setTitle(R.string.shortcut_action_reset)
                    .setMessage(R.string.reset_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.dlg_ok,
                        new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            getOwner().resetToDefault();
                        }
                    })
                    .create();
            }
            throw new IllegalArgumentException("unknown id " + id);
        }

        @Override
        public void onCancel(DialogInterface dialog) {
        }
    }

}
