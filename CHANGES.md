# ZerotierFix 本次修改总结

> 时间：2026-07-21 ~ 2026-07-22
> 范围：把第三方 ZeroTier Android 客户端 ZerotierFix 对照官网升级（核心 1.14.0 + Material 3），本机成功编译出可运行 APK，修复若干问题、移除移动网络限制、将子模块拍平成单仓库，并将内嵌引擎追到官方 1.16.2，全部推送到 GitHub。
> 仓库：`https://github.com/stalxjason/ZerotierFix.git`（已推送，远端 master = `cc4b845`）

---

## 一、原生编译问题修复（首次 `assembleDebug` 通过）

根因链与对应修复：

| 问题 | 根因 | 修复 |
|------|------|------|
| `:core:buildCMakeDebug` 找不到 nlohmann / prometheus 头 | CMakeLists include 目录层级错 | `externals/core/java/CMakeLists.txt` 中 include 目录改为 `../ext`（而非 `../ext/nlohmann`），并补 `ext/prometheus-cpp-lite-1.0/core/include` 与 `.../simpleapi/include` |
| 链接 undefined `__android_log_print` | 原生未链接 Android `log` 库 | CMakeLists 在 `if(ANDROID)` 下 `find_library(log-lib log)` 并 `target_link_libraries(${PROJECT_NAME} ${log-lib})` |
| `:app:compileDebugJavaWithJavac` 86 个错（缺 getter/构造器） | Lombok 1.18.20 不支持 JDK17 | `app/build.gradle` Lombok `1.18.20` → `1.18.34`（compileOnly + annotationProcessor） |
| `Network` 类型歧义 | `ZeroTierOneService` 同时 import `android.net.Network` 与 `model.Network` | 删 `android.net.Network` 导入，相关处显式限定 |
| 多处 case/常量/方法找不到 | 缺 `import static` | 补 `DNSMode.*` / `NetworkStatus.*` 的 `import static` |
| zxing 包不存在 | 包名写错 | `JoinNetworkActivity` 中 `com.journeyapps.zxing.android.embedded` → `com.journeyapps.barcodescanner`（ScanContract/ScanOptions）+ 补 `CaptureActivity` 导入 |
| AGP 原生缓存 `.cxx` 删不掉 | gradle 守护进程持锁，`Remove-Item` 静默失败 | 删 `.cxx` 前先 `./gradlew.bat --stop` 停守护进程 |
| `./gradlew` 报 ClassNotFound | Git Bash 把路径转成 MINGW（`/c/...`），Windows `java.exe` 不认 | 改用 **PowerShell + `gradlew.bat` + Windows 风格 `JAVA_HOME`**（`C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot`） |

产物：`app/build/outputs/apk/debug/app-debug.apk`，`BUILD SUCCESSFUL`。

---

## 二、修复「设置入口消失」（用户报"设置被删，没地方设置"）

**根因澄清**：设置入口代码（`menu_network_list.xml` 的 `menu_item_settings` → `PrefsActivity` → `PrefsFragment`）从未被删，与原始提交完全一致。真正原因是此前"升级到 Material 3"时把基础主题从 `Theme.MaterialComponents.DayNight.DarkActionBar` 换成 `Theme.Material3.DayNight`，而 **M3 主题不提供窗口自带 ActionBar**；全 app 所有 Activity 都依赖 `getSupportActionBar()` 且从未调用 `setSupportActionBar(toolbar)`，导致顶部 ActionBar（含 设置 / Peers / Orbit 三个点菜单）整体不显示 → 用户"找不到设置"。

**修复（保留 M3 动态取色升级）**：
- `app/src/main/res/layout/activity_fragment.xml`：加入 `com.google.android.material.appbar.MaterialToolbar`（id=`toolbar`，并 `fitsSystemWindows` 防状态栏遮挡）
- `app/src/main/java/.../ui/SingleFragmentActivity.java`：`onCreate` 中 `setSupportActionBar(toolbar)` —— 一处改动覆盖 NetworkList / JoinNetwork / Prefs / PeerList / MoonOrbit / NetworkDetail 全部页面
- `res/values/themes.xml` + `values-night/themes.xml`：把 `windowActionBar` 由 `true` 改为 `false`（避免与 Toolbar 重复）

重编 `BUILD SUCCESSFUL`，主界面右上角三个点菜单恢复 设置 / Peers / Orbit。

---

## 三、移除「移动网络限制」（用户："这个不需要了"）

删除了整套"允许移动网络下使用"功能：
- `service/ZeroTierOneService.java`：去掉启动时的 `CONNECTION_MOBILE` 拦截（不再 `waitForReconnect`，仅保留无网络时的等待）
- `ui/NetworkListFragment.java`：开网络时去掉 `useCellularData` 判定，移动数据下也直接连接
- `ui/PrefsFragment.java`：`onSharedPreferenceChanged` 中相关分支清空
- `util/Constants.java`：删 `PREF_NETWORK_USE_CELLULAR_DATA`
- `res/xml/preferences.xml`：删 `network_use_cellular_data` CheckBoxPreference
- 删字符串 `preferences_network_use_cell_data`、`toast_mobile_data`（zh + en）

重编 `BUILD SUCCESSFUL`。

---

## 四、子模块拍平为单仓库（用户："不能都推送到同一个仓库吗"）

`externals/core` 原本是 git 子模块（独立仓库，主仓库只记 gitlink + URL），子模块的 CMakeLists 修复无法随主仓库推送、且别人 clone 后取不到本地提交。用户决定把所有代码并入同一个仓库：

- 删除 gitlink（`git rm --cached externals/core`）、`.gitmodules`、`.git/modules/externals`
- 把 `externals/core` 的 ZeroTierOne 1.14.0 完整源码（57MB / 1790 文件）作为**普通目录**纳入主仓库
- 提交 `1808ccb`（"chore: vendor ZeroTierOne core as plain dir, drop submodule"）

之后 `git submodule status` 为空，整个项目自包含，clone 即可编译，无需 submodule fetch。代价：主仓库体积变大（含 ZeroTierOne 源码）。

---

## 五、Git 提交与首次推送

| 提交 | 说明 |
|------|------|
| `d3d6017` | 升级到 1.14.0 核心 + Material 3、扫码/深链加入、移除移动网络限制、修复设置入口 |
| `1808ccb` | 拍平子模块（含 ZeroTierOne 1.14.0 全部源码 + CMakeLists 修复） |

- 已推送到 `origin`（=`https://github.com/stalxjason/ZerotierFix.git`）的 `master` 分支
- 首次推送校验：远端 master HEAD = `1808ccb`，与本地一致

---

## 六、引擎升级 1.14.0 → 1.16.2（追官方最新）

应"追一下更新"，把内嵌引擎 `externals/core` 从 1.14.0 追到官方 `zerotier/ZeroTierOne` 最新稳定版 **1.16.2**（2026-05-28 发布，BSL 已过 Change Date 转为 Apache 2.0，vendoring 无许可顾虑）。

**过程与关键坑：**
- 浅克隆官方 `1.16.2` tag 到临时目录 `_zt1162`，用 `tar --exclude=.git` 干净倒入 `externals/core`（无嵌套 `.git`）。
- **上游 `java/CMakeLists.txt` 对 1.16.2 是过时的、不可用**——它仍显式引用已被上游删除的文件（`ext/lz4/lz4.c`、`ext/json-parser/json.c`、`node/Defaults.cpp`、`node/Dictionary.cpp`）。这些在 1.14.0→1.16.2 之间被重构内联进了 `Packet.cpp` 等，已不存在。
- 正确做法沿用此前 1.14.0 的 GLOB 思路重写 `externals/core/java/CMakeLists.txt`：
  - `file(GLOB NODE_SOURCES ../node/*.cpp)`（含 `Metrics.cpp`，其计数器被 8 个 node 文件 unguarded 引用，必须编）
  - + `ext/http-parser/http_parser.c` + `osdep/*.cpp` + `jni/*.cpp`
  - include 加 `../ext`（nlohmann）+ `ext/prometheus-cpp-lite-1.0/core/include` 与 `.../simpleapi/include`
  - `add_definitions(-DZT_NO_PEER_METRICS)`（仅关闭 metrics 发布）
  - Android 链 `-llog`（`__android_log_print` 在 `PortMapper.cpp` / `ZT_jniutils.h` 仍用到）
- `app/` 用到的 `com.zerotier.sdk` 类清单在 1.14.0 ↔ 1.16.2 间**完全一致**，故 `:app` 编译零报错，无需改 UI 代码。

**验证**：停 gradle 守护 + 删 `core/.cxx` 与 `core/build` 后 `.\gradlew.bat assembleDebug` → **BUILD SUCCESSFUL（1m8s）**，arm64-v8a / armeabi-v7a / x86 / x86_64 四种 ABI 的 `libZeroTierOneJNI.so` 均产出，APK 14.6MB。

**提交与推送：**
- 提交 `cc4b845`（"chore(core): upgrade vendored ZeroTierOne engine 1.14.0 -> 1.16.2"，1871 文件变更）
- 推送：`1808ccb..cc4b845 master -> master`（远端已有 `1808ccb`，本次只推引擎升级）；`git ls-remote` 校验远端 = 本地 = `cc4b845`，已同步。
- 本步同时清理了临时文件：10 个 `build_log*.txt` 编译日志 + 临时克隆 `_zt1162/`。

---

## 七、改动文件清单（索引）

**构建 / 原生**
- `externals/core/java/CMakeLists.txt`（已 vendor）—— nlohmann/prometheus include 路径（1.16.2 仍为 GLOB node/*.cpp）、`-DZT_NO_PEER_METRICS`、链接 Android `log` 库
- `app/build.gradle` —— Lombok 1.18.20 → 1.18.34；Material 1.12.0；compileSdk/targetSdk 34、minSdk 21
- `.gitmodules` —— 已删除（拍平）
- `local.properties` —— `sdk.dir` 指向本机 Android SDK

**UI / 主题**
- `res/values/themes.xml` + `values-night/themes.xml` —— Material 3 主题 + `windowActionBar=false`
- `ZerotierFixApplication.java` —— 启用 Monet 动态取色
- `res/layout/activity_fragment.xml` —— 新增 `MaterialToolbar`
- `ui/SingleFragmentActivity.java` —— `setSupportActionBar(toolbar)`

**功能（来自早期升级对齐）**
- `util/JoinTargetParser.java`（新增）—— 二维码/链接解析
- `ui/JoinNetworkActivity.java` / `JoinNetworkFragment.java` / `CaptureActivity.java` / `menu_join_network.xml` + `res/layout/capture_small.xml` —— ZXing 扫码加入
- `ui/NetworkListActivity.java` / `AndroidManifest.xml` —— App Links / 深链（joinzt.com 等）
- `res/xml/preferences.xml` + `service/ZeroTierOneService.java` + `util/Constants.java` —— 禁用连接检查 + 等待重连

**本次会话新增/删改**
- `ui/ZeroTierOneService.java` / `ui/NetworkListFragment.java` / `ui/PrefsFragment.java` / `util/Constants.java` / `res/xml/preferences.xml` / `res/values/strings.xml` + `values-zh/strings.xml` —— 移除移动网络限制
- `externals/core/**` —— 1.14.0 → 1.16.2 整树替换（1871 文件）
- `BUILD_UPGRADE.md` / `CHANGES.md` —— 说明文档（本文档已提交）

---

## 八、Windows 本机构建命令（备忘）

```powershell
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.19.10-hotspot"
cd ZerotierFix
.\gradlew.bat --stop          # 如需清理原生缓存，先停守护进程
# Remove-Item app\.cxx, externals\core\.cxx 后再编（如改过 CMakeLists）
.\gradlew.bat assembleDebug
```

---

## 九、可选后续

- 子页面（设置/加入网络）如需左上角「←」返回箭头：给各 Activity 加 `setDisplayHomeAsUpEnabled(true)`
- 仓库体积因 vendor ZeroTierOne 源码变大（约 57MB + 1.16.2 增量）；如需瘦身可后续再议
- 若要继续"追官方"：可对照官方 `ZeroTier+One_1.16.0-1` APK 反编译，把官方最新 UI/交互行为移植进 ZerotierFix（反编译产物仅作参考字典，不建议在其上直接改 smali）
