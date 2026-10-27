package com.wenjin.magnetplayer.torrent

/**
 * 内置 tracker 列表，写入 TorrServer 的 DefaultTrackers（RetrackersMode=1 时会追加到每个种子）。
 *
 * TorrServer 默认从 GitHub 上的 ngosang/trackerslist 拉取，国内经常拉不到，只剩它自带的十几个
 * （多数是俄罗斯的 retracker）。这里内置一份 trackers_best.txt 的快照（2026-10-27），
 * 能拉到远程列表时 TorrServer 会自动合并。
 */
object Trackers {
  val builtIn: List<String> = listOf(
    "udp://tracker.opentrackr.org:1337/announce",
    "udp://open.stealth.si:80/announce",
    "udp://open.demonii.com:1337/announce",
    "udp://open.tracker.cl:1337/announce",
    "udp://exodus.desync.com:6969/announce",
    "udp://tracker.torrent.eu.org:451/announce",
    "udp://tracker.skynetcloud.site:6969/announce",
    "udp://tracker.qu.ax:6969/announce",
    "udp://tracker.corpscorp.online:80/announce",
    "udp://tracker-udp.gbitt.info:80/announce",
    "udp://tracker.bittor.pw:1337/announce",
    "udp://retracker01-msk-virt.corbina.net:80/announce",
    "udp://tracker.tryhackx.org:6969/announce",
    "udp://explodie.org:6969/announce",
    "udp://tracker.ducks.party:1984/announce",
    "udp://tracker2.dler.org:80/announce",
    "udp://tracker.dler.org:6969/announce",
    "http://tracker.dler.com:6969/announce",
    "udp://tracker.nyaa.vc:6969/announce",
    "udp://tracker.peerfect.org:6969/announce",
    // TorrServer 自带的默认列表（server/settings/btsets.go）
    "http://retracker.local/announce",
    "http://bt4.t-ru.org/ann?magnet",
    "http://tracker2.itzmx.com:6961/announce",
    "udp://opentor.org:2710",
    "udp://public.popcorn-tracker.org:6969/announce",
    "wss://tracker.btorrent.xyz",
    "wss://tracker.openwebtorrent.com",
  ).distinct()

  val text: String get() = builtIn.joinToString("\n")
}
