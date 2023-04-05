-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}

# Add any project-specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in the build.gradle file.

# Keep classes that are referenced in the AndroidManifest.xml file.
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.view.View
-keep public class * extends androidx.fragment.app.Fragment
-keep public class * extends androidx.appcompat.app.AppCompatActivity
-keep public class * extends androidx.core.app.NotificationCompat
-keep public class * extends androidx.work.ListenableWorker

# Keep the R classes.
-keepclassmembers class **.R$* {
  public static <fields>;
}

# Keep the BuildConfig class.
-keepclassmembers class **.BuildConfig {
  public static <fields>;
}

# Keep annotations.
-keepattributes *Annotation*

# Keep all public and protected methods that are not overridden or implemented.
-keepclassmembers class * {
  public protected <methods>;
}

# Remove unused classes, fields, and methods.
-assumenosideeffects class android.util.Log {
  public static *** d(...);
  public static *** v(...);
  public static *** i(...);
  public static *** w(...);
  public static *** e(...);
}

# Optimize the APK.
-optimizations !code/simplification/arithmetic
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-dontpreverify
-verbose

# Obfuscate the code.
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable
-keep class com.squareup.okhttp.** { *; }
-keep class org.slf4j.** { *; }

-dontwarn com.squareup.okhttp.**
-dontwarn org.slf4j.**

-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class io.grpc.okhttp.** { *; }
-keep class org.slf4j.** { *; }
-keepattributes Signature
# Uncomment for DexGuard only
#-keep resourcexmlelements manifest/application/meta-data@value=GlideModule