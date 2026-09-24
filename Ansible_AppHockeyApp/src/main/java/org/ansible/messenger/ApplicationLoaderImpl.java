package org.ansible.messenger;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.ViewGroup;

import androidx.core.content.FileProvider;


import org.ansible.messenger.regular.BuildConfig;
import org.ansible.asnet.ConnectionsManager;
import org.ansible.asnet.TLRPC;
import org.ansible.ui.Components.AlertsCreator;
import org.ansible.ui.Components.UpdateAppAlertDialog;
import org.ansible.ui.Components.UpdateLayout;
import org.ansible.ui.IUpdateLayout;

import java.io.File;

public class ApplicationLoaderImpl extends ApplicationLoader {
    @Override
    protected String onGetApplicationId() {
        return BuildConfig.APPLICATION_ID;
    }


    private String getVersionName(int code) {
        switch (code) {
            case 0: return "local-debug";
            case 1: return "private";
            case 4: return "public";
            case 5: return "hardcore";
            case 6: return "standalone";
            case 7: return "release";
            default: return "unknown";
        }
    }

    // Телеметрия вырезана: модуль слал uid и @username пользователя вместе с
    // отпечатком устройства в Microsoft App Center и в Firebase Crashlytics,
    // причём Crashlytics писал в проект Телеграма tmessages2. App Center в
    // наших сборках и так был инертен (APP_CENTER_HASH берётся из
    // local.properties, которого в репозитории нет), а Crashlytics работал:
    // хеш ему не нужен. Боевые модули (Ansible_App, Ansible_AppStandalone)
    // держат эти методы пустыми — теперь и бета ведёт себя так же.
    @Override
    protected void startAppCenterInternal(Activity context) {

    }

    @Override
    protected void checkForUpdatesInternal() {
        // Обновления беты раздавал App Center. Мы раздаём сборки релизами
        // GitHub, так что проверять здесь нечего.
    }

    protected void appCenterLogInternal(Throwable e) {
        // Исключения больше никуда не уезжают: они и так пишутся в FileLog.
    }

    protected void logDualCameraInternal(boolean success, boolean vendor) {
//        try {
//            Analytics.trackEvent("dual-camera[" + (Build.MANUFACTURER + " " + Build.DEVICE).toUpperCase() + "]",
//                new EventProperties()
//                    .set("success", success)
//                    .set("vendor", vendor)
//                    .set("product", Build.PRODUCT + "")
//                    .set("model", Build.MODEL)
//            );
//        } catch (Throwable ignore) {
//
//        }
    }

    @Override
    public boolean checkApkInstallPermissions(final Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !ApplicationLoader.applicationContext.getPackageManager().canRequestPackageInstalls()) {
            AlertsCreator.createApkRestrictedDialog(context, null).show();
            return false;
        }
        return true;
    }

    @Override
    public boolean openApkInstall(Activity activity, TLRPC.Document document) {
        boolean exists = false;
        try {
            final String fileName = FileLoader.getAttachFileName(document);
            final File f = FileLoader.getInstance(UserConfig.selectedAccount).getPathToAttach(document, true);
            if (exists = f.exists()) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                if (Build.VERSION.SDK_INT >= 24) {
                    intent.setDataAndType(FileProvider.getUriForFile(activity, ApplicationLoader.getApplicationId() + ".provider", f), "application/vnd.android.package-archive");
                } else {
                    intent.setDataAndType(Uri.fromFile(f), "application/vnd.android.package-archive");
                }
                try {
                    activity.startActivityForResult(intent, 500);
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return exists;
    }


    @Override
    protected boolean isBeta() {
        return true;
    }

    @Override
    public boolean isCustomUpdate() {
        return !TextUtils.isEmpty(org.ansible.messenger.BuildConfig.BETA_URL);
    }

    @Override
    public BetaUpdate getUpdate() {
        if (!isCustomUpdate()) return null;
        return BetaUpdaterController.getInstance().getUpdate();
    }

    @Override
    public void checkUpdate(boolean force, Runnable whenDone) {
        if (!isCustomUpdate()) return;
        BetaUpdaterController.getInstance().checkForUpdate(force, whenDone);
    }

    @Override
    public void downloadUpdate() {
        if (!isCustomUpdate()) return;
        BetaUpdaterController.getInstance().downloadUpdate();
    }

    @Override
    public void cancelDownloadingUpdate() {
        if (!isCustomUpdate()) return;
        BetaUpdaterController.getInstance().cancelDownloadingUpdate();
    }

    @Override
    public boolean isDownloadingUpdate() {
        if (!isCustomUpdate()) return false;
        return BetaUpdaterController.getInstance().isDownloading();
    }

    @Override
    public float getDownloadingUpdateProgress() {
        if (!isCustomUpdate()) return 0;
        return BetaUpdaterController.getInstance().getDownloadingProgress();
    }

    @Override
    public File getDownloadedUpdateFile() {
        if (!isCustomUpdate()) return null;
        return BetaUpdaterController.getInstance().getDownloadedFile();
    }

    @Override
    public IUpdateLayout takeUpdateLayout(Activity activity, ViewGroup sideMenuContainer) {
        if (!isCustomUpdate()) return null;
        return new UpdateLayout(activity, sideMenuContainer);
    }

    @Override
    public boolean showCustomUpdateAppPopup(Context context, BetaUpdate update, int account) {
        try {
            (new UpdateAppAlertDialog(context, update, account)).show();
        } catch (Exception e) {
            FileLog.e(e);
        }
        return true;
    }
}
