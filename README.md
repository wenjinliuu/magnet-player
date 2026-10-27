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

## 许可证

GPL-3.0。本项目使用的开源项目及其许可证见"关于"页。
