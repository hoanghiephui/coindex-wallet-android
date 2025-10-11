-dontwarn java.lang.instrument.ClassFileTransformer
-dontwarn sun.misc.SignalHandler
-dontwarn java.lang.instrument.Instrumentation
-dontwarn sun.misc.Signa
-dontwarn java.beans.ConstructorProperties
-dontwarn java.beans.Transient
-dontwarn javax.naming.NamingEnumeration
-dontwarn javax.naming.NamingException
-dontwarn javax.naming.directory.Attribute
-dontwarn javax.naming.directory.Attributes
-dontwarn javax.naming.directory.DirContext
-dontwarn javax.naming.directory.InitialDirContext
-dontwarn org.slf4j.impl.StaticLoggerBinder
-dontwarn lombok.Generated
-dontwarn lombok.NonNull
-dontwarn org.joda.convert.FromString
-dontwarn org.joda.convert.ToString

# Retrofit does reflection on generic parameters. InnerClasses is required to use Signature and
# EnclosingMethod is required to use InnerClasses.
-keepattributes Signature, InnerClasses, EnclosingMethod

# Retrofit does reflection on method and parameter annotations.
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations

# Retain service method parameters when optimizing.
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Ignore annotation used for build tooling.
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# Ignore JSR 305 annotations for embedding nullability information.
-dontwarn javax.annotation.**

# Guarded by a NoClassDefFoundError try/catch and only used when on the classpath.
-dontwarn kotlin.Unit

# Top-level functions that can only be used by Kotlin.
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# With R8 full mode, it sees no subtypes of Retrofit interfaces since they are created with a Proxy
# and replaces all potential values with null. Explicitly keeping the interfaces prevents this.
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>


#=================WALLET==================
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

-keepattributes *Annotation*

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers,includedescriptorclasses public class * extends android.view.View {
    void set*(***);
    *** get*();
}

-keepclassmembers class * extends android.app.Activity {
    public void *(android.view.View);
}

-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

-keepclassmembers class **.R$* {
    public static <fields>;
}

# android
-dontnote android.widget.SearchView

# androidx
-keep,includedescriptorclasses class androidx.fragment.app.FragmentContainerView {
    FragmentContainerView(android.content.Context,android.util.AttributeSet,androidx.fragment.app.FragmentManager);
}
-dontwarn androidx.viewpager.widget.PagerAdapter
-dontnote androidx.core.**
-dontnote androidx.fragment.app.FragmentTransition
-dontnote androidx.versionedparcelable.VersionedParcel
# OkHttp
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontnote okhttp3.internal.platform.ConscryptPlatform
-dontnote okhttp3.internal.platform.AndroidPlatform,okhttp3.internal.platform.AndroidPlatform$CloseGuard
-dontnote okhttp3.internal.platform.Android10Platform
-dontnote okhttp3.internal.platform.Platform
# ALSO REMEMBER KEEPING YOUR MODEL CLASSES
-keep class io.horizontalsystems.bankwallet.entities.** { *; }
-keep class io.horizontalsystems.bitcoincore.** { *; }
-keep class io.horizontalsystems.bitcoinkit.** { *; }
-keep class io.horizontalsystems.bitcoincash.** { *; }
-keep class io.horizontalsystems.bitcoincashkit.** { *; }
-keep class io.horizontalsystems.binancechainkit.** { *; }
-keep class io.horizontalsystems.xrateskit.** { *; }
-keep class io.horizontalsystems.ethereumkit.** { *; }
-keep class io.horizontalsystems.hdwalletkit.** { *; }
-keep class io.horizontalsystems.dashkit.** { *; }
-keep class io.horizontalsystems.hodler.** { *; }
-keep class io.horizontalsystems.litecoinkit.** { *; }
-keep class io.horizontalsystems.tools.** { *; }
-keep class in3.** { *; }
-keep class io.horizontalsystems.erc20kit.** { *; }
-keep class io.horizontalsystems.uniswapkit.** { *; }
-keep class io.horizontalsystems.feeratekit.** { *; }
-keep class io.horizontalsystems.coinkit.** { *; }
-keep class io.horizontalsystems.tor.** { *; }
-keep class org.bouncycastle.** { *; }
-keep class cash.z.** { *; }
-keep class org.dashj.bls.** { *; }
#-keep class io.horizontalsystems.bankwallet.core.adapters.zcash.** { *; }
-keep class com.unstoppabledomains.** { *; }
-keep class io.horizontalsystems.bankwallet.model.** { *; }

-keepclassmembers enum * { *; }
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }
-keep class kotlinx.coroutines.android.AndroidDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}
-keepnames class androidx.navigation.fragment.NavHostFragment
-keep class androidx.fragment.app.FragmentContainerView { *; }
-keep class com.google.firebase.inappmessaging.** { *; }
-keep class com.google.internal.firebase.inappmessaging.** { *; }

-keepclassmembers class * extends androidx.work.Worker {
    public <init>(android.content.Context,androidx.work.WorkerParameters);
}
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}


-keep class java.security.** { *; }
-keepclassmembers class java.security.** { *; }
-keep public interface java.security.Key {*;}
-keep public interface java.security.PublicKey {*;}
-keepclassmembers class * implements java.security.PublicKey {
    public <methods>;
}
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
-dontwarn org.codehaus.mojo.animal_sniffer.*
-dontwarn okhttp3.internal.platform.ConscryptPlatform
-dontwarn org.conscrypt.ConscryptHostnameVerifier
# OkHttp
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.squareup.okhttp.** { *; }
-keep interface com.squareup.okhttp.** { *; }
-dontwarn com.squareup.okhttp.**


# Retrofit
-keep class com.google.gson.** { *; }
-keep public class com.google.gson.** {public private protected *;}
-keep class com.google.inject.** { *; }
-keep class org.apache.http.** { *; }
-keep class org.apache.james.mime4j.** { *; }
-keep class javax.inject.** { *; }
-keep class javax.xml.stream.** { *; }
-keep class retrofit.** { *; }
-keep class com.google.appengine.** { *; }
-keepattributes *Annotation*
-keepattributes Signature
-dontwarn com.squareup.okhttp.*
-dontwarn rx.**
-dontwarn javax.xml.stream.**
-dontwarn com.google.appengine.**
-dontwarn java.nio.file.**
-dontwarn org.codehaus.**



-dontwarn retrofit2.**
-dontwarn org.codehaus.mojo.**
-keep class retrofit2.** { *; }
-keepattributes Exceptions
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations

-keepattributes EnclosingMethod
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keepclasseswithmembers interface * {
    @retrofit2.* <methods>;
}
# Platform calls Class.forName on types which do not exist on Android to determine platform.
-dontnote retrofit2.Platform
# Platform used when running on RoboVM on iOS. Will not be used at runtime.
-dontnote retrofit2.Platform$IOS$MainThreadExecutor
# Platform used when running on Java 8 VMs. Will not be used at runtime.
-dontwarn retrofit2.Platform$Java8
# Retain generic type information for use by reflection by converters and adapters.
-keepattributes Signature
# Retain declared checked exceptions for use by a Proxy instance.
-keepattributes Exceptions

-keep class com.android.org.conscrypt.** { *; }

-keep class com.bytedance.sdk.** { *; }
-keep class com.pgl.sys.ces.* {*;}
-keep class com.mopub.mobileads.PangleAdapterConfiguration.** { *; }
-keep public class com.google.android.gms.** { public protected *; }

# Keep public classes and methods.
-keep public class android.webkit.JavascriptInterface {}


# Support for Android Advertiser ID.
-keep class com.google.android.gms.common.GooglePlayServicesUtil {*;}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient {*;}
-keep class com.google.android.gms.ads.identifier.AdvertisingIdClient$Info {*;}

# Support for Google Play Services
# http://developer.android.com/google/play-services/setup.html
-keep class * extends java.util.ListResourceBundle {
    protected Object[][] getContents();
}

-keep public class com.google.android.gms.common.internal.safeparcel.SafeParcelable {
    public static final *** NULL;
}

-keepnames @com.google.android.gms.common.annotation.KeepName class *
-keepclassmembernames class * {
    @com.google.android.gms.common.annotation.KeepName *;
}

-keepnames class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# For a bug in AudioFocusHandler where we have a workaround via reflection
-keep class androidx.media2.player.AudioFocusHandler {*;}
-keepnames class androidx.media2.player.MediaPlayer {*;}
-keep class com.tradingview.lightweightcharts.** {
  public protected private *;
}

-keep class io.horizontalsystems.bitcoincash.** { *; }
-keep class io.horizontalsystems.bitcoincashkit.** { *; }
-keep class io.horizontalsystems.bitcoincore.** { *; }
-keep class io.horizontalsystems.bitcoinkit.** { *; }
-keep class io.horizontalsystems.dashkit.** { *; }
-keep class org.dashj.bls.** { *; }
-keep class io.horizontalsystems.hodler.** { *; }
-keep class io.horizontalsystems.litecoinkit.** { *; }
-keep class io.horizontalsystems.tools.** { *; }
-keep class io.horizontalsystems.erc20kit.** { *; }
-keep class in3.** {*;}
-keep class io.horizontalsystems.ethereumkit.** { *; }
-keep class io.horizontalsystems.nftkit.** { *; }
-keep class io.horizontalsystems.oneinchkit.** { *; }
-keep class io.horizontalsystems.uniswapkit.** { *; }
-keep class io.horizontalsystems.tonkit.** { *; }
-keep class io.horizontalsystems.binancechainkit.** { *; }
-keep class io.horizontalsystems.feeratekit.** { *; }
-keep class io.horizontalsystems.hdwalletkit.** { *; }
-keep class io.horizontalsystems.marketkit.** { *; }
-keep class io.horizontalsystems.ecash.** { *; }
-keep class io.horizontalsystems.solanakit.** { *; }
-keep class io.horizontalsystems.tor.** { *; }
-keep class io.horizontalsystems.tronkit.** { *; }
-keep class net.freehaven.tor.control.** { *; }
-keep class com.trustwallet.walletconnect.** { *; }
-keep class com.tradingview.lightweightcharts.** { *; }
-keep class com.twitter.** { *; }
-keep class io.horizontalsystems.bankwallet.modules.coin.tweets.** { *; }
-keep class com.walletconnect.** { *; }
-keep class io.horizontalsystems.bankwallet.core.App { *; }
-keepclassmembers class * { public <init>(...); }
-keep class org.koin.** {*;}
-keepclassmembers public class * extends androidx.lifecycle.ViewModel { public <init>(...); }
-keep class com.squareup.sqldelight.** { *; }
-keep,includedescriptorclasses class net.sqlcipher.** { *; }
-keep,includedescriptorclasses interface net.sqlcipher.** { *; }
-keep class io.horizontalsystems.bankwallet.modules.walletconnect.entity.** { *; }
-keep class io.horizontalsystems.bankwallet.modules.nft.** { *; }
-keep class io.horizontalsystems.bankwallet.core.providers.** { *; }
-keep class io.horizontalsystems.bankwallet.modules.hsnft.** { *; }
-keep class io.horizontalsystems.bankwallet.modules.transactions.** { *; }
-keep class io.horizontalsystems.bankwallet.modules.transactionInfo.** { *; }
-keep class io.horizontalsystems.bankwallet.modules.swap.** { *; }
-keep class io.horizontalsystems.bankwallet.modules.showkey.** { *; }
-keep class io.horizontalsystems.bankwallet.modules.transactionInfo.options.** { *; }
##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Application classes that will be serialized/deserialized over Gson
-keep class com.google.gson.examples.android.model.** { <fields>; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

##---------------End: proguard configuration for Gson  ----------
-keepnames class com.fasterxml.jackson.** { *; }
-dontwarn com.fasterxml.jackson.databind.**
-keep class org.spongycastle.** { *; }
-dontwarn javax.naming.directory.SearchControls
-dontwarn javax.naming.directory.SearchResult
-dontwarn org.slf4j.impl.StaticMDCBinder

-keepattributes InnerClasses
-keepnames class io.horizontalsystems.bitcoincore.network.Network
-keepnames class io.horizontalsystems.bitcoincore.models.Checkpoint

-keep class androidx.datastore.*.** {*;}
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite* {
   <fields>;
}

# RxJava
-keep class io.reactivex.** { *; }
-dontwarn io.reactivex.**

# RxAndroid
-keep class io.reactivex.android.** { *; }
-dontwarn io.reactivex.android.**

# RxJavaPlugins
-keep class rx.plugins.* { *; }
-dontwarn rx.plugins.*

# Retrofit uses RxJava
-keepattributes Exceptions
-keepattributes Signature
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# If you are using Gson for serialization/deserialization
-keep class com.google.gson.* { *; }
-dontwarn com.google.gson.**

# OkHttp uses Okio which is internal
-dontwarn okhttp3.internal.**
# Keep generic signature of Call, Response (R8 full mode strips signatures from non-kept items).
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

 # With R8 full mode generic signatures are stripped for classes that are not
 # kept. Suspend functions are wrapped in continuations where the type argument
 # is used.
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-keep class org.json.**
-keepclassmembers,includedescriptorclasses class org.json.** { *; }

-keep class androidx.lifecycle.** { *; }
-keep interface androidx.lifecycle.** { *; }
-keep @androidx.compose.runtime.Composable class * { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}
-keep class io.horizontalsystems.marketkit.models.** { *; }
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** w(...);
    public static *** v(...);
    public static *** i(...);
}
# Ktor Client
-keep class io.ktor.** { *; }
-keepclassmembers class io.ktor.** { *; }
-dontwarn io.ktor.**

# Ktor Client Network Sockets
-keep class io.ktor.client.network.sockets.** { *; }
-dontwarn io.ktor.client.network.sockets.**

# Ktor Client Plugins
-keep class io.ktor.client.plugins.** { *; }
-dontwarn io.ktor.client.plugins.**

# Ktor Utils IO
-keep class io.ktor.utils.io.** { *; }
-keepclassmembers class io.ktor.utils.io.** { *; }
-dontwarn io.ktor.utils.io.**

# Ktor Utils IO Core
-keep class io.ktor.utils.io.core.** { *; }
-keepclassmembers class io.ktor.utils.io.core.** { *; }
-dontwarn io.ktor.utils.io.core.**

# TON Blockchain
-keep class org.ton.** { *; }
-keepclassmembers class org.ton.** { *; }
-dontwarn org.ton.**

# TON TL
-keep class org.ton.tl.** { *; }
-dontwarn org.ton.tl.**

# TON BOC
-keep class org.ton.boc.** { *; }
-dontwarn org.ton.boc.**

# TON Block
-keep class org.ton.block.** { *; }
-dontwarn org.ton.block.**
-dontwarn io.ktor.client.network.sockets.SocketTimeoutException
-dontwarn io.ktor.client.plugins.HttpTimeout$HttpTimeoutCapabilityConfiguration
-dontwarn io.ktor.client.plugins.HttpTimeout$Plugin
-dontwarn io.ktor.client.plugins.HttpTimeout
-dontwarn io.ktor.utils.io.CoroutinesKt
-dontwarn io.ktor.utils.io.core.BytePacketBuilder
-dontwarn io.ktor.utils.io.core.ByteReadPacket
-dontwarn io.ktor.utils.io.core.ByteReadPacketExtensionsKt
-dontwarn io.ktor.utils.io.core.Input
-dontwarn io.ktor.utils.io.core.InputLittleEndianKt
-dontwarn io.ktor.utils.io.core.InputPrimitivesKt
-dontwarn io.ktor.utils.io.core.Output
-dontwarn io.ktor.utils.io.core.OutputLittleEndianKt
-dontwarn io.ktor.utils.io.core.OutputPrimitivesKt