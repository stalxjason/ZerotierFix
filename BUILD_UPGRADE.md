# ZerotierFix 升级说明（对照官网 1.16.0）

> 目标：把非官方 ZeroTier Android 客户端 ZerotierFix 补齐官网 1.16.0 的新特性、
> 升级原生核心到 1.14.0、并把 UI 迁移到 Material 3 + 动态取色（Monet）。

## 一、改动概览

| 类别 | 内容 |
|------|------|
| 原生核心 | `externals/core` 子模块从 `kaaass/ZeroTierOne`（1.4.6 时代）切换到 `zerotier/ZeroTierOne` **1.14.0** |
| UI 升级 | 主题由 `Theme.MaterialComponents.DayNight` 迁移到 `Theme.Material3.DayNight`，并在 `ZerotierFixApplication` 启用 Monet 动态取色；`build.gradle` Material 1.9.0 → 1.12.0、compileSdk/targetSdk 33 → 34、minSdk 19 → 21 |
| 新功能：扫码加入 | 加入网络页右上角「扫码」按钮，集成 ZXing（`zxing-android-embedded:4.3.0`），扫描二维码/链接自动解析并填入 16 位 Network ID |
| 新功能：禁用连接检查 | 设置项 `network_disable_connectivity_check`（与官网 1.16.0 完全一致，旧名 `network_disable_connection_check` 已废弃），开启后无网络时也尝试启动 |
| 新功能：App Links / 深链 | 支持官网二维码/分享链接 `https://joinzt.com/addnetwork?v=1&nwid=<16位ID>`（与官网 1.16.0 一致），并兼容旧式 `zerotier://join?n=<id>`、`https://join.zerotier.com/<id>`、`https://my.zerotier.com/network/<id>` 一键跳转到加入页（非验证型，无需 `assetlinks.json`） |
| 体验优化：等待重连 | 启动时若网络不可用，注册 `ConnectivityManager` 回调，待 Wi-Fi/Cell 恢复后自动重试（约 30s 超时），而非立即退出 |

## 二、⚠️ 重要约束（务必阅读）

**本次所有改动均在本机源码层面完成，但本机没有 Android SDK / JDK / NDK，无法编译验证。**

- Java 与资源改动经过逐文件审查，并**对照了 1.14.0 真实的 JNI 源码**（`Node.java`、`VirtualNetworkConfig.java`、`VirtualNetworkFrameListener` 等接口），确认现有 app 代码与 1.14.0 JNI API 完全兼容。
- 但 Android 工程的**最终正确性必须由你在装有 Android Studio + Android SDK + NDK 的机器上 `gradlew assembleDebug` 编译验证**。原生核心 1.14.0 的重编尤其需要 NDK。

## 三、构建前准备

1. 安装 **Android SDK**（compileSdk 34）、**NDK（建议 r25+）**、**CMake 3.22.1**。
2. 确认子模块已切到 1.14.0（本机已执行；如工作树未切换，手动执行下面命令）。
3. 在仓库根目录执行：`./gradlew assembleDebug`。

**若需手动切换子模块到 1.14.0：**

```bash
git submodule sync
cd externals/core
git remote set-url origin https://github.com/zerotier/ZeroTierOne.git
git fetch origin 1.14.0
git checkout 1.14.0
cd ../..
git add externals/core        # 记录新的 gitlink
```

## 四、原生核心升级要点

- **API 兼容性（已核对）**：1.14.0 的 JNI 接口与现有 app 调用一致：
  - `new Node(long now)`、`init(get, put, sender, event, frame, config, pathChecker)`、`processBackgroundTasks`、`join/leave`、`multicastSubscribe/Unsubscribe`、`orbit`、`networkConfig(s)`、`peers()`、`getVersion()`、`address()`、`status()`、`isInited()`、`close()` —— 签名均未变化。
  - `TunTapAdapter` 实现的 `onVirtualNetworkFrame(...)` 与 1.14.0 `VirtualNetworkFrameListener` 完全一致。
  - `VirtualNetworkConfig` 的 `getAssignedAddresses / getRoutes / getDns / getMac / getName / getStatus / getType / getMtu` 均存在。
  - 因此**业务 Java 代码无需为升级核心做任何改动**。
- **编译源来源修复**：原仓库用 `core/src/main/java` 符号链接指向 `externals/core/java/src`，在 Windows 下符号链接退化为普通文件，导致 `com.zerotier.sdk.*` 编译源丢失。已在 `core/build.gradle` 中改为显式 `sourceSets { main { java.srcDirs = ['../externals/core/java/src'] } }`，跨平台可构建，并使「核心升级时 Java 与 Native 同步更新」。
- **NDK 构建注意**：1.14.0 引擎可能需要较新的 NDK / C++ 标准。如遇原生编译错误，优先调整 `core/build.gradle` 的 `ndkVersion` 与 `externalNativeBuild` 配置（必要时参考 `externals/core/java/CMakeLists.txt` 的要求）。

### 4.1 `java/CMakeLists.txt` 与 1.14.0 的偏差修复（必做）

上游 `zerotier/ZeroTierOne` **1.14.0 的 `externals/core/java/CMakeLists.txt` 是过时的**：它引用的几个源文件在该版本已被重构移除，直接 `assembleDebug` 会在 `:core:configureCMakeDebug` 报 `Cannot find source file: ../ext/lz4/lz4.c`。已在本仓库修复（`externals/core/java/CMakeLists.txt`）：

- 删除 4 个已不存在的源文件引用：`../ext/lz4/lz4.c`、`../ext/json-parser/json.c`、`../node/Defaults.cpp`、`../node/Dictionary.cpp`
  - `ext/lz4` → 已内联进 `node/Packet.cpp`（提交 `0995c1dc`）。
  - `ext/json-parser` → 已整体移除（提交 `8e3463d4`）。
  - `node/Defaults.cpp` / `node/Dictionary.cpp` → 重构移除（`objects.mk` 主构建清单也不列这俩，符号已并入其他 node 源）。
- node 源文件改用 `file(GLOB .../node/*.cpp)` 全量编译（覆盖 1.14.0 新增的 Bond/Capability/Membership/Revocation/Tag/Trace/Path 等被引用文件，避免链接缺符号）。
- **保留 `node/Metrics.cpp`（不要排除）** 并加 `add_definitions(-DZT_NO_PEER_METRICS)`：`Metrics.cpp` 会**无条件**定义 `ZeroTier::Metrics` 的 counter 单例（`registry_ptr` / `packets` / `pkt_*_in` / `network_*` 等），而引擎在 `Network.cpp`、`Node.cpp`、`IncomingPacket.cpp`、`Switch.cpp`、`Bond.cpp` 等处**无守卫地引用**这些符号。只有 `Metrics.cpp:179` 之后的「发布到 prometheus」逻辑被 `#ifndef ZT_NO_PEER_METRICS` 关掉，**counter 定义本身在宏守卫之外**，所以排除 `Metrics.cpp` 反而会导致链接期未定义符号。正确做法是保留它一起编译，并给 prometheus 头文件加上 include 目录（`ext/prometheus-cpp-lite-1.0/core/include` 与 `.../simpleapi/include`）——prometheus-cpp-lite 是 header-only（实现 inline 在 `core/include/prometheus/*.h`），**无需链接额外库**。`make-linux.mk:12` 的 `INCLUDES` 正是这样配的。
- 同时给 include 目录加上 `ext/nlohmann`：`OSUtils.hpp` 在 `#ifndef OMIT_JSON_SUPPORT` 下 `#include <nlohmann/json.hpp>`（仅 OSUtils 使用，已全守卫），加上该目录即可保留 JSON 能力。
- 注：`externals/core` 是子模块，改此文件会使其工作树相对 1.14.0 变 dirty（仅影响本机构建）。

## 五、功能自测清单

- [ ] **扫码加入**：加入网络页 → 右上角扫码 → 扫描网络二维码/邀请链接 → 自动填入 16 位 ID、启用「加入」按钮。
- [ ] **禁用连接检查**：设置中开启 → 飞行模式/无网下点连接 → 不再立即报错退出（等待重连）。
- [ ] **深链**：浏览器/其他 App 打开 `https://joinzt.com/addnetwork?v=1&nwid=<16位ID>`（官网同款，扫码即得此链接）→ 跳转并预填；旧式 `https://join.zerotier.com/<16位ID>`、`zerotier://join?n=<16位ID>` 同样兼容。
- [ ] **等待重连**：连接中切换飞行模式再恢复 → 服务在数秒内自动重连（约 30s 超时后才会停止）。
- [ ] **UI**：Android 12+ 跟随壁纸动态配色；旧系统使用橙色回退主题；明暗模式均正常。
- [ ] **常规**：加入/离开网络、Peer 列表、Moon 入轨、自定义 Planet（文件/URL）仍可用。

## 六、已知风险

- **[高]** 原生核心 1.14.0 需 NDK 重编，未在本机编译验证，可能需要微调 NDK / CMake 配置。
- **[中]** Material 3 迁移后，个别旧布局/控件属性（如自定义 `AlertDialog`、`Switch`）建议目测微调。
- **[低]** 深链为「非验证型 App Link」（`android:autoVerify="false"`），无需在域名发布 `assetlinks.json`；如需浏览器直接点头验证，需在对应域名托管 `assetlinks.json` 并将 `autoVerify` 改为 `true`。

## 七、主要改动文件索引

- `build.gradle` / `app/build.gradle` / `core/build.gradle` / `.gitmodules` —— 构建与核心子模块
- `app/src/main/res/values/themes.xml` + `values-night/themes.xml` —— Material 3 主题
- `app/src/main/java/.../ZerotierFixApplication.java` —— 动态取色
- `app/src/main/java/.../util/JoinTargetParser.java`（新增）—— 二维码/链接解析
- `app/src/main/java/.../ui/JoinNetworkActivity.java` / `JoinNetworkFragment.java` / `CaptureActivity.java` / `menu_join_network.xml` + `res/layout/capture_small.xml` —— 扫码（ZXing 内嵌界面，对齐官网 1.16.0）
- `app/src/main/java/.../ui/NetworkListActivity.java` / `AndroidManifest.xml` —— 深链
- `app/src/main/res/xml/preferences.xml` + `service/ZeroTierOneService.java` + `util/Constants.java` —— 禁用连接检查 + 等待重连
