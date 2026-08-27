# JNI 方法由 NativeEngine 使用；保持名称，避免原生注册/查找受混淆影响。
-keep class com.xiangyan.nativeapp.engine.NativeEngine { *; }
