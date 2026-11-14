-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'o'

# Preserve Kotlinx Serialization metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keep,includedescriptorclasses class com.securelock.ai.data.**$$serializer { *; }
-keepclassmembers class com.securelock.ai.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.securelock.ai.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Remove debug logging in release builds for smaller APK & security
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
}
