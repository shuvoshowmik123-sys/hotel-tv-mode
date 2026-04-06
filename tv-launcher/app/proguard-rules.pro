# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified in the
# default ProGuard configuration shipped with the Android Gradle plugin.

# Keep Room schema
-keep class com.hotelvision.launcher.data.db.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep data classes used by Retrofit/Serialization
-keep class com.hotelvision.launcher.data.api.** { *; }

# Coil
-keep class coil3.** { *; }
-dontwarn coil3.PlatformContext

# Kotlin Serialization
-keepattributes *Annotation*
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class ** { @kotlinx.serialization.Serializable <fields>; }
