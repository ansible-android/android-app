-keep public class com.google.android.gms.* { public *; }
-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keep @interface androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * { @androidx.annotation.Keep *; }

-keep class org.webrtc.* { *; }
-keep class org.webrtc.audio.* { *; }
-keep class org.webrtc.voiceengine.* { *; }
# org.ansible.messenger: only the names that native code and reflection resolve at runtime.
# Before changing these rules see Ansible/proguard-checklist.md and run Tools/r8_keep_check.py.
# Static JNI exports Java_org_ansible_messenger_<Class>_<method> (Utilities, Intro, MediaController, MrzRecognizer).
-keepclasseswithmembers,includedescriptorclasses class org.ansible.messenger.* {
    native <methods>;
}
# gifvideo.cpp: FindClass + GetMethodID, called from the native reader.
-keep class org.ansible.messenger.AnimatedFileDrawableStream {
    int read(int, int);
    void cancel();
    boolean isFinishedLoadingFile();
    boolean isCanceled();
    java.lang.String getFinishedFilePath();
}
# NotificationBadge.BADGERS holds class literals, instances are made with Class.newInstance().
-keep class org.ansible.messenger.NotificationBadge$* implements org.ansible.messenger.NotificationBadge$Badger {
    <init>();
}
-keep class org.ansible.messenger.camera.* { *; }
-keep class org.ansible.messenger.secretmedia.* { *; }
-keep class org.ansible.messenger.support.* { *; }
-keep class org.ansible.messenger.support.* { *; }
-keep class org.ansible.messenger.time.* { *; }
-keep class org.ansible.messenger.video.* { *; }
-keep class org.ansible.messenger.voip.* { *; }
-keep class org.ansible.SQLite.** { *; }
-keep class org.ansible.asnet.ConnectionsManager { *; }
-keep class org.ansible.asnet.NativeByteBuffer { *; }
-keep class org.ansible.asnet.RequestTimeDelegate { *; }
-keep class org.ansible.asnet.RequestDelegate { *; }
-keep class org.ansible.ui.Stories.recorder.FfmpegAudioWaveformLoader { *; }
-keep class androidx.mediarouter.app.MediaRouteButton { *; }
-keepclassmembers class ** {
    @android.webkit.JavascriptInterface <methods>;
}

-keep class org.ansible.utils.code.highlight.PrismaHighlighter {
    private static native <methods>;
}

# https://developers.google.com/ml-kit/known-issues#android_issues
-keep class com.google.mlkit.nl.languageid.internal.LanguageIdentificationJni { *; }

# WidgetSandbox: JNI_OnLoad uses RegisterNatives; backend methods are called by native workers.
-keep class org.ansible.utils.twidget.WidgetSandboxNative { *; }
# The client's share (filters, gesture arbiter) in the client library: static Java_* exports, resolved by name.
-keepclasseswithmembers,includedescriptorclasses class org.ansible.utils.twidget.WidgetClientNative {
    native <methods>;
}
-keep class org.ansible.utils.twidget.WidgetSandboxBackend { *; }
# The isolated engine process's backend (WidgetEngineService): the same JNI callbacks.
-keep class org.ansible.utils.twidget.WidgetEngineBackend { *; }

# Huawei Services
-keep class com.huawei.hianalytics.**{ *; }
-keep class com.huawei.updatesdk.**{ *; }
-keep class com.huawei.hms.**{ *; }

# Don't warn about checkerframework and Kotlin annotations
-dontwarn org.checkerframework.**
-dontwarn javax.annotation.**

-keep class io.nano.tex.** {*;}

-keep class org.ansible.asnet.** { *; }

# JLatexMath: macro/atom classes are loaded reflectively by Class.forName
-keep class org.scilab.forge.jlatexmath.** { *; }
-keep class ru.noties.jlatexmath.** { *; }
-dontwarn org.scilab.forge.jlatexmath.**

# Use -keep to explicitly keep any other classes shrinking would remove
#-dontoptimize
#-dontobfuscate