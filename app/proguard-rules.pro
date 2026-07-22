# Room / Media3 / Hilt zachowują większość reguł automatycznie.
# kotlinx.serialization — zachowaj serializery DTO Xtream.
-keepclassmembers class **$$serializer { *; }
-keep,includedescriptorclasses class com.mibox.iptv.data.remote.xtream.** { *; }

# Media3 renderery/dekodery.
-keep class androidx.media3.** { *; }
