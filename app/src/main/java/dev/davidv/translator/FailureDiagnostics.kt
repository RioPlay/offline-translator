/*
 * Copyright (C) 2026 RioPlay
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package dev.davidv.translator

/**
 * Preserve the failure type and code locations for Logcat without logging content carried by
 * exception messages, causes or suppressed exceptions. UI/callback errors retain the original.
 * Only code-generated stack frames are retained; callers must also use a content-free log label.
 */
internal fun redactedException(error: Throwable): Throwable =
  Throwable(error.javaClass.name).apply {
    stackTrace = error.stackTrace
  }
