# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- 1. 기본 설정 및 디버그 스택트레이스 보존 ---
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod
-keepattributes *Annotation*,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# --- 2. 프로젝트 내부 코드 보존 ---
-keep class org.example.myapp.auth.network.** { *; }
-keep class org.example.myapp.auth.viewmodel.** { *; }
-keep class org.example.myapp.util.** { *; }

# --- 3. Kotlin Serialization ---
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclassmembers class * {
    @kotlinx.serialization.SerialName <fields>;
}
-dontnote kotlinx.serialization.SerializationKt

# --- 4. Koin (Dependency Injection) ---
-keep class org.koin.** { *; }
-keep interface org.koin.** { *; }
-dontwarn org.koin.**

# --- 5. Coil 3 ---
-keep class coil3.** { *; }
-dontwarn coil3.**

# --- 6. Media3 / ExoPlayer ---
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# --- 7. Ktor & 네트워크 통신 ---
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**
-keepattributes Signature, Exceptions
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**
-keepclassmembernames interface * {
    @retrofit2.http.* <methods>;
}
-if interface * { @retrofit2.http.* public *** *(...); }
-keep,allowoptimization,allowshrinking,allowobfuscation class <3>
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.slf4j.**
-dontwarn javax.annotation.**
-dontwarn org.bouncycastle.jsse.**

# --- 8. Kotlin Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# --- 9. Kakao SDK ---
-keep class com.kakao.sdk.** { *; }
-keep interface com.kakao.sdk.** { *; }
-keep class com.kakao.sdk.**.model.* { <fields>; }
-dontwarn com.kakao.sdk.**

# --- 10. Android 컴포넌트 생명주기 ---
-keep class * extends androidx.lifecycle.ViewModel {
    public <init>(...);
}
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Service { *; }