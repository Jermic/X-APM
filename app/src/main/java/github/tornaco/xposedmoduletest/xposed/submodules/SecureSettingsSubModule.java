package github.tornaco.xposedmoduletest.xposed.submodules;

import android.os.Binder;
import android.provider.Settings;
import android.util.Log;

import java.util.Set;

import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import github.tornaco.xposedmoduletest.BuildConfig;
import github.tornaco.xposedmoduletest.xposed.app.XAshmanManager;
import github.tornaco.xposedmoduletest.xposed.util.XposedLog;

/**
 * Created by guohao4 on 2017/10/31.
 * Email: Tornaco@163.com
 */

// Hook hookGetStringForUser settings.
class SecureSettingsSubModule extends IntentFirewallAndroidSubModule {

    @Override
    public void initZygote(IXposedHookZygoteInit.StartupParam startupParam) {
        super.initZygote(startupParam);
        hookGetStringForUser();
    }

    private void hookGetStringForUser() {
        XposedLog.verbose("hookGetStringForUser...");
        try {
            Class sceclass = XposedHelpers.findClass("android.provider.Settings$Secure",
                    null);
            Set unHooks = XposedBridge.hookAllMethods(sceclass, "getStringForUser",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            super.beforeHookedMethod(param);
                            String name = String.valueOf(param.args[1]);
                            if (BuildConfig.DEBUG && Settings.Secure.ANDROID_ID.equals(name)) {
                                // Use of defined id.
                                XAshmanManager ash = XAshmanManager.get();
                                if (ash.isServiceAvailable()) {
                                    int callingUid = Binder.getCallingUid();
                                    boolean priv = ash.isUidInPrivacyList(callingUid);
                                    if (BuildConfig.DEBUG) {
                                        Log.d(XposedLog.TAG_DANGER,
                                                "getStringForUser, uid: " + callingUid + ", hook: " + priv);
                                    }
                                    if (priv) {
                                        String androidId = ash.getUserDefinedAndroidId();
                                        if (androidId != null) {
                                            Log.d(XposedLog.TAG_DANGER, "Using user defined androidId!!! for: " + callingUid);
                                            param.setResult(androidId);
                                        }
                                    }
                                }
                            }
                        }
                    });
            XposedLog.verbose("hookGetStringForUser OK:" + unHooks);
            setStatus(unhooksToStatus(unHooks));
        } catch (Exception e) {
            XposedLog.verbose("Fail hookGetStringForUser: " + Log.getStackTraceString(e));
            setStatus(SubModuleStatus.ERROR);
            setErrorMessage(Log.getStackTraceString(e));
        }
    }
}
