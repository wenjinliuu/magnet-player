# 技术方案与选型

## 目标

在**小米平板（安卓，横屏，arm64）**上，粘贴磁力链接后**直接在线播放**视频：

- 原画，不转码；边下边播，开头下几十 MB 就能开始看。
- **不留文件**：缓存放在 App 内部、限制大小，看完自动清空。
- 能拖进度条（拖到哪里优先下载哪里）。
- 字幕能力强：种子自带字幕、在线字幕、后续 AI 识别翻译（日语 → 中文）。
- 界面简约好看，适配平板，浅色 / 深色两种模式。
- 全部通过 **GitHub Actions 云端编译** APK，不依赖本地开发环境。

## 已定的选型（经过调研，均为该领域主流项目）

| 模块 | 选用 | 说明 |
|---|---|---|
| 磁力 / BT 内核 | [YouROK/TorrServer](https://github.com/YouROK/TorrServer)（Go，约 3.1k 星，GPL-3.0，持续活跃） | 专为"不预先下载、在线观看种子"设计；可调缓存大小；HTTP API 输出流地址；支持找出种子内的外挂字幕并转成 WebVTT。安卓上作为**子进程**运行，App 通过 `127.0.0.1` 的 HTTP 接口交互。 |
| 播放器内核 | **libmpv**（[mpv-android](https://github.com/mpv-android/mpv-android)，约 3.4k 星） | 格式最全（MKV / HEVC / AV1 / DTS / TrueHD），libass 渲染 ASS 特效字幕，支持**双字幕**（`secondary-sid`），为后续字幕功能打基础。**不用 ExoPlayer**：ASS 样式和双字幕都弱。 |
| App 界面基础 | [marlboro-advance/mpvEx](https://github.com/marlboro-advance/mpvEx)（约 2.0k 星，Jetpack Compose + Material3） | mpv-android 的现代化分支，自带手势、画中画、后台播放、外挂字幕、网络串流，以及 GitHub Actions 编译签名流程。**以它为代码基础**，删掉不需要的部分、换成我们的界面。 |
| 参考：玻璃风格播放器 | [mpvRex/REX-Player](https://github.com/mpvRex/REX-Player) | 基于 mpvEx。我们**不用毛玻璃**，只参考它的控件布局与手势实现。 |
| 参考：视频上叠加层 | [WindustH/mpvEx（mpvDanmaku）](https://github.com/WindustH/mpvEx) | 基于 mpvEx，实现了弹幕叠加层，并有从源码构建 mpv-android AAR 的脚本。**第二期 AI 字幕叠加层**参考它的做法。 |
| 参考：mpvKt | [abdallahmehiz/mpvKt](https://github.com/abdallahmehiz/mpvKt)（约 1.4k 星） | mpvEx 的前身。 |

> 星数为 2026 年 10 月调研时的数据。实施前请再确认各仓库的最新版本、许可证和构建方式。

### 许可证

TorrServer 是 GPL-3.0，本项目整体采用 **GPL-3.0**，并在 README / 关于页注明所用开源项目及其许可证。仅个人自用，不上架应用商店。

## 架构

```
┌────────────────────────── App（Kotlin + Jetpack Compose）──────────────────────────┐
│                                                                                    │
│  首页 UI ──粘贴磁力──► TorrentRepository ──HTTP──► TorrServer 子进程（127.0.0.1:8090） │
│     │                    │  添加种子 / 文件列表 / 状态 / 字幕列表 / 移除             │  │
│     │                    ▼                                                          │
│     └──点播放──► 播放页 UI ──► libmpv ◄──HTTP 流地址── TorrServer /stream            │
│                     │            ▲                                                  │
│                     │            └── sub-add（种子内字幕 / 在线字幕 / 本地字幕）       │
│                     └── 状态轮询（速度、节点、缓冲、已下载片段）                       │
│                                                                                    │
│  TorrServerService（前台服务）：负责启动 / 监控 / 停止子进程                          │
│  Room 数据库：最近播放（infohash + 文件序号 → 进度、标题、时长）                      │
│  DataStore：设置（主题、缓存上限、自动清空、字幕偏好、API Key 等）                    │
└────────────────────────────────────────────────────────────────────────────────────┘
```

### TorrServer 嵌入方式

1. CI 中从 TorrServer 的 GitHub Releases 下载 **android arm64** 二进制（实施时先确认 release 资产的确切名称；若没有现成安卓构建，则在 CI 里用 Go + Android NDK 交叉编译）。
2. 重命名为 `libtorrserver.so` 放进 `jniLibs/arm64-v8a/`，并在 Manifest 设置 `android:extractNativeLibs="true"`。这样它会被解压到 `nativeLibraryDir`，在 Android 10+ 上也能执行（应用数据目录不允许执行文件）。
3. `TorrServerService`（前台服务，类型 `mediaPlayback`）用 `ProcessBuilder` 启动：
   `libtorrserver.so --port 8090 --ip 127.0.0.1 --path <filesDir>/torrserver`，只监听本机。
4. 启动后轮询 `/echo` 确认就绪；App 退出或空闲时停止。
5. 首次启动通过 `/settings` 写入设置：**内存缓存（不落盘）**、缓存大小 = 用户设置的上限、预加载比例适中、关闭 DLNA 等不需要的功能。

### TorrServer API（实施时以其 Swagger 文档 `/swagger/index.html` 为准，不要凭记忆）

- 添加种子 / 获取文件列表与状态 / 移除：`POST /torrents`（`action`: `add` / `get` / `list` / `rem` / `drop`）。
- 播放流地址：`/stream?link=<磁力或infohash>&index=<文件序号>&play`。
- 种子内外挂字幕：`GET /subs/{hash}/{id}` 列出，`?track=<n>` 取单个（默认 WebVTT，可 `format=original` 拿原始 ASS）。
- 设置：`POST /settings`。

### 不留文件

- TorrServer 用**内存缓存**，大小 = 缓存上限（默认 300 MB）。
- 关闭播放页时：保存进度 → `drop` 当前种子；若开启"看完自动清空"，再清理 TorrServer 数据目录里该种子的残留。
- 设置页提供"立即清空全部缓存"。
- 种子元数据不长期保存，最近播放只存 infohash、磁力链接、文件序号、标题、进度。

## 字幕路线

按优先级：

1. **种子自带字幕**：TorrServer `/subs` 接口 → mpv `sub-add`，打开视频时自动加载，优先选中文。
2. **从平板选择字幕文件**：系统文件选择器 → 复制到缓存目录 → `sub-add`。
3. **在线字幕**（第一期后半）：
   - 伪射手 assrt.net：按片名搜索，需在设置中填写 API Token。
   - 迅雷：按视频文件特征（CID）精确匹配，需要读取文件头尾若干数据块（通过 TorrServer 流按 Range 读取）。
4. **AI 识别并翻译（第二期）**：
   - 在播放位置**前方**持续截取音频（例如每段 60 秒），用 ffmpeg 从 TorrServer 流提取 16kHz 单声道音频。
   - 发送到 **Groq Whisper Large v3 Turbo**（约 216 倍实时速度，0.04 美元 / 小时音频，免费额度每天 2000 次请求），语言设为日语。
   - 识别结果交给大模型翻译成中文（接口用 **OpenAI 兼容格式**，地址、模型、Key 都可配置，方便换成国内服务，例如 DeepSeek）。
   - 生成双语 SRT，通过 mpv `sub-add` / `sub-reload` 加载；或参考 mpvDanmaku 的叠加层自己绘制。
   - 注意：Groq 在国内可能需要代理，接口地址必须可配置。
   - 离线备选：[k2-fsa/sherpa-onnx](https://github.com/k2-fsa/sherpa-onnx)（支持安卓离线识别，含 SenseVoice 中英日韩粤模型），作为没网 / 不想用云服务时的选项。

## 里程碑

| 阶段 | 内容 | 验收标准 |
|---|---|---|
| **M0 地基** | 导入 mpvEx 代码并改包名、改名；GitHub Actions 编译出 arm64 APK | Actions 绿灯，产物 APK 装到平板上能播放一个 HTTP 视频地址 |
| **M1 磁力播放** | 嵌入 TorrServer；粘贴磁力 → 文件列表 → 播放；拖动进度；缓存上限与自动清空 | 一个热门磁力链接 30 秒内开播；拖到中间能继续播；关闭后缓存清空 |
| **M2 首页** | 按 `UI_SPEC.md` 实现首页（浅色 / 深色）、最近播放（Room）、剪贴板识别、.torrent 文件 | 与设计稿一致；最近播放能从上次位置继续 |
| **M3 播放页** | 沉浸式播放页、彩色控件、已下载片段进度条、状态显示、手势、字幕面板、种子内字幕自动加载、本地字幕、双语、字号、偏移 | 与设计稿一致；种子带字幕时自动显示 |
| **M4 在线字幕** | 伪射手搜索、迅雷 CID 匹配 | 能为常见视频搜到并加载字幕 |
| **第二期** | AI 识别并翻译字幕 | 日语视频能在播放前方几分钟内生成中文字幕 |

## 不做的事

- 不做资源搜索、索引站、Torznab 等"找片"功能，只负责播放用户自己粘贴的链接。
- 不提供"下载到本地保存"。
- 不上架应用商店，不做账号系统，不做统计上报。
