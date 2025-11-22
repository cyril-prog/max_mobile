# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Compose classes
-keep class androidx.compose.** { *; }

# Keep data classes used with Gson/Moshi (if you add JSON serialization later)
-keep class com.max.aiassistant.model.** { *; }
