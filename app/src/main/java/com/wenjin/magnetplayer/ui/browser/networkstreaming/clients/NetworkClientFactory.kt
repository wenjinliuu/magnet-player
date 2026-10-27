package com.wenjin.magnetplayer.ui.browser.networkstreaming.clients

import com.wenjin.magnetplayer.domain.network.NetworkConnection
import com.wenjin.magnetplayer.domain.network.NetworkProtocol

object NetworkClientFactory {
  fun createClient(connection: NetworkConnection): NetworkClient =
    when (connection.protocol) {
      NetworkProtocol.SMB -> SmbClient(connection)
      NetworkProtocol.FTP -> FtpClient(connection)
      NetworkProtocol.WEBDAV -> WebDavClient(connection)
    }
}
