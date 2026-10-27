# CLAUDE.md — magnet-player 实施指南

这是一个**安卓平板磁力链接在线播放器**。用户粘贴磁力链接，App 原画边下边播，不在设备上留文件。
目标设备：**小米平板（HyperOS / Android，横屏，arm64-v8a）**，只给用户本人自用。

开工前必须先读：

1. `docs/PLAN.md` —— 技术方案、选型理由、架构、TorrServer 嵌入方式、字幕路线、里程碑。
2. `docs/UI_SPEC.md` —— 界面规范（颜色、尺寸、每个页面的元素和交互）。
3. `docs/design/*.html` —— 设计稿（1280×800，1px = 1dp）。实现时用浏览器打开对照，**尽量做到像素级一致**。

## 沟通

- 用户只说中文：所有回复、提交说明、代码注释、App 界面文字都用**简体中文**（代码标识符用英文）。
- 用户只在 GitHub 网页和平板上操作，**没有本地开发环境**。所有编译、验证都靠 GitHub Actions。

## 硬性约束

- **技术栈**：Kotlin + Jetpack Compose + Material3；播放内核 **libmpv**（不用 ExoPlayer）；BT 内核 **TorrServer**（子进程）。
- **代码基础**：以 [marlboro-advance/mpvEx](https://github.com/marlboro-advance/mpvEx) 为起点（复制某个固定 commit 的源码进本仓库，在 README 记录来源 commit；不要用 git submodule）。复用它获取 libmpv 的方式（预编译 AAR 依赖或其构建脚本），**不要自己从源码编译 mpv**，除非它的方式在 CI 里跑不通。
- **包名** `com.wenjin.magnetplayer`，App 名称"磁力播放器"。
- **只打 arm64-v8a**，minSdk 26，targetSdk 用 mpvEx 当前的设置。
- **许可证**：GPL-3.0，保留 mpvEx / mpv-android / TorrServer 的版权声明，在"关于"页列出。
- **不留文件**：TorrServer 用内存缓存，大小可设（默认 300 MB）；关闭播放即移除种子；不提供"下载保存"。
- **不做找片**：不加资源搜索、Torznab、索引站等功能。
- **界面**：严格按 `docs/UI_SPEC.md`。浅色主题背景必须**纯白**；播放页永远深色、沉浸式，**全 App 不使用毛玻璃 / blur**。删除 mpvEx 中用不到的界面（本地媒体库浏览、SMB/FTP/WebDAV 等），但保留其播放器内核封装、手势、画中画等可复用的部分。
- **API 不要凭记忆**：TorrServer 接口以其仓库内文档 / Swagger 为准；mpv 属性和命令以 mpv 官方手册为准。

## 构建与发布（GitHub Actions）

- `.github/workflows/build.yml`：push 到 `main` 和 Pull Request 时编译 **release** APK（arm64-v8a），上传为 Actions 产物。
- 打 `v*` 标签时创建 GitHub Release，附上 APK。
- 签名：从仓库 Secrets 读取 `KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`；没配置时退回 debug 签名，保证始终能产出可安装的 APK。在 README 写清楚如何生成 keystore 并填入 Secrets（同一个签名才能覆盖安装）。
- TorrServer 二进制在 CI 中下载（版本号写在一个配置文件里，便于升级），放入 `jniLibs/arm64-v8a/libtorrserver.so`，校验 SHA-256。不要把二进制提交进仓库。
- 每次提交后用 `gh run watch` / `gh run view --log-failed` 查看 Actions 结果，**失败就修到绿灯为止**，不要留下红灯的 main。

## 工作方式

- 按 `docs/PLAN.md` 的里程碑 **M0 → M1 → M2 → M3 → M4** 顺序推进，每个里程碑一个或几个小提交，完成后更新 README 的进度清单。
- 每完成一个里程碑：确认 Actions 绿灯，在 Release 或 Actions 产物里给出 APK，并用中文写几句"这一版可以测试什么、怎么测"。
- 遇到需要用户在平板上实测才能判断的问题（例如能否开播、字幕是否显示），把要测的步骤写清楚，等用户反馈。
- 设计稿里的示例文字（示例视频、节点数、速度等）都是占位数据，实际显示真实数据。
- 第二期（AI 字幕）在用户确认第一期可用之后再做；第一期界面里它的入口显示"第二期"并置灰。

## 目录约定（建议）

```
app/src/main/java/com/wenjin/magnetplayer/
  torrent/      TorrServer 子进程、HTTP 客户端、仓库层
  player/       mpv 封装、播放页、手势、字幕
  home/         首页、解析结果、最近播放
  settings/     设置页
  data/         Room、DataStore
  ui/theme/     颜色 token（与 UI_SPEC 一一对应）、字体、形状
docs/           方案、规范、设计稿
```
