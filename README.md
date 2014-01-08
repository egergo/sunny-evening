sunny-evening
=============

```bash
git submodule init
git submodule update

cd jni

svn checkout http://libyuv.googlecode.com/svn/trunk/ libyuv

export NDK_HOME=/Users/egergo/Downloads/android-ndk-r9b

cd libvpx
./configure --sdk-path=$NDK_HOME --target=armv7-android-gcc --disable-examples --enable-vp8 --disable-vp9 --enable-realtime-only --enable-error-concealment --disable-neon
cd ..
libvpx/configure --sdk-path=$NDK_HOME --target=armv7-android-gcc --disable-examples --enable-vp8 --disable-vp9 --enable-realtime-only --enable-error-concealment --disable-neon

$NDK_HOME/ndk-build
```

