# LogUtils

一个基于 Kotlin 开发的轻量级 Android 日志库，修改自知名项目 [AndroidUtilCode](https://github.com/Blankj/AndroidUtilCode) 中的 `LogUtils` 模块。

---

## 功能特点

- 基于 Kotlin 的实现，简洁高效。
- 支持多种日志级别（如：`debug`、`error`、`info`）。
- 可自定义日志 TAG，方便日志筛选和管理。
- 提供现代 Android 开发所需的最小化设计。

---

## 安装方法

本库已托管在 [JitPack](https://jitpack.io)。按照以下步骤集成到项目中：

### 第一步：添加 JitPack 仓库

在项目的 `build.gradle`（项目根目录）中添加：

```groovy
allprojects {  
    repositories {  
        maven { url 'https://jitpack.io' }  
    }  
}  
```

### 第二步：添加依赖

在模块的 `build.gradle` 中添加以下依赖：

```groovy
dependencies {  
    implementation 'com.github.TaylorsZ:LogUtils-Kotlin:1.0.4'  
}  
```

如果你使用的是 Kotlin DSL（`build.gradle.kts`），请添加以下代码：

```kotlin
dependencies {  
    implementation("com.github.TaylorsZ:LogUtils-Kotlin:1.0.4")  
}  
```

---

## 使用方法

### 初始化

在应用的 `Application` 类中初始化库（可选设置默认 TAG）：

```kotlin
class MyApp : Application() {  
    override fun onCreate() {  
        super.onCreate()
        LogHelpr().init(this)
    }  
}  
```

### 打印日志

直接调用日志方法即可：

```kotlin
LogUtils.d("这是一个调试日志")  
LogUtils.e("这是一个错误日志")  
LogUtils.i("这是一个信息日志")  
```

### 完整示例

```kotlin
fun main() {  
    // 初始化日志库  
    LogUtils.init("DemoApp")  

    // 打印不同级别的日志  
    LogUtils.d("调试日志内容")  
    LogUtils.i("信息日志内容")  
    LogUtils.w("警告日志内容")  
    LogUtils.e("错误日志内容")  
}  
```

---

## 项目来源

本项目基于 [Blankj/AndroidUtilCode](https://github.com/Blankj/AndroidUtilCode) 的 `LogUtils` 模块进行了 Kotlin 重构和优化，更适合现代 Android 开发需求。

本项目基于 [teprinciple/UpdateAppUtils](https://github.com/teprinciple/UpdateAppUtils) 进行了 Kotlin 重构和优化，适配targetSDK=34。

---

## 许可证

本项目使用 MIT 许可证。详情请参阅 [LICENSE](LICENSE) 文件。

---

## 问题反馈

如果你在使用过程中遇到问题或有功能需求，欢迎在 GitHub 仓库提交 Issue。
