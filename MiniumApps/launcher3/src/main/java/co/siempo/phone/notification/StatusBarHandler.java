package co.siempo.phone.notification;

import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import co.siempo.phone.R;
import co.siempo.phone.app.Launcher3App;
import co.siempo.phone.pause.PauseActivity;
import co.siempo.phone.service.SiempoAccessibilityService;
import minium.co.core.config.Config;
import minium.co.core.log.Tracer;
import minium.co.core.ui.CoreActivity;

/**
 * Created by itc on 02/03/17.
 */

public class StatusBarHandler {


    private String TAG = "StatusBarHandler";
    private boolean isActive = false;


    public boolean isActive() {
        return isActive;
    }


    public static boolean isNotificationTrayVisible = false;
    private Context mContext;
    private static customViewGroup blockingView;
    private int status_bar_height = 0;
    private static List<customViewGroup> blockingViewCollection = new ArrayList<>();

    public StatusBarHandler(Context context) {
        mContext = context;
        blockingView = new customViewGroup(context);
    }

    public void requestStatusBarCustomization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(mContext)) {
                Toast.makeText(mContext, R.string.msg_overlay_settings, Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + mContext.getPackageName()));
                mContext.startActivity(intent);
            } else {
                preventStatusBarExpansion();
                if (!isAccessibilitySettingsOn(mContext)) {
                    Toast.makeText(mContext, R.string.msg_accessibility2, Toast.LENGTH_SHORT).show();
                    Intent intent1 = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    mContext.startActivity(intent1);
                }
            }
        } else {
            preventStatusBarExpansion();
            if (!isAccessibilitySettingsOn(mContext)) {
                Toast.makeText(mContext, R.string.msg_accessibility2, Toast.LENGTH_SHORT).show();
                Intent intent1 = new Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS);
                mContext.startActivity(intent1);
            }
        }
    }

    private static boolean isAccessibilitySettingsOn(Context mContext) {
        int accessibilityEnabled = 0;
        final String service = mContext.getPackageName() + "/" + SiempoAccessibilityService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    mContext.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        TextUtils.SimpleStringSplitter mStringColonSplitter = new TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    mContext.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private void preventStatusBarExpansion() {
        try {
            if (blockingViewCollection != null && blockingViewCollection.size() == 0) {
                System.out.println(TAG + " preventStatusBarExpansion");
                WindowManager manager = ((WindowManager) mContext.getApplicationContext()
                        .getSystemService(Context.WINDOW_SERVICE));

                Activity activity = (Activity) mContext;
                WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
                localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
                localLayoutParams.gravity = Gravity.TOP;
                localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |

                        // this is to enable the notification to recieve touch events
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |

                        // Draws over status bar
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

                localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
                int resId = activity.getResources().getIdentifier("status_bar_height", "dimen", "android");
                int result = 0;
                if (resId > 0) {
                    result = activity.getResources().getDimensionPixelSize(resId);
                }

                status_bar_height = result;

                localLayoutParams.height = result;

                localLayoutParams.format = PixelFormat.TRANSPARENT;
                manager.addView(blockingView, localLayoutParams);
                blockingViewCollection.add(blockingView);
                isActive = true;

            } else {
                Log.d(TAG, "Blocking View already added...");
            }
        } catch (Exception e) {
            Tracer.e(e, e.getMessage());
        }
    }

    private class customViewGroup extends ViewGroup {


        public customViewGroup(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
        }


        @Override
        public boolean onTouchEvent(MotionEvent event) {

            if (event.getY() > status_bar_height) {
                if (mContext != null && mContext instanceof PauseActivity) {
                    PauseActivity pauseActivity = (PauseActivity) mContext;
                    if (pauseActivity.launcherPrefs != null && pauseActivity.launcherPrefs.isPauseActive().get()) {
                        Log.d(TAG, "Pause mode is active.");
                    } else {
                        showSiempoNotification(event);
                    }
                } else {
                    showSiempoNotification(event);
                }
            }


            return super.onTouchEvent(event);
        }

    }

    /*
    Added so that when not in launcher it allow status bar to default state
     */


    public void restoreStatusBarExpansion() {

        System.out.println(TAG + " restoreStatusBarExpansion");
        if (blockingView != null)
            System.out.println(TAG + " restoreStatusBarExpansion  token == " + blockingView.getWindowToken());
        if (blockingView != null)
            if (blockingView.getWindowToken() != null) {
                WindowManager manager = ((WindowManager) mContext.getApplicationContext().getSystemService(Context.WINDOW_SERVICE));
                manager.removeView(blockingView);
                isActive = false;
                System.out.println(TAG + " restored StatusBar Expansion total used blocked view == " + blockingViewCollection.size());

            } else {
                System.out.println(TAG + " restoreStatusBarExpansion got null ");
            }

        for (customViewGroup b : blockingViewCollection
                ) {

            if (b.getWindowToken() != null) {
                WindowManager manager = ((WindowManager) mContext.getApplicationContext().getSystemService(Context.WINDOW_SERVICE));
                manager.removeView(b);
                isActive = false;
                System.out.println(TAG + "  StatusBar total used blocked view == " + blockingViewCollection.size());

            } else {
                System.out.println(TAG + " blockingView got null ");
            }

            b.destroyDrawingCache();
            blockingViewCollection.remove(b);
            Log.d(TAG, "RESTORE NATIVE NOTIFICATION .......... ");


        }
    }

    private void showSiempoNotification(MotionEvent event) {
        if (Launcher3App.getInstance().siempoBarLaunch && !isNotificationTrayVisible) {
            System.out.println(TAG + " y position on Touch on notification tray " + event.getY() + "status_bar_height " + status_bar_height);
            //Intent intent = new Intent(mContext, NotificationFragment.class);
            //mContext. startActivity(intent);
//                    ((CoreActivity) mContext).loadChildFragment(NotificationFragment_.builder().build(), R.id.mainView);
//                    ((CoreActivity) mContext).getFragmentManager().beginTransaction().setCustomAnimations(R.anim.slide_in_down, R.anim.slide_out_down);
            try {
                Config.isNotificationAlive = true;
                FragmentTransaction ft = ((CoreActivity) mContext).getFragmentManager().beginTransaction();
                ft.setCustomAnimations(R.animator.push_down_in_no_alpha, R.animator.push_down_out_no_alpha);
                ft.replace(R.id.mainView, NotificationFragment_.builder().build());
                ft.commitAllowingStateLoss();
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(new Intent("IsNotificationVisible").putExtra("IsNotificationVisible", true));
            } catch (Exception e) {
                e.printStackTrace();
            }
            isNotificationTrayVisible = true;
        }
    }
}