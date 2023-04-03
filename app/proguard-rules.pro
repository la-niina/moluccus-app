-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
 <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
  **[] $VALUES;
  public *;
}
-keep class com.bumptech.glide.load.data.ParcelFileDescriptorRewinder$InternalRewinder {
  *** rewind();
}
# Uncomment for DexGuard only
#-keepresourcexmlelements manifest/application/meta-data@value=GlideModule
#-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
#-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