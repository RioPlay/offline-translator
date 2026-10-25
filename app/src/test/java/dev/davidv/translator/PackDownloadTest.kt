/*
 * Copyright (C) 2026 RioPlay
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.davidv.translator

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.concurrent.CancellationException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPOutputStream

class PackDownloadTest {
  @get:Rule
  val folder = TemporaryFolder()

  private val target: File get() = File(folder.root, "pack")
  private val temporary: File get() = File(folder.root, "pack.tmp")

  // Use actual HTTP framing: a fake InputStream cannot reproduce a short HTTP response.
  private fun serve(
    response: ByteArray,
    stall: Boolean = false,
    action: (String) -> Unit,
  ) {
    val release = CountDownLatch(1)
    val executor = Executors.newSingleThreadExecutor()
    ServerSocket(0, 1, InetAddress.getByName("127.0.0.1")).use { server ->
      server.soTimeout = 5_000
      val worker =
        executor.submit {
          server.accept().use { socket ->
            socket.soTimeout = 5_000
            val reader = socket.getInputStream().bufferedReader()
            val headers = mutableListOf<String>()
            while (true) {
              val line = reader.readLine() ?: break
              if (line.isEmpty()) break
              headers += line
            }
            assertTrue(headers.any { it.equals("Accept-Encoding: identity", ignoreCase = true) })
            socket.getOutputStream().apply {
              write(response)
              flush()
            }
            if (stall) assertTrue(release.await(5, TimeUnit.SECONDS))
          }
        }
      try {
        action("http://127.0.0.1:${server.localPort}/pack")
      } finally {
        release.countDown()
        try {
          worker.get(6, TimeUnit.SECONDS)
        } finally {
          executor.shutdownNow()
        }
      }
    }
  }

  private fun response(
    body: ByteArray,
    length: Int = body.size,
    status: String = "200 OK",
  ): ByteArray = "HTTP/1.1 $status\r\nContent-Length: $length\r\nConnection: close\r\n\r\n".toByteArray() + body

  private fun download(
    url: String,
    gunzip: Boolean = false,
    checkActive: () -> Unit = {},
    onRead: (Int) -> Unit = {},
  ) = downloadPackFile(url, target, gunzip, checkActive, onRead, readTimeoutMs = 200)

  private fun assertPreserved() {
    assertEquals("existing", target.readText())
    assertFalse(temporary.exists())
  }

  @Test
  fun `complete response replaces existing file and reports wire bytes`() {
    target.writeText("existing")
    var received = 0
    serve(response("new pack".toByteArray())) { url -> download(url, onRead = { received += it }) }
    assertEquals("new pack", target.readText())
    assertEquals(8, received)
    assertFalse(temporary.exists())
  }

  @Test
  fun `truncated response preserves existing file and removes partial download`() {
    target.writeText("existing")
    serve(response("short".toByteArray(), length = 100)) { url ->
      assertThrows(IOException::class.java) { download(url) }
    }
    assertPreserved()
  }

  @Test
  fun `failed first download does not publish a new file`() {
    serve(response("short".toByteArray(), length = 100)) { url ->
      assertThrows(IOException::class.java) { download(url) }
    }
    assertFalse(target.exists())
    assertFalse(temporary.exists())
  }

  @Test
  fun `HTTP failure preserves existing file`() {
    target.writeText("existing")
    serve(response("unavailable".toByteArray(), status = "503 Service Unavailable")) { url ->
      assertThrows(IOException::class.java) { download(url) }
    }
    assertPreserved()
  }

  @Test
  fun `stalled body times out and removes partial download`() {
    target.writeText("existing")
    serve(response("short".toByteArray(), length = 100), stall = true) { url ->
      assertThrows(SocketTimeoutException::class.java) { download(url) }
    }
    assertPreserved()
  }

  @Test
  fun `stalled response headers time out`() {
    target.writeText("existing")
    serve(byteArrayOf(), stall = true) { url ->
      assertThrows(SocketTimeoutException::class.java) { download(url) }
    }
    assertPreserved()
  }

  @Test
  fun `chunked response remains supported without Content-Length`() {
    val response = "HTTP/1.1 200 OK\r\nTransfer-Encoding: chunked\r\nConnection: close\r\n\r\n4\r\npack\r\n0\r\n\r\n"
    serve(response.toByteArray()) { download(it) }
    assertEquals("pack", target.readText())
    assertFalse(temporary.exists())
  }

  @Test
  fun `connection close response remains supported without Content-Length`() {
    serve("HTTP/1.1 200 OK\r\nConnection: close\r\n\r\npack".toByteArray()) { download(it) }
    assertEquals("pack", target.readText())
  }

  @Test
  fun `gzip validates compressed wire length and reports compressed bytes`() {
    val payload = ByteArray(80_000) { (it % 17).toByte() }
    val compressed =
      ByteArrayOutputStream()
        .apply {
          GZIPOutputStream(this).use { it.write(payload) }
        }.toByteArray()
    var received = 0
    serve(response(compressed)) { url -> download(url, gunzip = true, onRead = { received += it }) }
    assertArrayEquals(payload, target.readBytes())
    assertEquals(compressed.size, received)
    assertFalse(temporary.exists())
  }

  @Test
  fun `corrupt gzip preserves existing file`() {
    target.writeText("existing")
    serve(response("not gzip".toByteArray())) { url ->
      assertThrows(IOException::class.java) { download(url, gunzip = true) }
    }
    assertPreserved()
  }

  @Test
  fun `truncated gzip preserves existing file`() {
    target.writeText("existing")
    val compressed =
      ByteArrayOutputStream()
        .apply {
          GZIPOutputStream(this).use { it.write("pack".toByteArray()) }
        }.toByteArray()
        .dropLast(4)
        .toByteArray()
    serve(response(compressed)) { url ->
      assertThrows(IOException::class.java) { download(url, gunzip = true) }
    }
    assertPreserved()
  }

  @Test
  fun `cancellation observed after EOF does not replace existing file`() {
    target.writeText("existing")
    var hasRead = false
    var checksAfterRead = 0
    serve(response("pack".toByteArray())) { url ->
      assertThrows(CancellationException::class.java) {
        download(
          url,
          checkActive = {
            // First check is before the EOF read, second is before publishing.
            if (hasRead && ++checksAfterRead == 2) throw CancellationException()
          },
          onRead = { hasRead = true },
        )
      }
    }
    assertPreserved()
  }

  @Test
  fun `cancellation after final read does not publish file`() {
    target.writeText("existing")
    var cancelled = false
    serve(response("pack".toByteArray())) { url ->
      assertThrows(CancellationException::class.java) {
        download(
          url,
          checkActive = { if (cancelled) throw CancellationException() },
          onRead = { cancelled = true },
        )
      }
    }
    assertPreserved()
  }
}
