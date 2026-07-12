# Kalkulator CNC — aplikacja Android

Natywna aplikacja Android opakowująca kalkulator CNC (stożki Morse'a, pasowania ISO 286,
identyfikacja gwintów, gwintowanie sztywne G84, toczenie gwintów G76/G92). Kalkulator
działa w pełni offline — HTML jest wbudowany w aplikację (`app/src/main/assets/index.html`).

## Jak zdobyć APK

**Opcja 1 — GitHub Actions (bez instalowania czegokolwiek):**
Po każdym pushu workflow *Build APK* buduje aplikację. Wejdź w zakładkę **Actions** →
wybierz ostatni przebieg → pobierz artefakt **KalkulatorCNC-debug-apk** → wypakuj
`app-debug.apk` i zainstaluj na telefonie (trzeba zezwolić na instalację z nieznanych źródeł).

**Opcja 2 — Android Studio:**
Otwórz ten katalog w Android Studio i kliknij **Run**, albo z terminala:

```
./gradlew assembleDebug
```

APK pojawi się w `app/build/outputs/apk/debug/app-debug.apk`.

## Szczegóły techniczne

- WebView z `WebViewAssetLoader` (bezpieczny kontekst `https://appassets.androidplatform.net`)
- Natywny mostek do schowka — przyciski „Kopiuj" G-kodu działają w aplikacji
- Ciemny motyw dopasowany do kalkulatora (`#0e1216`), stan formularzy przeżywa obrót ekranu
- minSdk 26 (Android 8.0+), targetSdk 35

## Aktualizacja kalkulatora

Podmień plik `app/src/main/assets/index.html` na nową wersję i zbuduj ponownie.
