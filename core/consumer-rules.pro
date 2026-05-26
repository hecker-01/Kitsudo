# Consumer ProGuard rules for :core
# Room entities and DAOs must not be obfuscated
-keep class dev.heckr.kitsudo.data.local.** { *; }
-keep class dev.heckr.kitsudo.domain.model.** { *; }
