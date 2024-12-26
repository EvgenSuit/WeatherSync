-keep public class * extends java.lang.Exception

# The first line ensures that source file names are hidden (will be named "SourceFile" in stack traces), the second instructs ProGuard to include the line numbers.
-renamesourcefileattribute SourceFile
-keepattributes SourceFile,LineNumberTable

-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener
-dontwarn android.media.LoudnessCodecController