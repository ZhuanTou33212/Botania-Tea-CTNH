# Botania: tea — CTNH internal module

CTNH 专用的 Tea Art / Botania: tea 源码模块。仅通过 CTNH-Core 的 jarJar 分发，不独立发布模组。

本仓库包含 `tea_art`、`botania_tea` 与基础常量库所需源码和资源，使用全新的提交历史。原版 Botania: tea 继续独立开发和发布。

## 构建

本仓库使用 CTNH-Modules 的共享构建脚本，不提供独立 Gradle 构建。
Minecraft 1.20.1、Java 17 目标、Forge 47.4.10、Parchment 2023.09.03。

在 CTNH-Modules 中添加 `modules/Tea-Art-CTNH` 子模块，并在 `settings.gradle` 中指定：

```groovy
include "modules:Tea-Art-CTNH"
project(":modules:Tea-Art-CTNH").buildFileName = "ctnh.module.gradle"
```

在 CTNH-Core 的 `dependencies.gradle` 中添加：

```groovy
implementation(project(":modules:Tea-Art-CTNH")) { transitive = false }
jarJar(project(":modules:Tea-Art-CTNH")) { transitive = false }
```

然后从 CTNH-Modules 根目录运行：

```powershell
.\gradlew.bat :modules:Tea-Art-CTNH:build
.\gradlew.bat :modules:CTNH-Core:build
```

CTNH-Lib 由 Core 独立打包，本模块仅声明非传递依赖。保留 `tea_art` 与 `botania_tea` 两个 Mod ID、注册名和现有存档格式。

## 专用内容

- 源质液体单位 100 mB；铁粒、金粒等金属粒提供 15 mB 金属源质。
- 支持的合成分析路径将输出限制在输入总量以内，允许损耗。
- 8000 mB 源质储罐，客户端可见液面，并保存、同步液体状态。
- 元素液体使用岩浆纹理、水的流动参数，按元素颜色染色。
- 每种胚材拥有独立贴图。

## 验证状态

专用模块的编译、资源处理、Spotless、语言注入和重混淆已通过。
本地 CTNH-Core 编译与 jarJar 验证通过，包含本模块嵌套 JAR、两个 Mod ID、储罐资源和有效的 Mixin refmap 引用。
Core 的已有 Java 格式检查涉及 434 个文件，本地打包验证单独跳过了该检查；本模块自身检查正常执行。

客户端、专用服务器启动及整合包全部自定义配方的源质守恒仍待游戏内验收。

## 许可

All Rights Reserved。版权由原作者保留；源码公开不改变其许可。
