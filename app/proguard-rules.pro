# Kanso ProGuard / R8 rules

# Preserve line numbers for readable crash stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Kotlinx Serialization across all Kanso modules (:core:model, :core:backup, :app)
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep,includedescriptorclasses class com.kanso.**$$serializer { *; }
-keepclassmembers class com.kanso.** {
    *** Companion;
}
-keepclasseswithmembers class com.kanso.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Room Database & Entities
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }

# Hilt EntryPoints used by Glance, WorkManager, and BroadcastReceivers
-keep @dagger.hilt.EntryPoint interface * { *; }

# Glance ActionCallbacks & AppWidgetReceiver
-keep class * extends androidx.glance.appwidget.action.ActionCallback { *; }
-keep class * extends androidx.glance.appwidget.GlanceAppWidgetReceiver { *; }
-keep class com.kanso.app.widget.** { *; }

# WorkManager CoroutineWorkers, BroadcastReceivers & Quick Settings Tile
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class com.kanso.app.notifications.DailyReminderWorker { *; }
-keep class com.kanso.app.notifications.NotificationActionReceiver { *; }
-keep class com.kanso.app.sync.DriveSyncWorker { *; }
-keep class com.kanso.app.tile.KansoQuickCaptureTileService { *; }

# Optional compile-time annotations referenced by transitive libraries (ZXing/Guava/CameraX)
-dontwarn com.google.errorprone.annotations.**
-dontwarn javax.annotation.**
