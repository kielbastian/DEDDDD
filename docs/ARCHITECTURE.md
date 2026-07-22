# Architektura aplikacji IPTV / VOD dla Android TV (Xiaomi Mi Box S / TV Box 4)

> Specyfikacja techniczna natywnej aplikacji IPTV/VOD dedykowanej dla urządzeń
> Xiaomi Mi Box S / Mi TV Box 4 (2 GB RAM, Amlogic S905X/X2, Android TV 9/11,
> pilot z D-Padem). Nacisk na **skrajnie niskie zużycie pamięci**, płynność,
> akcelerację sprzętową i nawigację pilotem.

---

## 1. Założenia i ograniczenia sprzętowe

| Parametr | Wartość | Konsekwencja projektowa |
|---|---|---|
| RAM | 2 GB (realnie ~1.2 GB dostępne dla app) | Twardy budżet pamięci ~180–220 MB, brak trzymania całych playlist w RAM |
| SoC | Amlogic S905X / S905X2 | Dekodowanie sprzętowe HEVC/AV1 via MediaCodec, **nie** software |
| GPU | Mali-450 / G31 | Minimalizacja przerysowań Compose, brak ciężkich blur/shadow |
| Wejście | Pilot Bluetooth/IR, D-Pad + OK + Back + Home | UI wyłącznie sterowane fokusem, brak zależności od dotyku |
| Ekran | 1080p/4K, różne odświeżanie (24/25/50/60 Hz) | Auto Frame Rate (AFR) obowiązkowy |
| Sieć | Wi-Fi / Ethernet, playlisty rzędu 10k–50k kanałów | Parsowanie strumieniowe w tle, paginacja, indeksy w bazie |

**Główne zasady wydajnościowe:**
1. Nigdy nie ładujemy całej playlisty M3U ani EPG do pamięci — parsujemy strumieniowo i zapisujemy wprost do Room (`insert` w partiach / batch).
2. Listy w UI korzystają z `LazyColumn`/`LazyRow` + `PagingSource` (Room Paging 3) — w pamięci tylko widoczne okno.
3. Jeden współdzielony `ExoPlayer` na cały cykl życia sesji odtwarzania, zwalniany deterministycznie w `onStop`/`onDestroy`.
4. Obrazy (loga kanałów) przez Coil z limitem cache pamięciowego (`memoryCache { maxSizePercent(0.15) }`).
5. Brak wycieków: `ExoPlayer`, `MediaSession`, listenery i coroutine scope wiązane z `lifecycleScope`/`ViewModel` scope.

---

## 2. Struktura modułów (Android Clean Architecture)

Podział wielomodułowy Gradle — separacja warstw wymusza kierunek zależności
`presentation → domain ← data` i skraca czas kompilacji przyrostowej.

```
DEDDDD/  (root Gradle project)
│
├── :app                    # Presentation + DI kompozycja (Compose for TV)
│   ├── presentation/
│   │   ├── navigation/      # NavHost, trasy, graf D-Pad
│   │   ├── player/          # PlayerScreen, PlayerManager, AFR, PiP, QuickZap
│   │   ├── live/            # Lista kanałów, EPG now/next, kategorie
│   │   └── common/          # Komponenty TV (FocusableCard, Immersive rows)
│   ├── di/                  # Moduły Hilt (kompozycja grafu)
│   ├── TvIptvApp.kt         # Application + Hilt entry point
│   └── MainActivity.kt
│
├── :domain                 # CZYSTY Kotlin (bez Androida) — reguły biznesowe
│   ├── model/              # Channel, Category, EpgProgram, PlaylistSource...
│   ├── repository/         # Interfejsy (kontrakty) — implementacje w :data
│   └── usecase/            # SyncPlaylistUseCase, GetChannelsUseCase, GetNowNextUseCase...
│
├── :data                   # Implementacje: sieć, baza, parsery, mappery
│   ├── local/
│   │   ├── entity/         # Encje Room (indeksowane)
│   │   ├── dao/            # DAO z zapytaniami paginowanymi
│   │   └── IptvDatabase.kt
│   ├── remote/
│   │   └── xtream/         # Retrofit API Xtream Codes + DTO
│   ├── parser/            # M3uParser (SAX-like), XmltvParser (XmlPullParser)
│   ├── mapper/            # DTO/Entity <-> Domain
│   └── repository/        # *RepositoryImpl
│
└── :core                   # Wspólne narzędzia (Result, DispatcherProvider, logi)
```

**Kierunek zależności (Gradle):**

```
:app     ──▶ :domain, :data (tylko do wpięcia DI), :core
:data    ──▶ :domain, :core
:domain  ──▶ (nic — czysty Kotlin)
:core    ──▶ (nic)
```

Warstwa `:domain` nie zna Androida ani ExoPlayera — dzięki temu jest testowalna
w JVM bez emulatora i stanowi stabilny rdzeń.

---

## 3. Stos technologiczny

| Obszar | Wybór | Uzasadnienie dla Mi Box (2 GB) |
|---|---|---|
| Język | **Kotlin** + Coroutines/Flow | Struktura async, brak wątków „ręcznych", strukturalna anulowalność |
| UI | **Jetpack Compose for TV** (`androidx.tv:tv-material`) | Nowoczesny fokus D-Pad, mniej boilerplate niż Leanback; alternatywa: Leanback dla starszych API |
| Odtwarzanie | **Media3 / ExoPlayer** (`androidx.media3`) | Sprzętowe MediaCodec (HEVC/AV1/DV), tunneling, passthrough, MediaSession |
| DI | **Hilt** (Dagger) | Kompilowany graf (zero refleksji w runtime = mniej RAM/GC niż Koin przy dużym grafie) |
| Baza | **Room** (SQLite) + **Paging 3** | Indeksy, zapytania paginowane, brak trzymania list w RAM |
| Sieć | **Retrofit** + **OkHttp** + **kotlinx.serialization** | Xtream API JSON; OkHttp z limitem połączeń i cache |
| Parsowanie XML | **XmlPullParser** (wbudowany) | Strumieniowe, zero-alloc dla wielkich EPG XMLTV — bez DOM |
| Obrazy | **Coil** (`coil-compose`) | Lekki, coroutine-native, twardy limit cache pamięci |
| Async/wątki | **Coroutines** + **WorkManager** | Sync playlist/EPG w tle jako zadania z ograniczeniami (Wi-Fi, ładowanie) |
| Preferencje | **DataStore (Proto/Preferences)** | Ustawienia (ostatni kanał, profil, AFR on/off) |
| Testy | JUnit5, Turbine, Room in-memory, MockK | Testy domeny w JVM |

**Wersje bazowe** (patrz `gradle/libs.versions.toml`):
`minSdk 25` (Android TV 7.1 — bezpieczny dla Mi Box S), `targetSdk 34`,
`compileSdk 34`, Media3 `1.4.x`, Compose BOM `2024.09.x`, Room `2.6.x`.

---

## 4. Warstwa odtwarzania — kluczowe decyzje

### 4.1 Akceleracja sprzętowa
- `DefaultRenderersFactory.setExtensionRendererMode(EXTENSION_RENDERER_MODE_OFF)`
  — wymuszamy dekodery **sprzętowe** (MediaCodec), bez software fallback, który
  zabiłby CPU Amlogica na HEVC/AV1.
- Wideo tunneling (`MediaCodec` tunneled playback) dla płynności i mniejszego
  obciążenia — włączane, gdy `Display` je wspiera.
- HEVC/H.265, AV1, Dolby Vision — obsługiwane natywnie przez SoC (deklarujemy
  możliwości, ale to MediaCodec/kernel realnie dekodują).

### 4.2 Audio passthrough (Dolby Digital / DTS przez HDMI/S-PDIF)
- `DefaultAudioSink` z `AudioCapabilities` odczytanymi z `AudioManager` —
  ExoPlayer sam wybiera passthrough (E-AC3/AC3/DTS), gdy AVR to zgłasza.

### 4.3 Auto Frame Rate (AFR)
- Po `onVideoSizeChanged`/gotowości formatu odczytujemy `Format.frameRate`
  i przez `Surface`/`Display.Mode` (`preferredDisplayModeId`, API 23+) lub
  `Surface.setFrameRate` (API 30+) dopasowujemy odświeżanie ekranu do materiału
  (23.976/24/25/50/60), eliminując judder.

### 4.4 Cykl życia i brak wycieków
- Jeden `ExoPlayer` trzymany w `PlayerManager` (scoped do sesji), zwalniany w
  `DisposableEffect`/`onStop`. `MediaSession` zwalniany razem z playerem.
- Bufory ExoPlayera dostrojone w dół (`DefaultLoadControl`) pod 2 GB RAM.

Pełna konfiguracja: [`app/.../presentation/player/PlayerManager.kt`](../app/src/main/java/com/mibox/iptv/presentation/player/PlayerManager.kt).

---

## 5. Parsowanie i baza danych

### 5.1 M3U / M3U8
- Parser strumieniowy (`M3uParser`) czyta linia po linii z `BufferedReader`,
  emituje `Flow<M3uEntry>` — repozytorium zapisuje partiami po ~500 wpisów
  (`@Insert` w transakcji). RAM nie rośnie wraz z rozmiarem playlisty.

### 5.2 Xtream Codes API
- `player_api.php?action=get_live_categories | get_live_streams | get_vod_streams | get_series`
  przez Retrofit; strony mapowane do encji Room. EPG z `get_short_epg` lub XMLTV.

### 5.3 EPG XMLTV
- `XmltvParser` na `XmlPullParser` (pull, nie DOM) — parsuje `<programme>`
  strumieniowo, mapuje czasy do epoch millis, zapisuje batch do Room.
  Zobsługa gzip (XMLTV bywa `.xml.gz`).

### 5.4 Schemat Room (indeksowanie)
- `channels(id, categoryId, name, tvgId, streamUrl, logo, sortOrder)`
  — indeks na `categoryId`, `tvgId`, `name`.
- `epg_programs(id, channelTvgId, start, end, title, desc)`
  — indeks złożony `(channelTvgId, start)` do szybkiego „now/next".
- Zapytania „now playing": `WHERE channelTvgId = :id AND start <= :now AND end > :now`.
- Paging 3 `PagingSource` dla list kanałów per kategoria.

---

## 6. Nawigacja i UX pod pilota (D-Pad)

### 6.1 Graf nawigacji
```
[Splash/Sync] ──▶ [Home / Live TV]
                     │  ├── Rail kategorii (LazyRow, fokus poziomy)
                     │  ├── Lista kanałów (LazyColumn, fokus pionowy)
                     │  └── Panel EPG now/next (prawa strona)
                     │
                     ├─ OK na kanale ──▶ [Player (pełny ekran)]
                     │                     ├── OK  → overlay info + EPG
                     │                     ├── ↑/↓ → Quick Zap (poprzedni/następny kanał)
                     │                     ├── OK długie / MENU → Channel list overlay
                     │                     ├── Back → PiP (Picture-in-Picture)
                     │                     └── Back x2 → powrót do Home
                     │
                     ├─ [VOD] ──▶ [Kategorie VOD] ──▶ [Szczegóły] ──▶ [Player]
                     └─ [Ustawienia] (źródła playlist, AFR, buforowanie)
```

### 6.2 Zasady fokusa (Compose for TV)
- Każdy element listy: `Modifier.focusable()` + wyraźny stan fokusa (skala 1.06,
  obrys/kolor) — czytelny z 3 m od TV.
- `bringIntoViewRequester` + `focusRestorer()` — powrót do ostatniego fokusa po
  wyjściu z ekranu (kluczowe dla UX pilota).
- Obsługa klawiszy `KEYCODE_DPAD_UP/DOWN` w playerze → Quick Zap (bez otwierania
  pełnej listy, przełączenie strumienia w <300 ms z buforowaniem sąsiadów).
- `KEYCODE_MEDIA_PLAY_PAUSE`, `KEYCODE_INFO` mapowane na akcje playera.

### 6.3 Picture-in-Picture
- `MainActivity` z `supportsPictureInPicture`, `enterPictureInPictureMode` na
  `Back` w playerze; `PictureInPictureParams` z proporcjami wideo i `RemoteAction`.

### 6.4 Quick Zap (szybkie przełączanie)
- Prefetch sąsiednich kanałów: gdy gramy kanał N, przygotowany jest lekki
  `MediaItem` dla N-1 i N+1; przełączenie to `setMediaItem` + `prepare` bez
  rekonstrukcji playera — subiektywnie „natychmiast".

---

## 7. Praca w tle i synchronizacja
- **WorkManager**: `PlaylistSyncWorker` i `EpgSyncWorker` z ograniczeniami
  (`NetworkType.CONNECTED`, opcjonalnie tylko przy zasilaniu), okresowo (np. EPG
  co 6–12 h). Idempotentne: `upsert` do Room, czyszczenie starych programów EPG.
- Wynik synchronizacji komunikowany do UI przez Flow z Room (single source of truth).

---

## 8. Budżet pamięci — checklista anty-wyciek
- [ ] `ExoPlayer.release()` w `DisposableEffect onDispose` / `onStop`.
- [ ] `MediaSession.release()` sprzężony z playerem.
- [ ] Listenery ExoPlayera usuwane (`removeListener`) przy zwolnieniu.
- [ ] Coroutines w `viewModelScope` / `lifecycleScope` — auto-anulowanie.
- [ ] Coil: `maxSizePercent(0.15)`, brak trzymania oryginałów logo.
- [ ] Parsery: strumień + batch insert, `use { }` na strumieniach (auto-close).
- [ ] `DefaultLoadControl` z obniżonymi buforami (patrz `PlayerManager`).
- [ ] LeakCanary tylko w `debug` (weryfikacja), nigdy w `release`.

---

## 9. Mapowanie plików scaffoldu

| Warstwa | Plik |
|---|---|
| Domain – model | `domain/.../model/Channel.kt`, `EpgProgram.kt`, `Category.kt`, `PlaylistSource.kt` |
| Domain – kontrakty | `domain/.../repository/*.kt` |
| Domain – use case | `domain/.../usecase/*.kt` |
| Data – baza | `data/.../local/IptvDatabase.kt`, `entity/*.kt`, `dao/*.kt` |
| Data – Xtream | `data/.../remote/xtream/XtreamApi.kt`, DTO |
| Data – parsery | `data/.../parser/M3uParser.kt`, `XmltvParser.kt` |
| Data – repo | `data/.../repository/*Impl.kt` |
| Player | `app/.../presentation/player/PlayerManager.kt` |
| UI/Nawigacja | `app/.../presentation/navigation/*.kt`, `live/*.kt` |
| DI | `app/.../di/*.kt` |

Pełny, gotowy do rozbudowy szkielet znajduje się w tym repozytorium.
