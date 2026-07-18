# Kalkulator CNC (Android)

Natywna aplikacja na Androida opakowująca istniejący, w pełni działający
kalkulator warsztatowy (HTML/CSS/JS) w prosty `WebView`. Cała logika
obliczeniowa pozostaje bez zmian — działa lokalnie, bez połączenia z
internetem (jedynie czcionki Google Fonts wymagają sieci; bez niej
aplikacja używa czcionki systemowej).

## Funkcje kalkulatora

- **Stożek Morse'a** – kąt stożka z dwóch średnic i długości, dopasowanie
  do najbliższej normy MT0–MT6.
- **Pasowania ISO 286** – odchyłki otworu/wałka, rodzaj pasowania
  (luźne / wciskowe / mieszane).
- **Identyfikacja gwintu** – rozpoznaje gwint metryczny lub calowy
  (UNC/UNF/BSP/NPT) na podstawie zmierzonej średnicy i opcjonalnie skoku.
- **Gwintowanie sztywne** – parametry (obroty, wiertło, posuw) i gotowy
  kod G84 dla tokarki lub frezarki.
- **Toczenie gwintu nożem** – cykle G76 / G92 / G32 z harmonogramem
  przejść i generowanym kodem G-code (przycisk „Kopiuj” do schowka).
- **Koło na wieloboku** – średnica przygotówki pod kwadrat, sześciokąt,
  ośmiokąt lub prostokąt.

## Budowanie

```bash
./gradlew :cncapp:assembleDebug
```

APK: `cncapp/build/outputs/apk/debug/cncapp-debug.apk`.

## Struktura

```
cncapp/src/main/
├── assets/index.html         # oryginalny kalkulator (HTML/CSS/JS, bez zmian logiki)
├── java/.../MainActivity.kt  # pełnoekranowy WebView + obsługa przycisku "wstecz"
└── res/                      # ikona aplikacji (wyodrębniona z pliku HTML), motyw
```
