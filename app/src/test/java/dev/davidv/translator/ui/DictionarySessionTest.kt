/*
 * Copyright (C) 2026 RioPlay
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See <https://www.gnu.org/licenses/>.
 */

package dev.davidv.translator.ui

import dev.davidv.translator.Language
import dev.davidv.translator.Script
import dev.davidv.translator.WordWithTaggedEntries
import dev.davidv.translator.singleScript
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExecutorCoroutineDispatcher
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DictionarySessionTest {
  private lateinit var ui: ExecutorCoroutineDispatcher
  private lateinit var worker: ExecutorCoroutineDispatcher
  private lateinit var scope: CoroutineScope
  private val gates = mutableListOf<CountDownLatch>()
  private val language = Language("es", "Spanish", "Spanish", Script.LATIN, singleScript(Script.LATIN), "es")

  @Before
  fun setUp() {
    ui = Executors.newSingleThreadExecutor { Thread(it, "dictionary-test-ui") }.asCoroutineDispatcher()
    worker = Executors.newSingleThreadExecutor { Thread(it, "dictionary-test-worker") }.asCoroutineDispatcher()
    scope = CoroutineScope(SupervisorJob() + ui)
  }

  @After
  fun tearDown() {
    gates.forEach { it.countDown() }
    scope.cancel()
    worker.close()
    ui.close()
  }

  private fun word(text: String) = WordWithTaggedEntries(text, 0, emptyList(), null, emptyList(), emptyList())

  private fun session(find: (Language, String) -> WordWithTaggedEntries?) = DictionarySession(scope, UiEvents(scope), worker, find)

  private fun gate() = CountDownLatch(1).also { gates += it }

  private suspend fun opened(
    session: DictionarySession,
    text: String,
  ): DictionaryState.Open =
    withTimeout(5_000) {
      session.state.filterIsInstance<DictionaryState.Open>().first { it.word.word == text }
    }

  private suspend fun drain() {
    withTimeout(5_000) {
      withContext(worker) { }
      withContext(ui) { }
    }
  }

  @Test
  fun `blocking native work does not block UI or reopen a closed sheet`() = runBlocking {
    val started = gate()
    val release = gate()
    val thread = AtomicReference<String>()
    val session =
      session { _, text ->
        thread.set(Thread.currentThread().name)
        started.countDown()
        check(release.await(5, TimeUnit.SECONDS))
        word(text)
      }
    withContext(ui) { session.lookup("slow", language) }
    assertTrue(started.await(5, TimeUnit.SECONDS))
    withTimeout(5_000) { withContext(ui) { session.clear() } }
    assertNotEquals("dictionary-test-ui", thread.get())
    release.countDown()
    drain()
    assertEquals(DictionaryState.Closed, session.state.value)
  }

  @Test
  fun `a newer tap replaces a pending lookup without stacking its late result`() = runBlocking {
    val started = gate()
    val release = gate()
    val session =
      session { _, text ->
        if (text == "slow") {
          started.countDown()
          check(release.await(5, TimeUnit.SECONDS))
        }
        word(text)
      }
    withContext(ui) { session.lookup("slow", language) }
    assertTrue(started.await(5, TimeUnit.SECONDS))
    withContext(ui) { session.lookup("new", language) }
    release.countDown()
    val result = opened(session, "new")
    assertEquals(emptyList<WordWithTaggedEntries>(), result.previous)
  }

  @Test
  fun `Back cancels pending work and returns to the previous completed word`() = runBlocking {
    val started = gate()
    val release = gate()
    val session =
      session { _, text ->
        if (text == "slow") {
          started.countDown()
          check(release.await(5, TimeUnit.SECONDS))
        }
        word(text)
      }
    withContext(ui) { session.lookup("first", language) }
    opened(session, "first")
    withContext(ui) { session.lookup("second", language) }
    assertEquals(listOf(word("first")), opened(session, "second").previous)
    withContext(ui) { session.lookup("slow", language) }
    assertTrue(started.await(5, TimeUnit.SECONDS))
    withContext(ui) { session.pop() }
    release.countDown()
    drain()
    assertEquals(DictionaryState.Open(language, word("first"), emptyList()), session.state.value)
  }

  @Test
  fun `disposing the owner ignores an in-flight native result`() = runBlocking {
    val started = gate()
    val release = gate()
    val session =
      session { _, text ->
        started.countDown()
        check(release.await(5, TimeUnit.SECONDS))
        word(text)
      }
    withContext(ui) { session.lookup("slow", language) }
    assertTrue(started.await(5, TimeUnit.SECONDS))
    withContext(ui) { scope.cancel() }
    release.countDown()
    drain()
    assertEquals(DictionaryState.Closed, session.state.value)
  }
}
