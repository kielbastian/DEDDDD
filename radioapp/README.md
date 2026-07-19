# Radio Fala (Android)

Aplikacja do słuchania radia internetowego z podziałem na kategorie
(Lata 80, Lata 90, Lata 2000, Rock, Pop, Dance/Disco, Polskie stacje),
ulubionymi i odtwarzaniem w tle – działa również przy zgaszonym ekranie.

## Skąd stacje?

Lista stacji pochodzi z otwartej, community'owej bazy
[Radio-Browser](https://www.radio-browser.info) (bez klucza API) –
ten sam duch, w jakim aplikacja Program TV korzysta z publicznego źródła
EPG. Kategorie odpowiadają popularnym tagom w tej bazie (`80s`, `90s`,
`2000s`, `rock`, `pop`, `dance`), a „Polskie stacje” filtrują po kodzie
kraju `PL`.

## Funkcje

- **Kategorie** – Ulubione, Polskie stacje, Lata 80/90/2000, Rock, Pop, Dance/Disco.
- **Ulubione** – gwiazdka przy stacji, zapisywana lokalnie (Room), działa offline jako lista.
- **Wyszukiwarka** – szukanie stacji po nazwie w całej bazie Radio-Browser.
- **Odtwarzanie w tle i przy zgaszonym ekranie** – usługa pierwszoplanowa
  (Media3/ExoPlayer + MediaSession) z powiadomieniem i sterowaniem na
  ekranie blokady; dźwięk nie przerywa się po wygaszeniu ekranu.
- **Timer snu** – 15/30/45/60 minut, po czym radio automatycznie się zatrzymuje.
- **Ładny odtwarzacz** – mini-player przyklejony u dołu ekranu, pełny
  odtwarzacz z animowanym korektorem i logo stacji (lub kolorowym
  awatarem z inicjałami, gdy stacja nie ma ikony).

## Budowanie

```bash
./gradlew :radioapp:assembleDebug
```

APK: `radioapp/build/outputs/apk/debug/radioapp-debug.apk`.
Minimalna wersja Androida: 8.0 (API 26).

## Struktura

```
radioapp/src/main/java/pl/radiofala/app/
├── MainActivity.kt          # szkielet aplikacji, nawigacja, uprawnienia
├── RadioViewModel.kt        # stan aplikacji (kategorie, wyszukiwanie, ulubione, timer snu)
├── data/
│   ├── RadioModels.kt       # RadioStation, kategorie, encja ulubionych
│   ├── FavoriteDao.kt       # zapytania Room dla ulubionych
│   ├── RadioDatabase.kt
│   └── RadioBrowserApi.kt   # klient publicznej bazy Radio-Browser
├── playback/
│   ├── RadioPlaybackService.kt  # usługa pierwszoplanowa (Media3), timer snu
│   └── PlayerConnection.kt      # łączy UI z usługą przez MediaController
└── ui/
    ├── Theme.kt, Components.kt (logo stacji, korektor)
    ├── Screens.kt           # kategorie, lista stacji, wyszukiwarka
    └── PlayerUi.kt          # mini-player i pełny odtwarzacz
```
