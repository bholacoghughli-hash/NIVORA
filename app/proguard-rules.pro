# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep WebView JavaScript interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep Room Database entities and schemas
-keep class com.nivora.browser.database.entities.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.paging.**

# Keep Jetpack Compose & Kotlin coroutines
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-dontwarn sun.misc.Unsafe
