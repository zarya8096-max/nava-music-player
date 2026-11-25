# Nava Music Player R8 / ProGuard rules
-keep class com.nava.music.** { *; }

# Keep Media3 & ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep Coil & Palette
-keep class coil.** { *; }
-keep class androidx.palette.** { *; }
