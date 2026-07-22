# Mi Box IPTV — natywna aplikacja IPTV/VOD dla Android TV

Szkielet natywnej aplikacji IPTV/VOD dedykowanej dla **Xiaomi Mi Box S / Mi TV Box 4**
(2 GB RAM, Amlogic, pilot z D-Padem). Nacisk na niskie zużycie RAM, akcelerację
sprzętową odtwarzania i nawigację pilotem.

> **Pełna specyfikacja techniczna i architektura:** [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)

## Stos technologiczny
- **Kotlin** + Coroutines/Flow
- **Jetpack Compose for TV** (`androidx.tv:tv-material`) — fokus D-Pad
- **Media3 / ExoPlayer** — dekodowanie sprzętowe (HEVC/AV1/Dolby Vision), passthrough audio, AFR
- **Room** (SQLite, indeksy) + **Paging 3** — wielkie playlisty i EPG
- **Retrofit + OkHttp + kotlinx.serialization** — Xtream Codes API
- **XmlPullParser** — strumieniowe parsowanie EPG XMLTV
- **Hilt** — wstrzykiwanie zależności
- **WorkManager** — synchronizacja w tle
- **Coil** — loga kanałów z twardym limitem cache pamięci

## Struktura modułów (Clean Architecture)
```
:app      Presentation (Compose for TV) + kompozycja DI
:domain   Czysty Kotlin — modele, kontrakty repozytoriów, use case'y
:data     Room, Retrofit (Xtream), parsery M3U/XMLTV, implementacje repo
:core     Wspólne narzędzia (dispatchery)
```
Kierunek zależności: `:app → :domain ← :data`, `:core` bez zależności.

## Kluczowe elementy szkieletu
| Funkcja | Plik |
|---|---|
| Konfiguracja ExoPlayera (HW, AFR, passthrough, niskie bufory) | `app/.../presentation/player/PlayerManager.kt` |
| Strumieniowy parser M3U | `data/.../parser/M3uParser.kt` |
| Strumieniowy parser EPG XMLTV | `data/.../parser/XmltvParser.kt` |
| Xtream Codes API | `data/.../remote/xtream/XtreamApi.kt` |
| Room (encje + DAO indeksowane) | `data/.../local/` |
| Quick Zap + D-Pad + PiP | `app/.../presentation/player/PlayerScreen.kt` |
| Nawigacja | `app/.../presentation/navigation/AppNavHost.kt` |

## Budowanie
Projekt używa Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`).

```bash
# Wygeneruj wrapper (jar wrappera nie jest w repo):
gradle wrapper --gradle-version 8.9
./gradlew :app:assembleDebug
./gradlew :data:testDebugUnitTest   # test parsera M3U
```

Wymagane: JDK 17, Android SDK 34, `local.properties` z `sdk.dir`.

## Status
Szkielet architektoniczny gotowy do rozbudowy o pełne ekrany VOD/Series,
ekran ustawień źródeł i pełną paginację list (PagingSource jest już w DAO).
