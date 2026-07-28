# Kolorowy czat na bubleroyal.com

Userscript, który pozwala wybrać kolor Twoich wiadomości na czacie tak, żeby **widzieli go też inni gracze**.

## Najpierw najważniejsze — jak to w ogóle może działać

Nie mamy dostępu do serwera gry. Serwer przyjmuje wiadomość jako **zwykły tekst** i takim tekstem rozsyła ją dalej — nie ma w protokole pola „kolor” i nie da się go dodać z poziomu przeglądarki. Nie da się też zmusić cudzej przeglądarki, żeby coś pokolorowała.

Jedyne, co przechodzi przez serwer, to sama treść. Dlatego kolor jedzie **w treści wiadomości**, jako krótki znacznik:

```
[c#ff5555]siema wszystkim
```

Skrypt po stronie odbiorcy wycina ten znacznik i koloruje wiadomość. W praktyce:

| Odbiorca | Co widzi |
|---|---|
| ma ten skrypt | `siema wszystkim` na czerwono |
| nie ma skryptu | `[c#ff5555]siema wszystkim` |

Żeby kolory działały „dla wszystkich”, skrypt musi rozejść się wśród graczy — inaczej się nie da bez ingerencji w serwer gry.

## Instalacja

1. Zainstaluj [Tampermonkey](https://www.tampermonkey.net/) (Chrome/Edge/Opera) albo [Violentmonkey](https://violentmonkey.github.io/) (też Firefox).
2. Otwórz panel rozszerzenia → **Utwórz nowy skrypt**.
3. Wklej całą zawartość `bubleroyal-chat-color.user.js` i zapisz (Ctrl+S).
4. Wejdź na `bubleroyal.com` — w prawym dolnym rogu pojawi się kółko 🎨.

## Konfiguracja (raz)

Skrypt nie zna struktury HTML gry, więc próbuje sam znaleźć pole czatu. Jeśli trafi źle albo kolory nie działają:

1. Kliknij 🎨 → **Wskaż pole czatu** → kliknij pole, w którym piszesz wiadomości.
2. Kliknij 🎨 → **Wskaż listę wiadomości** → kliknij ramkę, w której pojawiają się wpisy czatu.

Wybór zapisuje się w `localStorage`, więc robisz to tylko raz. Podczas wskazywania podświetlany jest element pod kursorem; `Esc` anuluje.

## Panel

- **paleta / próbnik / pole hex** — wybór koloru,
- **Włącz kolorowanie** — globalny wyłącznik (wyłączony = wysyłasz czysty tekst bez znacznika),
- **Koloruj cały wiersz** — koloruje całą linię wiadomości zamiast samego tekstu (przydatne, gdy gra trzyma nick i treść w jednym elemencie),
- **Tryb awaryjny (WebSocket)** — włącz tylko wtedy, gdy znacznik nie dociera do innych graczy. Skrypt dokleja go wtedy do wychodzącej ramki WebSocket zamiast do pola tekstowego.

Biały (`#ffffff`) traktowany jest jako „domyślny” — przy nim skrypt nie dokleja żadnego znacznika, żeby nie zaśmiecać czatu.

## Jak to jest zrobione

- `@run-at document-start` + podmiana `WebSocket.prototype.send` — żeby tryb awaryjny zdążył się wpiąć przed startem gry.
- Wysyłanie: nasłuch `keydown`/`submit`/`pointerdown` w fazie **przechwytywania**, więc znacznik trafia do pola zanim gra odczyta jego wartość. Wartość ustawiana jest natywnym setterem + zdarzeniem `input`, żeby React/Vue zauważyły zmianę.
- Odbieranie: `MutationObserver` na liście wiadomości, `TreeWalker` po węzłach tekstowych, wycięcie znacznika i `style.color` na elemencie.
- UI siedzi w Shadow DOM, więc CSS gry go nie rozjedzie i odwrotnie.

## Testy

```bash
cd userscript
npm install      # tylko dla testów DOM (jsdom)
npm test
```

`test/marker.test.js` sprawdza kodowanie/dekodowanie znacznika (bez zależności), `test/dom.test.js` uruchamia cały skrypt w jsdom na atrapie czatu i sprawdza oba kierunki: doklejanie znacznika przy wysyłce i kolorowanie odebranej wiadomości.

## Ograniczenia

- Gracze bez skryptu widzą surowy znacznik.
- Jeśli serwer gry filtruje nawiasy kwadratowe, spróbuj wariantów `<c#ff5555>` lub `{c#ff5555}` — dekoder rozumie wszystkie trzy.
- Limit długości wiadomości zmniejsza się o 10 znaków (długość znacznika).
- Zmiana kodu strony przez autorów gry może wymagać ponownego wskazania elementów.
