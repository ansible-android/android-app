/*
 * This is the source code of Telegram for Android v. 7.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2020.
 */

package org.ansible.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;

import com.android.billingclient.api.ProductDetails;

import java.util.Objects;

public class BuildVars {

    public static boolean DEBUG_VERSION = BuildConfig.DEBUG_VERSION;
    public static boolean LOGS_ENABLED = BuildConfig.DEBUG_VERSION;
    public static boolean DEBUG_PRIVATE_VERSION = BuildConfig.DEBUG_PRIVATE_VERSION;
    public static boolean USE_CLOUD_STRINGS = true;
    public static boolean CHECK_UPDATES = true;
    public static boolean NO_SCOPED_STORAGE = Build.VERSION.SDK_INT <= 29;
    public static String BUILD_VERSION_STRING = BuildConfig.BUILD_VERSION_STRING;

    // Ansible Android. Постоянная пара, выдана порталом my.ansible.su 2026-08-14
    // (реестр: workflow/APP_CREDENTIALS.md). Заменила временную 5450757, которая
    // была выписана на my.telegram.org 2026-05-11 «до продакшена» — то есть наш
    // клиент представлялся приложением из ЧУЖОГО реестра, и наш сервер не мог
    // ни узнать его, ни забанить, ни придушить по api_id.
    //
    // Константа в исходнике — это предписанный upstream способ: штатного
    // параметра сборки у Android нет вовсе (local.properties обслуживает только
    // APP_CENTER_HASH и BETA_URL), а README upstream прямо требует от форков
    // «obtain your own api_id» и заменить именно эту константу. Пара секретом
    // не является: upstream держит здесь же свои APP_ID=4 / 014b35b6…
    public static int APP_ID = 21000004;
    public static String APP_HASH = "ff442affb65f4604012d98ab1338e34c";

    // TODO(release): SafetyNet key - our own, see workflow/RELEASE_BLOCKERS_ANDROID.md
    // Was Telegram's own key. Empty disables the check (LoginActivity:3083).
    public static String SAFETYNET_KEY = "";

    public static String PLAYSTORE_APP_URL = "https://ansible.su/android";

    // TODO(release): Huawei AppGallery listing - ours, or drop the Huawei module, see workflow/RELEASE_BLOCKERS_ANDROID.md
    // Was Telegram's listing C101184875.
    public static String HUAWEI_STORE_URL = "";

    // TODO(release): Google OAuth client id from OUR Firebase project, see workflow/RELEASE_BLOCKERS_ANDROID.md
    // Was Telegram's client of project 760348033671. The server may override it
    // via appConfig googleAuthClientId (MessagesController:1825).
    public static String GOOGLE_AUTH_CLIENT_ID = "";

    // TODO(release): Huawei app id - ours, see workflow/RELEASE_BLOCKERS_ANDROID.md
    public static String HUAWEI_APP_ID = "";

    // You can use this flag to disable Google Play Billing (If you're making fork and want it to be in Google Play)
    public static boolean IS_BILLING_UNAVAILABLE = false;

    // works only on official app ids, disable on your forks
    public static boolean SUPPORTS_PASSKEYS = true;

    static {
        if (ApplicationLoader.applicationContext != null) {
            SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences("systemConfig", Context.MODE_PRIVATE);
            LOGS_ENABLED = DEBUG_VERSION || sharedPreferences.getBoolean("logsEnabled", DEBUG_VERSION);
            if (LOGS_ENABLED) {
                final Thread.UncaughtExceptionHandler pastHandler = Thread.getDefaultUncaughtExceptionHandler();
                Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
                    FileLog.fatal(exception, false);
                    if (pastHandler != null) {
                        pastHandler.uncaughtException(thread, exception);
                    }
                });
            }
        }
    }

    public static boolean useInvoiceBilling() {
        return BillingController.billingClientEmpty || DEBUG_VERSION && false || ApplicationLoader.isStandaloneBuild() || isBetaApp() && false || isHuaweiStoreApp() || hasDirectCurrency();
    }

    private static boolean hasDirectCurrency() {
        if (!BillingController.getInstance().isReady() || BillingController.PREMIUM_PRODUCT_DETAILS == null) {
            return false;
        }
        for (ProductDetails.SubscriptionOfferDetails offerDetails : BillingController.PREMIUM_PRODUCT_DETAILS.getSubscriptionOfferDetails()) {
            for (ProductDetails.PricingPhase phase : offerDetails.getPricingPhases().getPricingPhaseList()) {
                for (String cur : MessagesController.getInstance(UserConfig.selectedAccount).directPaymentsCurrency) {
                    if (Objects.equals(phase.getPriceCurrencyCode(), cur)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static Boolean betaApp;
    public static boolean isBetaApp() {
        if (betaApp == null) {
            betaApp = ApplicationLoader.applicationContext != null && "su.ansible.messenger.beta".equals(ApplicationLoader.applicationContext.getPackageName());
        }
        return betaApp;
    }


    public static boolean isHuaweiStoreApp() {
        return ApplicationLoader.isHuaweiStoreBuild();
    }

    // TODO(release): SMS Retriever hashes for OUR signing certificate, see workflow/RELEASE_BLOCKERS_ANDROID.md
    // The hash is derived from applicationId + signing cert fingerprint; both
    // changed, so Telegram's old values are dead weight. Empty just means the
    // code is not auto-filled from the SMS.
    public static String getSmsHash() {
        return "";
    }
}
