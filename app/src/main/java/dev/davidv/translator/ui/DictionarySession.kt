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

package dev.davidv.translator.ui

import android.util.Log
import dev.davidv.translator.Language
import dev.davidv.translator.LanguageCatalog
import dev.davidv.translator.WordWithTaggedEntries
import dev.davidv.translator.localizedName
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

sealed interface DictionaryState {
  data object Closed : DictionaryState

  // Following a word inside an entry stacks it on the previous one so back returns to it.
  data class Open(
    val language: Language,
    val word: WordWithTaggedEntries,
    val previous: List<WordWithTaggedEntries>,
  ) : DictionaryState {
    val stack: List<WordWithTaggedEntries> get() = previous + word
  }

  fun pushed(
    language: Language,
    word: WordWithTaggedEntries,
  ): Open =
    when (this) {
      Closed -> Open(language, word, emptyList())
      is Open -> Open(language, word, stack)
    }

  fun popped(): DictionaryState =
    when (this) {
      Closed -> Closed
      is Open -> previous.lastOrNull()?.let { copy(word = it, previous = previous.dropLast(1)) } ?: Closed
    }
}

class DictionarySession internal constructor(
  private val scope: CoroutineScope,
  private val uiEvents: UiEvents,
  private val lookupDispatcher: CoroutineDispatcher,
  private val findWord: (Language, String) -> WordWithTaggedEntries?,
) {
  constructor(
    catalog: StateFlow<LanguageCatalog>,
    uiEvents: UiEvents,
    scope: CoroutineScope,
  ) : this(scope, uiEvents, Dispatchers.IO, { language, word -> catalog.value.lookupDictionary(language, word) })

  private val _state = MutableStateFlow<DictionaryState>(DictionaryState.Closed)
  val state: StateFlow<DictionaryState> = _state.asStateFlow()
  private val lookupLock = Mutex()
  private var lookupJob: Job? = null
  private var requestId = 0L

  fun lookup(
    str: String,
    language: Language,
  ) {
    cancelLookup()
    val id = requestId
    lookupJob = scope.launch {
      val foundWord =
        try {
          // Native lookup is blocking. Serialize it away from the UI thread; cancelling
          // a coroutine does not interrupt an already executing native call.
          withContext(lookupDispatcher) { lookupLock.withLock { findWord(language, str) } }
        } catch (e: uniffi.bindings.CatalogException.MissingAsset) {
          if (!currentCoroutineContext().isActive || id != requestId) return@launch
          Log.i("DictionaryLookup", "No dictionary for ${language.displayName}", e)
          uiEvents.send(UiEvent.ShowToast("No ${language.localizedName()} dictionary installed"))
          return@launch
        } catch (e: uniffi.bindings.CatalogException.Other) {
          if (!currentCoroutineContext().isActive || id != requestId) return@launch
          Log.w("DictionaryLookup", "Lookup failed for ${language.displayName}", e)
          null
        }
      if (!currentCoroutineContext().isActive || id != requestId) return@launch
      if (foundWord == null) {
        uiEvents.send(UiEvent.ShowToast("'$str' not found in ${language.code} dictionary"))
        Log.w("DictionaryLookup", "Lookup failed for ${language.displayName}")
        return@launch
      }
      _state.update { it.pushed(language, foundWord) }
    }
  }

  // Called on the session's UI scope, including Back and sheet dismissal. A late
  // native result must never reopen the sheet or overwrite a newer word.
  private fun cancelLookup() {
    requestId++
    lookupJob?.cancel()
    lookupJob = null
  }

  fun pop() {
    cancelLookup()
    _state.update { it.popped() }
  }

  fun clear() {
    cancelLookup()
    _state.value = DictionaryState.Closed
  }
}
