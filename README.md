# LogUtils

基于 Kotlin 的轻量级 Android 日志库，修改自 [AndroidUtilCode](https://github.com/Blankj/AndroidUtilCode) 的 `LogUtils` 模块，并集成了 [UpdateAppUtils](https://github.com/teprinciple/UpdateAppUtils) 的 Kotlin 重构版。

---

## 功能特点

- 纯 Kotlin 实现，简洁高效
- 支持 `d` / `i` / `w` / `e` / `json` 等多种日志级别
- 支持日志写入文件，按日期自动分目录
- 可自动清理过期日志（`saveDays`）
- 可自定义日志 TAG、文件前缀、扩展名
- 包含应用内更新功能（UpdateAppUtils）

---

## 安装

### Step 1: 添加 JitPack 仓库

项目根目录 `settings.gradle` 或 `build.gradle` 中：

```groovy
repositories {
    maven { url 'https://jitpack.io' }
}
```

### Step 2: 添加依赖
本地
```groovy
dependencies {
    implementation 'com.taylorz:logutils:1.1.1'
}
```

仓库
```groovy
dependencies {
    implementation 'com.github.TaylorsZ:LogUtils-Kotlin:Tag'
}
```



---

## 使用方法

### 初始化（Application 中）

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        val config = LogUtils.config
        config.globalTag = "MyApp"           // 全局 TAG
        config.isLogSwitch = true            // 日志总开关
        config.isLog2FileSwitch = true       // 写入文件开关
        config.saveDays = 7                  // 日志保留天数

        // 自定义日志存储目录（需要存储权限，见下方说明）
        config.dir = Environment.getExternalStorageDirectory().absolutePath + "/MyLogs/"
        config.filePrefix = "app"            // 文件名前缀
        config.fileExtension = ".log"        // 文件扩展名

        // 应用内更新初始化
        UpdateAppUtils.init(this)
    }
}
```

### 打印日志

```kotlin
LogUtils.d("调试信息")
LogUtils.i("普通信息")
LogUtils.w("警告信息")
LogUtils.e("错误信息")
LogUtils.json("{ \"key\": \"value\" }")
```

### 打印异常

```kotlin
try {
    // ...
} catch (e: Exception) {
    LogUtils.e(e)
}
```

---

## 存储权限说明

如果日志目录使用 `Environment.getExternalStorageDirectory()`，需要处理分区存储权限：

- **Android 10**：Manifest 中需添加 `android:requestLegacyExternalStorage="true"`
- **Android 11+**：需用户在系统设置中授予"所有文件访问权限"

或使用应用专属目录（无需权限）：`getExternalFilesDir(null)?.absolutePath`

---

## 应用内更新

```kotlin
UpdateAppUtils.getInstance()
    .apkUrl("https://example.com/app.apk")
    .updateTitle("发现新版本")
    .updateContent("- 修复已知问题\n- 优化体验")
    .updateConfig(UpdateConfig().apply {
        serverVersionCode = 2
        serverVersionName = "1.2.0"
    })
    .update()
```

---

## 许可证

MIT License
