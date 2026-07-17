# Program TV (Android)

Aplikacja na Androida z programem telewizyjnym wybranych stacji oraz
wyszukiwarką filmów i programów. Wpisujesz tytuł (np. „Shrek”), a aplikacja
pokazuje, **na jakim kanale, którego dnia i o której godzinie** dana pozycja
będzie emitowana.

## Funkcje

- **Teraz** – co aktualnie leci na wybranych przez Ciebie stacjach
  (z paskiem postępu trwającej audycji).
- **Program** – pełna ramówka wybranego kanału na najbliższe dni,
  pogrupowana według dni („Dzisiaj”, „Jutro”, …).
- **Szukaj** – wyszukiwarka po tytule filmu/programu; wyniki obejmują
  wszystkie kanały i pokazują kanał, datę oraz godziny emisji.
- **Kanały** – lista wszystkich stacji ze źródła danych; zaznaczasz te,
  które Cię interesują (z polem filtrowania).
- Ręczne odświeżanie danych przyciskiem w górnym pasku; przy pierwszym
  uruchomieniu dane pobierają się automatycznie.
- Cały interfejs po polsku, motyw jasny i ciemny (wg ustawień systemu).

## Skąd pochodzą dane o programie TV?

Aplikacja korzysta z plików w standardzie **XMLTV** – otwartym formacie,
w którym publikowana jest większość darmowych źródeł EPG (Electronic
Program Guide). Domyślnie ustawione jest społecznościowe źródło z polskimi
kanałami:

```
https://epg.ovh/pl.xml
```

Adres można zmienić w aplikacji (ikona ⚙ w górnym pasku), np. na:

- własny plik wygenerowany narzędziem [WebGrab+Plus](http://www.webgrabplus.com/),
- dowolne inne źródło XMLTV (także spakowane gzipem – aplikacja
  rozpoznaje to automatycznie).

Dane zapisywane są lokalnie w bazie Room, więc po pobraniu program TV
i wyszukiwarka działają również offline.

## Budowanie

Wymagane: **Android Studio** (Koala lub nowsze) albo Android SDK
(compileSdk 34) + JDK 17.

```bash
./gradlew assembleDebug
```

Gotowy plik APK znajdziesz w `app/build/outputs/apk/debug/`.
Minimalna wersja Androida: **8.0 (API 26)**.

## Technologie

- Kotlin + Jetpack Compose (Material 3)
- Room (lokalna baza kanałów i ramówki)
- OkHttp (pobieranie pliku XMLTV, obsługa gzip)
- Kotlinx Coroutines / Flow
- Architektura MVVM (`AppViewModel` + repozytorium `EpgRepository`)

## Struktura projektu

```
app/src/main/java/pl/programtv/app/
├── MainActivity.kt          # szkielet aplikacji, nawigacja, ustawienia
├── AppViewModel.kt          # stan aplikacji (MVVM)
├── data/
│   ├── Entities.kt          # encje Room: kanały i pozycje programu
│   ├── EpgDao.kt            # zapytania (teraz / ramówka / wyszukiwanie)
│   ├── EpgDatabase.kt       # baza Room
│   ├── XmltvParser.kt       # parser formatu XMLTV
│   └── EpgRepository.kt     # pobieranie i zapis danych EPG
└── ui/
    ├── Screens.kt           # ekrany: Teraz, Program, Szukaj, Kanały
    └── Format.kt            # formatowanie dat i godzin po polsku
```
