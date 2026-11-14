# =====================================================================
# SecureLock AI - Release R8 / ProGuard Configuration
# Targets Android 8.0 (API 26) - Android 16 (API 36)
# =====================================================================

# --- 1. Core Security Obfuscation & Repackaging ---
# Flatten all obfuscated classes (including com.securelock.ai.security.* and
# AppLockSessionManager) into package 'o' to conceal internal architecture.
# Intentionally do NOT add -keep rules for com.securelock.ai.security.**.
-allowaccessmodification
-repackageclasses 'o'

# Mask original Kotlin source file names in stack traces
-renamesourcefileattribute SourceFile
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod, SourceFile, LineNumberTable

# Strip debug/info/verbose logging & Kotlin parameter-name strings from DEX
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
}

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {
    public static void checkExpressionValueIsNotNull(java.lang.Object, java.lang.String);
    public static void checkNotNullExpressionValue(java.lang.Object, java.lang.String);
    public static void checkParameterIsNotNull(java.lang.Object, java.lang.String);
    public static void checkNotNullParameter(java.lang.Object, java.lang.String);
}

# --- 2. Kotlinx Serialization (Encrypted Settings, Vault, & Backups) ---
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.securelock.ai.data.**$$serializer { *; }
-keepclassmembers class com.securelock.ai.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.securelock.ai.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers enum com.securelock.ai.data.* {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# --- 3. AndroidX BiometricPrompt Compatibility ---
-keep class androidx.biometric.BiometricFragment { *; }
-keep class androidx.biometric.FingerprintDialogFragment { *; }
-keep class androidx.biometric.BiometricViewModel { <init>(...); }
-keepclassmembers class * extends androidx.biometric.BiometricPrompt$AuthenticationCallback {
    public void onAuthenticationError(int, java.lang.CharSequence);
    public void onAuthenticationSucceeded(androidx.biometric.BiometricPrompt$AuthenticationResult);
    public void onAuthenticationFailed();
}
-dontwarn androidx.biometric.**

# --- 4. CameraX Compatibility (camera-core, camera2, lifecycle, view) ---
-keep class androidx.camera.camera2.Camera2Config$DefaultProvider { *; }
-keep class * implements androidx.camera.core.CameraXConfig$Provider {
    public <init>();
    public androidx.camera.core.CameraXConfig getCameraXConfig();
}
-keep class androidx.camera.core.impl.MetadataHolderService { *; }
-keep class androidx.camera.view.PreviewView {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}
-keepclassmembers class * extends androidx.camera.core.ExtendableBuilder { *; }
-dontwarn androidx.camera.**
-dontwarn com.google.common.util.concurrent.ListenableFuture

# --- 5. Lifecycle & AndroidX WebKit Safety ---
-keepclassmembers class * implements androidx.lifecycle.DefaultLifecycleObserver {
    public void onStart(androidx.lifecycle.LifecycleOwner);
    public void onStop(androidx.lifecycle.LifecycleOwner);
}
-keepclassmembers class * extends android.webkit.WebViewClient { *; }
-keepclassmembers class * extends android.webkit.WebChromeClient { *; }
-dontwarn androidx.webkit.**
