<h1 align="center">
  <img src="app/src/main/ic_launcher-playstore.png" alt="Zerotier Fix" width="200">
  <br>Zerotier Fix<br>
</h1>

<h4 align="center">ZeroTier 非官方 Android 客户端，基于官方客户端反编译修补而成。</h4>

<p align="center">
  <img src="screenshots/main_cn.png" alt="主界面" width="150"/>
  <img src="screenshots/peers_cn.png" alt="节点列表" width="150"/>
  <img src="screenshots/moons_cn.png" alt="Moon 列表" width="150"/>
</p>

<p align="center">
  <a href="https://github.com/stalxjason/ZerotierFix/actions/workflows/build-app.yml">
    <img src="https://github.com/stalxjason/ZerotierFix/actions/workflows/build-app.yml/badge.svg" alt="构建状态"/>
  </a>
</p>

> **Fork 说明：** 本仓库是 **[kaaass/ZerotierFix](https://github.com/kaaass/ZerotierFix)** 的 fork（分支）。
> 它在原项目的基础上增加了一些新功能与修复（见下方
> [与上游的差异](#与上游-kaaasszerotierfix-的差异)）。本仓库的维护版本位于
> **[stalxjason/ZerotierFix](https://github.com/stalxjason/ZerotierFix)**。

## 功能特性

- 支持自建 **Moon**（月球节点）中转
- 自定义 **Planet**（行星配置）支持：文件、**URL**、以及**扫描二维码**导入
- 可通过**扫描二维码**或点击**深链**（`joinzt.com`、`zerotier://` 等）加入网络
- 查看 **Peers**（节点）列表
- 提供「**禁用连接检查**」选项（无网络时也可启动）
- **Material 3** 界面，支持动态取色（Monet）
- 内置**中文 / 英文**双语
- 在 **Android 13 及以上**会主动申请「发送通知」权限
- 扫码界面**锁定为竖屏**，单手扫码更顺手

## 与上游（kaaass/ZerotierFix）的差异

本 fork 在原项目基础上额外做了以下改动：

| 方面 | 改动 |
|------|------|
| **内核引擎** | 内置的 ZeroTierOne 核心由 `1.14.0` 升级到 **`1.16.2`** |
| **Planet 导入** | 新增「扫描 Planet 二维码」设置项——可解析原始 base64 行星文件，**或**官方 `/addplanet` 深链，并写入文件式自定义 Planet 槽位 |
| **通知权限** | Android 13+（API 33）运行时主动申请 `POST_NOTIFICATIONS` 权限，而非静默无反应 |
| **扫码方向** | 扫码界面强制**竖屏**。所依赖的 `zxing-android-embedded` 库默认是横屏（`sensorLandscape`），本 fork 已覆盖，使扫码界面在任何设备上始终保持竖立 |
| **移动网络限制** | 移除了「允许使用移动网络」的限制，现在在任何网络下都会连接 |
| **仓库结构** | 将 ZeroTierOne 核心以**普通目录**形式并入仓库（不再使用 git 子模块），`git clone` 一次即可直接编译 |
| **主题** | 升级为 Material 3 并启用 Monet 动态取色 |

## 下载

请前往 [Releases 发布页](https://github.com/stalxjason/ZerotierFix/releases) 获取最新版本。

如果你想试用每夜构建（nightly），可从
[GitHub Actions](https://github.com/stalxjason/ZerotierFix/actions/workflows/build-app.yml?query=branch%3Amaster)
下载。请注意每夜构建可能**不稳定**、存在 **Bug**。

## 从源码构建

环境要求：

- Android SDK（compileSdk / targetSdk 34，minSdk 21）
- JDK 17
- NDK（用于 `:core` JNI 原生模块）

在 Windows 上请使用 PowerShell + `gradlew.bat` 构建（Git Bash 下的 `./gradlew` 因 MinGW 路径转换问题无法工作）：

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
cd ZerotierFix
.\gradlew.bat --stop          # 若改过 CMakeLists，先停掉守护进程
.\gradlew.bat assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

## 版权声明

本仓库代码基于对官方 Android 客户端的反编译，原作者为 Grant Limberg（glimberg@gmail.com）。
详见 [AUTHORS.md](https://github.com/zerotier/ZeroTierOne/blob/master/AUTHORS.md#primary-authors)。

- ZeroTier JNI SDK 源码位于 `externals/core`（内置的 ZeroTierOne 源码，原为 git 子模块）
- 原 Android 客户端代码位于 `net.kaaass.zerotierfix`（由 `com.zerotier.one` 重命名而来）
- 应用图标为 `ZeroTier, Inc.` 的商标，由原作者制作

## 后续计划

- [X] Moon 配置持久化与文件配置
- [x] 节点列表视图
- [x] 支持 Planet 配置
- [x] 替换预编译的 JNI 库
- [x] 重写并升级 UI 至 Material Design
- [x] 内置引擎升级至 ZeroTierOne 1.16.2
- [x] 扫码导入 Planet + Android 13 通知权限
- [x] 扫码界面强制竖屏
- [ ] *进行中* 用 v2 重写整个 APP
