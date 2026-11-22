# Kanso ProGuard rules

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.kanso.core.model.**$$serializer { *; }
-keepclassmembers class com.kanso.core.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.kanso.core.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Glance ActionCallbacks & AppWidgetReceiver
-keep class * extends androidx.glance.appwidget.action.ActionCallback { *; }
-keep class com.kanso.app.widget.** { *; }

# WorkManager Workers & Quick Settings Tile
-keep class com.kanso.app.notifications.DailyReminderWorker { *; }
-keep class com.kanso.app.tile.KansoQuickCaptureTileService { *; }
