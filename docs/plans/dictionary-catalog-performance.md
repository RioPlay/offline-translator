# Dictionary and catalog follow-up

Native dictionary lookup previously ran synchronously from the UI action. It now runs on Dispatchers.IO in the ViewModel's scope. A mutex serializes these blocking calls; cancellation cannot interrupt a native call already running, so a request generation also prevents obsolete results/errors from replacing a newer selection or reopening a dismissed sheet. Back, clear and ViewModel disposal invalidate pending work. Completed dictionary history is preserved.

Language asset rows now reuse localized search keys and sorted voice partitions instead of rebuilding them during each search or unrelated recomposition. Catalog and configuration locale remain cache keys, so refreshing the catalog or changing the display language invalidates the relevant cached values.

## Validation

The candidate containing these changes and the keyboard swap control compiled with the baseline Android/Kotlin configuration. All 90 JVM tests passed, including four new dictionary regression tests covering a blocked native call, a newer selection, Back/history and owner disposal. Tests inject a blocking loader and separate UI/worker executors; they do not benchmark the native dictionary or prove device frame times. The combined lint invocation exposed an existing generated-source task ordering problem, addressed in the separate build-tool update.

On device: open dictionaries repeatedly, follow entry links and navigate Back, dismiss during lookup, and leave/re-enter the screen. Search and scroll the language manager, switch locale, and install/remove a pack to check cache invalidation. Compare warm and cold interactions before attributing battery or frame-time improvements. No new claim of measured performance or battery improvement is made here.

Related UI context: https://github.com/DavidVentura/offline-translator/issues/300 and https://github.com/DavidVentura/offline-translator/issues/339 . These changes do not complete those broader requests. All proposed changes remain in RioPlay's fork pending device testing and explicit permission for any upstream submission.
