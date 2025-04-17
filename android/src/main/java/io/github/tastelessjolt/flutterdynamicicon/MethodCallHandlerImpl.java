package io.github.tastelessjolt.flutterdynamicicon;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ComponentInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

import static android.content.pm.PackageManager.COMPONENT_ENABLED_STATE_ENABLED;

class MethodCallHandlerImpl implements MethodCallHandler {
  static private String TAG = "Flutter_dynamic_icon";

  Context mContext;

  public MethodCallHandlerImpl(Context context) {
    mContext = context;
  }

  @Override
  public void onMethodCall(MethodCall call, MethodChannel.Result result) {
    if (call.method.equals("mSupportsAlternateIcons")) {
      result.success(true);
    } else if (call.method.equals("mGetAlternateIconName")) {
      result.success(getIcon());
    } else if (call.method.equals("mSetAlternateIconName")) {
      String data = call.argument("iconName");
      if (data == null) {
        resetIcon();
      } else {
        updateIcon(data);
      }
      result.success(null);
    } else if (call.method.equals("mGetApplicationIconBadgeNumber")) {
      result.error("Not supported", "Not supported on Android", null);
    } else if (call.method.equals("mSetApplicationIconBadgeNumber")) {
      result.error("Not supported", "Not supported on Android", null);
    } else {
      result.notImplemented();
    }
  }

  public  String getIcon() {
    String packageName = mContext.getPackageName();
    ActivityInfo[] oldName = getActivities();
    PackageManager pm = mContext.getPackageManager();

    for(ActivityInfo activity: oldName) {
      if (pm.getComponentEnabledSetting(new ComponentName(packageName, activity.name))
              == COMPONENT_ENABLED_STATE_ENABLED) {
        return activity.name;
      }
    }

    return null;
  }

  public void updateIcon(@NonNull String name) {
    String packageName = mContext.getPackageName();
    String className = String.format("%s.%s", packageName, name);
    ActivityInfo[] oldName = getActivities();
    PackageManager pm = mContext.getPackageManager();

    pm.setComponentEnabledSetting(
            new ComponentName(packageName, className),
            COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
    );

    String defaultClassName = oldName[0].name;
    pm.setComponentEnabledSetting(
            new ComponentName(packageName, defaultClassName),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
    );

  }

  public void resetIcon() {
    String packageName = mContext.getPackageName();
    ActivityInfo[] oldName = getActivities();
    String defaultClassName = oldName[0].name;
    PackageManager pm = mContext.getPackageManager();

    pm.setComponentEnabledSetting(
            new ComponentName(packageName, defaultClassName),
            COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
    );

    for(ActivityInfo activity: oldName) {
      if(activity.targetActivity != null) {
        pm.setComponentEnabledSetting(
                new ComponentName(packageName, activity.name),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
        );
      }
    }
  }

  public ActivityInfo[] getActivities() {
    ActivityInfo[] activityInfos;

    PackageManager pm = mContext.getPackageManager();
    String packageName = mContext.getPackageName();

    try {
      PackageInfo info = pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES | PackageManager.GET_DISABLED_COMPONENTS);
      activityInfos = info.activities;

      Log.d(TAG, "Found this configured activities:");
      for(ActivityInfo activityInfo : activityInfos) {
        Log.d(TAG, activityInfo.name);
      }

      return activityInfos;

    } catch (PackageManager.NameNotFoundException e) {
      Log.e(TAG, e.toString());
    }
    return null;
  }
}