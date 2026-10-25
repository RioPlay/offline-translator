/*
 * Copyright (C) 2026 RioPlay
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.davidv.translator

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.PrintWriter
import java.io.StringWriter

class FailureDiagnosticsTest {
  private fun rendered(error: Throwable): String = StringWriter().also { error.printStackTrace(PrintWriter(it)) }.toString()

  @Test
  fun `rendered diagnostics exclude messages throughout the failure graph`() {
    val secrets = listOf("PRIVATE_TRANSLATION", "PRIVATE_QUERY_TOKEN", "PRIVATE_DOCUMENT.pdf")
    val original =
      IllegalStateException(
        secrets[0],
        IllegalArgumentException("https://example.invalid/?token=${secrets[1]}"),
      ).apply {
        addSuppressed(IllegalStateException("/private/${secrets[2]}"))
      }
    val originalLog = rendered(original)
    secrets.forEach { assertTrue(originalLog.contains(it)) }

    val diagnostic = redactedException(original)
    val safeLog = rendered(diagnostic)
    secrets.forEach { assertFalse(safeLog.contains(it)) }
    assertTrue(safeLog.contains(original.javaClass.name))
    assertArrayEquals(original.stackTrace, diagnostic.stackTrace)
    assertNull(diagnostic.cause)
    assertTrue(diagnostic.suppressed.isEmpty())
  }

  @Test
  fun `redaction leaves the original error available to UI and callbacks`() {
    val cause = IllegalArgumentException("private cause")
    val suppressed = IllegalArgumentException("private suppressed")
    val original = IllegalStateException("private message", cause).apply { addSuppressed(suppressed) }
    val originalTrace = original.stackTrace
    val diagnostic = redactedException(original)
    diagnostic.stackTrace = emptyArray()

    assertEquals("private message", original.message)
    assertSame(cause, original.cause)
    assertSame(suppressed, original.suppressed.single())
    assertArrayEquals(originalTrace, original.stackTrace)
  }

  @Test
  fun `redaction never evaluates original message or string conversion`() {
    val original =
      object : RuntimeException() {
        override val message: String get() = error("Message must not be read")

        override fun toString(): String = error("String conversion must not be called")
      }
    val diagnostic = redactedException(original)
    assertEquals(original.javaClass.name, diagnostic.message)
    assertTrue(rendered(diagnostic).contains(original.javaClass.name))
  }

  @Test
  fun `cyclic cause and suppression graphs are not traversed`() {
    val first = IllegalStateException("private first")
    val second = IllegalStateException("private second")
    first.initCause(second)
    second.initCause(first)
    first.addSuppressed(second)
    second.addSuppressed(first)

    val diagnostic = redactedException(first)
    val safeLog = rendered(diagnostic)
    assertFalse(safeLog.contains("private"))
    assertFalse(safeLog.contains("CIRCULAR"))
    assertNull(diagnostic.cause)
    assertTrue(diagnostic.suppressed.isEmpty())
  }
}
