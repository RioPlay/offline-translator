# Keyboard-accessible language swap

While the software keyboard is visible on the normal text screen, expose the existing language-swap action in a 48 dp button immediately above it. Keep the selectors in their existing composition location: relocating them when the IME opens would risk dismissing a language-search popup. The existing top control remains available. Image and modal screens retain their existing control layout.

The footer uses the same SwapLanguages message and availability/auto-detection rules as the top button. It reuses the existing localized accessibility description. IME padding and Scaffold bottom padding reserve space for the control instead of placing it over text. Keyboard visibility is reduced to a derived Boolean so IME animation does not force composition for every inset pixel.

## Completed local checks

The combined `0.8.6-test2` candidate passed Android compilation, all 90 JVM tests, full app ktlint, release NewApi lint at minSdk 23, minified ARM64 packaging, and APK signature/identity verification. The app retains the Translator launcher name and isolated dev.davidv.translator.devicetest app ID. SHA-256: `aea7c59c8a6d76fe99eae5fe04554dc3d3e75bca4826c772db83ccf8fd11bc65`.

These checks establish compilation and packaging, not keyboard placement, focus, accessibility or touch behavior on a device. The checks below remain open.

## Validation required on RioPlay's device

- Open/close the keyboard and swap repeatedly. Confirm the button is reachable above the keyboard, works while typing and does not dismiss the keyboard or lose the draft.
- Compare its direction change and disabled states with the existing top action, including missing reverse models and auto-detection without a known language.
- Test portrait, landscape, gesture navigation, larger font/display sizes and TalkBack; verify both text panes remain usable and the button does not overlap content or the keyboard.
- Open language search and type into its search field. Its popup must remain open during IME transitions.
- Check image translation and read/write/readonly popups for layout regressions.

Compilation/packaging checks do not establish these device outcomes. Related upstream context: https://github.com/DavidVentura/offline-translator/issues/339 and https://github.com/DavidVentura/offline-translator/issues/300 ; this focused control does not complete those broader requests. No upstream change/submission is authorized without RioPlay's explicit approval.
