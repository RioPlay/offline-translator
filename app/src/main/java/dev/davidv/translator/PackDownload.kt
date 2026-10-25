/*
 * Copyright (C) 2024 David V
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.davidv.translator

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream

private const val DOWNLOAD_CONNECT_TIMEOUT_MS = 30_000
private const val DOWNLOAD_READ_TIMEOUT_MS = 30_000
private const val DOWNLOAD_BUFFER_BYTES = 16 * 1024

// The catalog's size is a progress estimate; Content-Length describes this response's wire bytes.
// Only publish a complete response, keeping an existing catalog/file intact on failure.
internal fun downloadPackFile(
  url: String,
  target: File,
  gunzip: Boolean,
  checkActive: () -> Unit,
  onRead: (Int) -> Unit,
  connectTimeoutMs: Int = DOWNLOAD_CONNECT_TIMEOUT_MS,
  readTimeoutMs: Int = DOWNLOAD_READ_TIMEOUT_MS,
) {
  val tempFile = File(target.parentFile, "${target.name}.tmp")
  val connection = URL(url).openConnection()
  try {
    checkActive()
    connection.connectTimeout = connectTimeoutMs
    connection.readTimeout = readTimeoutMs
    // Android otherwise transparently decompresses HTTP gzip, hiding the wire length from us.
    // A .gz pack is still explicitly decoded below, independently of HTTP content encoding.
    connection.setRequestProperty("Accept-Encoding", "identity")
    var received = 0L
    connection.getInputStream().use { raw ->
      val counted =
        CountingInputStream(raw) { bytes ->
          received += bytes
          onRead(bytes)
        }
      val decoded = if (gunzip) GZIPInputStream(counted) else counted
      decoded.use { input ->
        tempFile.outputStream().use { output ->
          val buffer = ByteArray(DOWNLOAD_BUFFER_BYTES)
          while (true) {
            checkActive()
            val read = input.read(buffer)
            if (read == -1) break
            output.write(buffer, 0, read)
          }
        }
      }
    }
    // Parse the header directly: URLConnection.contentLengthLong requires API 24.
    val contentLength = connection.getHeaderField("Content-Length")?.toLongOrNull()
    if (contentLength != null && contentLength >= 0 && received != contentLength) {
      throw IOException("Incomplete download: expected $contentLength bytes, received $received")
    }
    checkActive()
    if (!tempFile.renameTo(target)) {
      throw IOException("Failed to move $tempFile to $target")
    }
  } finally {
    (connection as? HttpURLConnection)?.disconnect()
    tempFile.delete()
  }
}

private class CountingInputStream(
  private val inputStream: InputStream,
  private val onRead: (Int) -> Unit,
) : InputStream() {
  override fun read(): Int {
    val byte = inputStream.read()
    if (byte != -1) onRead(1)
    return byte
  }

  override fun read(
    b: ByteArray,
    off: Int,
    len: Int,
  ): Int {
    val bytesRead = inputStream.read(b, off, len)
    if (bytesRead > 0) onRead(bytesRead)
    return bytesRead
  }

  override fun close() = inputStream.close()
}
