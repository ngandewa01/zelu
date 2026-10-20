# The release build shrinks the libraries only. Zelu's own classes are kept as written, so code that finds
# things by name (receivers in the manifest, saved settings, backups) keeps working and crash reports stay readable.
-keep class com.nagesh.zelu.** { *; }
-keepattributes SourceFile,LineNumberTable
