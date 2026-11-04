# Proguard rules for consumer projects importing ScratchCard library
-keep class com.apticstudio.scratchcard.ScratchCardKt { *; }
-keep class com.apticstudio.scratchcard.ScratchCardState {
    public <methods>;
    public <fields>;
}
