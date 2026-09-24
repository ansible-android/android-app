package org.ansible.messenger;

import android.content.Context;

import io.sentry.Sentry;
import io.sentry.android.core.SentryAndroid;
import io.sentry.protocol.User;

/**
 * Отчёты о падениях. Уезжают на НАШ приёмник и никуда больше.
 *
 * Библиотека называется Sentry, но сервер — наш GlitchTip в кластере
 * (glitchtip.ansible.su). GlitchTip говорит по протоколу Sentry, поэтому
 * официальный SDK работает с ним без единой правки, а данные не покидают
 * наш контур. Ни sentry.io, ни какой-либо другой чужой приёмник здесь не
 * участвует — адрес задан жёстко ниже.
 *
 * 🚨 Почему так придирчиво к тому, что уходит наружу: предыдущая телеметрия
 * (Microsoft App Center и Firebase Crashlytics) слала uid и @username
 * пользователя вместе с отпечатком устройства, причём Crashlytics писал в
 * ЧУЖОЙ проект. Её вырезали целиком. Возвращать сбор отчётов можно только
 * так, чтобы аккаунт по отчёту не опознавался.
 */
public class CrashReporter {

    // Ключ внутри DSN публичный по устройству протокола: это адрес проекта,
    // куда складывать отчёты, а не доступ к уже собранным данным. Он и должен
    // лежать в клиенте.
    private static final String DSN = "https://09d3e02092e540a4b2cc85a02f56ff33@glitchtip.ansible.su/1";

    private static volatile boolean inited;

    /**
     * Вызывается один раз при старте процесса. Автозапуск SDK через
     * ContentProvider отключён в манифесте намеренно: иначе первые события
     * ушли бы до того, как выставлены настройки приватности ниже.
     */
    public static void init(Context context) {
        if (inited || context == null) {
            return;
        }
        try {
            SentryAndroid.init(context, options -> {
                options.setDsn(DSN);
                options.setRelease(BuildVars.BUILD_VERSION_STRING);
                options.setEnvironment(environment());

                // Ничего, что помогает опознать человека.
                options.setSendDefaultPii(false);
                // Скриншот и дерево вью на момент падения — это содержимое
                // переписки на экране. Выключено явно, а не по умолчанию.
                options.setAttachScreenshot(false);
                options.setAttachViewHierarchy(false);
                // «Хлебные крошки» по нажатиям записывают подписи и id
                // элементов, а там бывают названия чатов и имена.
                options.setEnableUserInteractionBreadcrumbs(false);
                options.setEnableUserInteractionTracing(false);

                options.setBeforeSend((event, hint) -> {
                    // Подстраховка на будущее: даже если кто-то позовёт
                    // Sentry.setUser() с данными аккаунта, наружу они не уйдут.
                    // Случайный идентификатор установки оставляем — он не
                    // связан с аккаунтом, но позволяет понять, сколько человек
                    // поймали одно и то же падение.
                    User user = event.getUser();
                    if (user != null) {
                        user.setUsername(null);
                        user.setEmail(null);
                        user.setIpAddress(null);
                        user.setData(null);
                    }
                    event.setServerName(null);
                    return event;
                });
            });
            inited = true;
        } catch (Throwable e) {
            // Сбор отчётов не имеет права ронять приложение. FileLog здесь ещё
            // может быть не готов, поэтому молча.
            inited = false;
        }
    }

    /**
     * Отправить исключение, которое приложение поймало и пережило. Падения с
     * необработанным исключением SDK забирает сам.
     */
    public static void capture(Throwable e) {
        if (!inited || e == null) {
            return;
        }
        try {
            Sentry.captureException(e);
        } catch (Throwable ignore) {

        }
    }

    private static String environment() {
        if (BuildVars.DEBUG_PRIVATE_VERSION) {
            return "private";
        }
        if (BuildVars.DEBUG_VERSION) {
            return "debug";
        }
        return "release";
    }
}
