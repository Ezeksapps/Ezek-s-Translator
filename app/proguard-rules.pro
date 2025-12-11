# ========== FIX KTOR ANDROID ISSUES ==========
# Suppress the Ktor debug detection that uses missing Java management classes
-dontwarn java.lang.management.**
-dontwarn sun.management.**

# Keep basic Ktor functionality but remove problematic parts
-keep class io.ktor.util.debug.** { *; }

# Additional Ktor rules to prevent missing class issues
-dontwarn org.slf4j.**
-dontwarn kotlin.contracts.**
-dontwarn kotlin.coroutines.jvm.internal.**

# Keep core Ktor classes
-keep class io.ktor.client.** { *; }
-keep class io.ktor.network.** { *; }
-keep class io.ktor.utils.** { *; }

# ========== EXISTING RULES ==========
# Keep JNI classes and methods
-keep class com.ezeksapps.ezeksapp.jni.** { *; }

# Keep DetectionResult class and its constructor
-keep class com.ezeksapps.ezeksapp.jni.DetectionResult {
    public <init>(java.lang.String, boolean, int);
    *;
}

# Keep all classes with native methods
-keepclasseswithmembers class * {
    native <methods>;
}

# Keep all classes in jni package
-keep class com.ezeksapps.ezeksapp.jni.* { *; }

# ========== NETWORKING RULES ==========
-keep class kotlinx.serialization.** { *; }
-keep class com.ezeksapps.ezeksapp.network.** { *; }
-keep class com.ezeksapps.ezeksapp.model.** { *; }
-keep class * implements io.ktor.client.engine.HttpClientEngine { *; }
-keepclasseswithmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}
-keep class com.ezeksapps.ezeksapp.model.Lang { public *; }
-keep class kotlinx.coroutines.** { *; }
-keep class kotlin.reflect.** { *; }
-keepattributes Signature