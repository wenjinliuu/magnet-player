# 磁力播放器 magnet-player

安卓平板上的磁力链接在线播放器：粘贴磁力链接，原画边下边播，看完不留文件。

- BT 内核：[TorrServer](https://github.com/YouROK/TorrServer)
- 播放内核：libmpv（[mpv-android](https://github.com/mpv-android/mpv-android)）
- 界面基础：[mpvEx](https://github.com/marlboro-advance/mpvEx)（Jetpack Compose）

## 文档

- [技术方案与选型](docs/PLAN.md)
- [界面规范](docs/UI_SPEC.md)
- [设计稿](docs/design/index.html)（下载仓库后用浏览器打开）
- [实施指南（给 Claude）](CLAUDE.md)

## 进度

- [ ] M0 地基：导入 mpvEx，云端编译出 APK
- [ ] M1 磁力播放：嵌入 TorrServer，粘贴即播
- [ ] M2 首页：浅色 / 深色、最近播放
- [ ] M3 播放页：沉浸式控件、字幕面板、种子内字幕
- [ ] M4 在线字幕
- [ ] 第二期：AI 识别并翻译字幕

## 代码来源

| 项目 | 版本 | 用法 |
|---|---|---|
| [marlboro-advance/mpvEx](https://github.com/marlboro-advance/mpvEx) | commit `f4ea63e63ea33653b98426d075199260f9d27f68`（2026-10-24） | 源码复制进本仓库后改包名、删减、换界面。原许可证 Apache-2.0，见 `LICENSE-mpvEx-Apache-2.0` |
| libmpv | Maven 依赖 `io.github.marlboro-advance:mpv-android`（mpvEx 预编译的 AAR） | 播放内核，不在本仓库编译 |
| [YouROK/TorrServer](https://github.com/YouROK/TorrServer) | 见 `torrserver.properties` | CI 下载官方 release 的 android-arm64 二进制，校验 SHA-256 后打包成 `libtorrserver.so` |

## 下载 APK

- 每次推送后，在仓库的 **Actions → 编译 APK →** 对应那次运行的页面底部 **Artifacts** 里下载（zip 里是 APK）。
- 打 `v*` 标签时，会自动创建 GitHub Release 并附上 APK。

## 签名（同一个签名才能覆盖安装）

没有配置签名时，CI 用仓库里固定的 `app/debug.keystore` 签名，也能一直覆盖安装。
想换成自己的正式签名（一次性操作，换签名后需要先卸载旧版本）：

1. 在任意一台装了 Java 的电脑上生成 keystore（或在 GitHub Codespaces 的终端里运行）：
   ```bash
   keytool -genkeypair -v -keystore release.keystore -alias magnet \
     -keyalg RSA -keysize 2048 -validity 36500
   ```
   按提示设置密码。
2. 转成 base64：`base64 -w0 release.keystore > keystore.txt`（macOS 用 `base64 -i release.keystore -o keystore.txt`）。
3. 打开仓库 **Settings → Secrets and variables → Actions → New repository secret**，新建 4 个：
   - `KEYSTORE_BASE64`：keystore.txt 的全部内容
   - `KEYSTORE_PASSWORD`：keystore 密码
   - `KEY_ALIAS`：上面的 `magnet`
   - `KEY_PASSWORD`：key 密码（没单独设置就和 keystore 密码相同）
4. 妥善保存 `release.keystore` 和密码，丢了就无法再覆盖安装。

## 许可证

GPL-3.0（见 `LICENSE`）。本项目使用的开源项目及其许可证见 App 内"关于"页：

- mpvEx — Apache-2.0
- mpv / mpv-android — LGPL-2.1+ / MIT
- TorrServer — GPL-3.0
- FFmpeg — LGPL-2.1+
