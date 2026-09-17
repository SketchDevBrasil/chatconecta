# Regras iniciais do Gênesis Code
-keepattributes *Annotation*,Signature,InnerClasses
-keepclassmembers class * {
    public void *(android.view.View);
}
-keep class * extends android.app.Activity { *; }
-keep class * extends android.app.Application { *; }
-dontwarn android.support.**
