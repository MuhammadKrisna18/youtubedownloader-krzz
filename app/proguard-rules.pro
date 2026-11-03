-keep class com.yausername.** { *; }
-keep interface com.yausername.** { *; }
-dontwarn com.yausername.**

-keep class com.fasterxml.jackson.** { *; }
-keep interface com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.**

-keep class org.apache.commons.io.** { *; }
-dontwarn org.apache.commons.io.**

-keep class org.tukaani.xz.** { *; }
-dontwarn org.tukaani.xz.**

-keep class okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keep class com.personal.ytmusicdownloader.model.** { *; }
-keep class com.personal.ytmusicdownloader.data.** { *; }
