# ==============================================================================
# Proguard / R8 Configuration for Keuangan App
# ==============================================================================

# Preserve annotations and generic signatures
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod,Exceptions

# ------------------------------------------------------------------------------
# Gson & Data Models
# ------------------------------------------------------------------------------
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.keuangan.app.data.model.** { *; }
-keepclassmembers class com.keuangan.app.data.model.** { *; }

# ------------------------------------------------------------------------------
# Retrofit 2
# ------------------------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep interface com.keuangan.app.data.api.** { *; }

# ------------------------------------------------------------------------------
# OkHttp 3
# ------------------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ------------------------------------------------------------------------------
# Kotlin Coroutines
# ------------------------------------------------------------------------------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
