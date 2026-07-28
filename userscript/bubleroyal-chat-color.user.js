// ==UserScript==
// @name         BubleRoyal - kolorowy czat (multiplayer)
// @namespace    https://bubleroyal.com/
// @version      1.0.0
// @description  Zmiana koloru wlasnych wiadomosci na czacie. Kolor jest zakodowany w tresci wiadomosci, wiec widza go wszyscy gracze, ktorzy maja ten skrypt.
// @author       -
// @match        *://bubleroyal.com/*
// @match        *://*.bubleroyal.com/*
// @run-at       document-start
// @grant        none
// ==/UserScript==

/*
 * JAK TO DZIALA
 * -------------
 * Serwer gry rozsyla wiadomosci czatu jako zwykly tekst i nie da sie tego zmienic
 * bez dostepu do serwera. Dlatego kolor jedzie w samej tresci wiadomosci jako
 * krotki znacznik, np.:
 *
 *     [c#ff5555]siema
 *
 * Skrypt po stronie ODBIORCY wycina ten znacznik i koloruje wiadomosc.
 * Gracz bez skryptu zobaczy surowe "[c#ff5555]siema" - to jedyny kompromis,
 * ktorego nie da sie ominac bez modyfikacji serwera.
 *
 * KONFIGURACJA
 * ------------
 * Skrypt nie zna struktury HTML strony, wiec przy pierwszym uruchomieniu
 * wskazujesz mu dwa elementy klikajac w nie myszka (przycisk "?" -> panel):
 *   1. pole tekstowe czatu (tam gdzie piszesz),
 *   2. liste wiadomosci (kontener, w ktorym pojawiaja sie wpisy).
 * Wybor zapisuje sie w localStorage, wiec robisz to raz.
 */

(function () {
  'use strict';

  const STORAGE_KEY = 'brChatColor.cfg.v1';

  const DEFAULTS = {
    enabled: true,
    color: '#ff5555',
    inputSelector: '',
    listSelector: '',
    // Kolorowanie calego wiersza wiadomosci zamiast samego tekstu.
    colorWholeLine: false,
    // Awaryjne przechwytywanie wychodzacych ramek WebSocket. Wlacz tylko wtedy,
    // gdy gra nie czyta tresci bezposrednio z pola tekstowego i znacznik nie
    // dociera do innych graczy.
    wsHook: false,
  };

  const PALETTE = [
    '#ffffff', '#ff5555', '#ff9f43', '#ffd93d', '#6bcB77',
    '#4d96ff', '#9b5de5', '#ff6fb5', '#00d4c8', '#9aa0a6',
  ];

  // Akceptujemy kilka wariantow zapisu, zeby znacznik przetrwal ewentualne
  // filtry serwera podmieniajace nawiasy.
  // Znacznik na poczatku tresci (tak go wysylamy) oraz w dowolnym miejscu tekstu
  // (tak potrafi go zwrocic serwer, np. po doklejeniu nicku nadawcy).
  const MARKER_RE = /^\s*(?:\[c#|<c#|\{c#)([0-9a-fA-F]{6}|[0-9a-fA-F]{3})(?:\]|>|\})\s?/;
  const MARKER_RE_ANY = /(?:\[c#|<c#|\{c#)([0-9a-fA-F]{6}|[0-9a-fA-F]{3})(?:\]|>|\})\s?/;

  const cfg = loadConfig();
  let lastTypedText = '';
  let pickMode = null; // 'input' | 'list' | null
  let listObserver = null;
  let observedNode = null;

  // ---------------------------------------------------------------- config

  function loadConfig() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      return Object.assign({}, DEFAULTS, raw ? JSON.parse(raw) : {});
    } catch (e) {
      return Object.assign({}, DEFAULTS);
    }
  }

  function saveConfig() {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(cfg));
    } catch (e) {
      /* prywatny tryb przegladarki - trudno, dziala do konca sesji */
    }
  }

  // ---------------------------------------------------------------- helpers

  function normalizeHex(value) {
    let hex = String(value || '').trim().replace(/^#/, '');
    if (hex.length === 3) hex = hex.split('').map((c) => c + c).join('');
    return /^[0-9a-fA-F]{6}$/.test(hex) ? '#' + hex.toLowerCase() : null;
  }

  function buildMarker(color) {
    return '[c' + normalizeHex(color) + ']';
  }

  function isEditable(el) {
    if (!el || el.nodeType !== 1) return false;
    if (el.tagName === 'TEXTAREA') return true;
    if (el.tagName === 'INPUT') return /^(text|search|$)/i.test(el.type || '');
    return el.isContentEditable === true;
  }

  function getText(el) {
    return el.isContentEditable ? el.textContent : el.value;
  }

  /**
   * Ustawia wartosc pola tak, zeby zauwazyly to frameworki (React/Vue trzymaja
   * wlasny stan i ignoruja zwykle `el.value = ...`).
   */
  function setText(el, text) {
    if (el.isContentEditable) {
      el.textContent = text;
      placeCaretAtEnd(el);
    } else {
      const proto = el.tagName === 'TEXTAREA'
        ? HTMLTextAreaElement.prototype
        : HTMLInputElement.prototype;
      const setter = Object.getOwnPropertyDescriptor(proto, 'value').set;
      setter.call(el, text);
      try {
        el.setSelectionRange(text.length, text.length);
      } catch (e) { /* input[type=search] potrafi rzucac */ }
    }
    el.dispatchEvent(new Event('input', { bubbles: true }));
  }

  function placeCaretAtEnd(el) {
    const range = document.createRange();
    range.selectNodeContents(el);
    range.collapse(false);
    const sel = window.getSelection();
    sel.removeAllRanges();
    sel.addRange(range);
  }

  /** Sciezka CSS wystarczajaco stabilna, by odnalezc element po odswiezeniu. */
  function cssPath(el) {
    if (el.id) return '#' + CSS.escape(el.id);
    const parts = [];
    let node = el;
    while (node && node.nodeType === 1 && node !== document.body && parts.length < 6) {
      let part = node.tagName.toLowerCase();
      if (node.id) {
        parts.unshift('#' + CSS.escape(node.id));
        break;
      }
      const stableClass = Array.from(node.classList).find(
        (c) => c && !/^(ng-|css-|jsx-)/.test(c) && !/\d{4,}/.test(c)
      );
      if (stableClass) part += '.' + CSS.escape(stableClass);
      const parent = node.parentElement;
      if (parent) {
        const twins = Array.from(parent.children).filter((c) => c.tagName === node.tagName);
        if (twins.length > 1) part += ':nth-of-type(' + (twins.indexOf(node) + 1) + ')';
      }
      parts.unshift(part);
      node = node.parentElement;
    }
    return parts.join(' > ');
  }

  function queryOne(selector) {
    if (!selector) return null;
    try {
      return document.querySelector(selector);
    } catch (e) {
      return null;
    }
  }

  function getChatInput() {
    const saved = queryOne(cfg.inputSelector);
    if (saved && isEditable(saved)) return saved;
    return autodetectInput();
  }

  /** Najbardziej prawdopodobne pole czatu: widoczne, szerokie, nisko na ekranie. */
  function autodetectInput() {
    const candidates = Array.from(
      document.querySelectorAll('input, textarea, [contenteditable=""], [contenteditable="true"]')
    ).filter((el) => {
      if (!isEditable(el)) return false;
      const r = el.getBoundingClientRect();
      return r.width > 80 && r.height > 10;
    });
    if (!candidates.length) return null;

    const hinted = candidates.find((el) => {
      const hint = ((el.placeholder || '') + ' ' + (el.name || '') + ' ' +
        (el.id || '') + ' ' + (el.className || '') + ' ' +
        (el.getAttribute('aria-label') || '')).toLowerCase();
      return /chat|czat|message|wiadom|say|talk/.test(hint);
    });
    if (hinted) return hinted;

    return candidates.sort(
      (a, b) => b.getBoundingClientRect().top - a.getBoundingClientRect().top
    )[0];
  }

  function getChatList() {
    return queryOne(cfg.listSelector) || document.body;
  }

  // ------------------------------------------------------- wychodzace wiadomosci

  /** Dokleja znacznik koloru do tresci pola, o ile jeszcze go nie ma. */
  function stampInput(el) {
    if (!cfg.enabled || !el) return;
    const text = getText(el);
    if (!text || !text.trim()) return;
    if (MARKER_RE.test(text)) return;
    const color = normalizeHex(cfg.color);
    if (!color || color === '#ffffff') return; // biel = domyslny wyglad, nie smiecimy
    setText(el, buildMarker(color) + text);
  }

  function onKeyDownCapture(e) {
    if (e.key !== 'Enter' || e.shiftKey || e.isComposing) return;
    const el = e.target;
    if (!isEditable(el)) return;
    const saved = queryOne(cfg.inputSelector);
    if (saved && el !== saved) return;
    stampInput(el);
  }

  function onSubmitCapture(e) {
    const input = e.target.querySelector && e.target.querySelector('input, textarea');
    if (input) stampInput(input);
  }

  function onPointerDownCapture(e) {
    // Klikniecie w przycisk "wyslij" obok pola czatu.
    if (pickMode) return;
    const input = getChatInput();
    if (!input) return;
    const btn = e.target.closest && e.target.closest('button, [role="button"], .send, .btn');
    if (!btn || btn === input) return;
    const label = (btn.textContent + ' ' + (btn.className || '') + ' ' +
      (btn.getAttribute('aria-label') || '')).toLowerCase();
    if (/send|wysl|wyśl|chat|czat|➤|▶/.test(label)) stampInput(input);
  }

  function onInputCapture(e) {
    if (isEditable(e.target)) lastTypedText = getText(e.target) || '';
  }

  /**
   * Awaryjne wpiecie w WebSocket: jesli gra wysyla tekst pobrany wczesniej niz
   * w momencie Entera, doklejamy znacznik juz na poziomie ramki.
   */
  function installWebSocketHook() {
    const original = WebSocket.prototype.send;
    WebSocket.prototype.send = function (data) {
      try {
        if (cfg.enabled && cfg.wsHook && typeof data === 'string' && lastTypedText) {
          const color = normalizeHex(cfg.color);
          const plain = lastTypedText.trim();
          if (color && color !== '#ffffff' && plain && !MARKER_RE_ANY.test(data) && data.includes(plain)) {
            data = data.replace(plain, buildMarker(color) + plain);
            lastTypedText = '';
          }
        }
      } catch (e) { /* nigdy nie blokujemy wysylki */ }
      return original.call(this, data);
    };
  }

  // ------------------------------------------------------- przychodzace wiadomosci

  /** Znajduje wezly tekstowe ze znacznikiem, wycina go i nadaje kolor. */
  function colorizeIn(root) {
    if (!root || root.nodeType !== 1) return;
    if (!MARKER_RE_ANY.test(root.textContent || '')) return;

    const walker = document.createTreeWalker(root, NodeFilter.SHOW_TEXT, null);
    const hits = [];
    let node;
    while ((node = walker.nextNode())) {
      if (MARKER_RE_ANY.test(node.data)) hits.push(node);
    }

    for (const textNode of hits) {
      const match = textNode.data.match(MARKER_RE) || textNode.data.match(MARKER_RE_ANY);
      if (!match) continue;
      const color = normalizeHex(match[1]);
      if (!color) continue;

      textNode.data = textNode.data.replace(match[0], '');

      const target = cfg.colorWholeLine
        ? (textNode.parentElement && textNode.parentElement.closest('li, tr, p, div') ||
           textNode.parentElement)
        : textNode.parentElement;
      if (target && !target.dataset.brColored) {
        target.style.color = color;
        target.dataset.brColored = '1';
      }
    }
  }

  function attachListObserver() {
    if (listObserver) listObserver.disconnect();
    const list = getChatList();
    if (!list) return;
    listObserver = new MutationObserver((records) => {
      for (const rec of records) {
        for (const added of rec.addedNodes) {
          if (added.nodeType === 1) colorizeIn(added);
          else if (added.nodeType === 3 && added.parentElement) colorizeIn(added.parentElement);
        }
        if (rec.type === 'characterData' && rec.target.parentElement) {
          colorizeIn(rec.target.parentElement);
        }
      }
    });
    listObserver.observe(list, {
      childList: true,
      subtree: true,
      characterData: true,
    });
    observedNode = list;
    colorizeIn(list); // wiadomosci juz obecne na ekranie
  }

  // ---------------------------------------------------------------- UI

  let panelRoot = null;
  let ui = {};

  function buildUI() {
    const host = document.createElement('div');
    host.id = 'br-chat-color-host';
    host.style.cssText = 'position:fixed;z-index:2147483647;right:16px;bottom:16px;';
    const shadow = host.attachShadow({ mode: 'open' });

    shadow.innerHTML = `
      <style>
        :host, * { box-sizing: border-box; }
        .fab {
          width: 40px; height: 40px; border-radius: 50%; border: 2px solid #fff;
          cursor: pointer; font-size: 18px; line-height: 1;
          box-shadow: 0 2px 10px rgba(0,0,0,.45);
          display: flex; align-items: center; justify-content: center;
        }
        .panel {
          position: absolute; right: 0; bottom: 50px; width: 250px; display: none;
          background: #1b1d22; color: #e8e8e8; border: 1px solid #3a3d45;
          border-radius: 10px; padding: 12px;
          font: 13px/1.45 system-ui, "Segoe UI", sans-serif;
          box-shadow: 0 8px 26px rgba(0,0,0,.5);
        }
        .panel.open { display: block; }
        h4 { margin: 0 0 8px; font-size: 13px; font-weight: 600; }
        .swatches { display: grid; grid-template-columns: repeat(5, 1fr); gap: 6px; margin-bottom: 10px; }
        .sw { height: 24px; border-radius: 5px; border: 2px solid transparent; cursor: pointer; }
        .sw.sel { border-color: #fff; }
        .row { display: flex; align-items: center; gap: 8px; margin: 8px 0; }
        input[type=color] { width: 34px; height: 26px; padding: 0; border: none; background: none; cursor: pointer; }
        input[type=text] {
          flex: 1; min-width: 0; background: #12141a; color: #e8e8e8;
          border: 1px solid #3a3d45; border-radius: 5px; padding: 4px 6px; font: inherit;
        }
        button {
          background: #2a2e37; color: #e8e8e8; border: 1px solid #454a55;
          border-radius: 5px; padding: 5px 8px; cursor: pointer; font: inherit; width: 100%;
        }
        button:hover { background: #333845; }
        button.on { background: #2f6b3d; border-color: #3f8b50; }
        label.chk { display: flex; align-items: center; gap: 6px; cursor: pointer; margin: 6px 0; }
        .hint { color: #9aa0a6; font-size: 11px; margin-top: 8px; }
        .ok { color: #6bcB77; }
        .bad { color: #ff8a8a; }
      </style>
      <div class="panel" id="panel">
        <h4>Kolor czatu</h4>
        <div class="swatches" id="swatches"></div>
        <div class="row">
          <input type="color" id="picker">
          <input type="text" id="hex" spellcheck="false">
        </div>
        <label class="chk"><input type="checkbox" id="enabled"> Wlacz kolorowanie</label>
        <label class="chk"><input type="checkbox" id="whole"> Koloruj caly wiersz</label>
        <label class="chk"><input type="checkbox" id="ws"> Tryb awaryjny (WebSocket)</label>
        <div class="row"><button id="pickInput">Wskaz pole czatu</button></div>
        <div class="row"><button id="pickList">Wskaz liste wiadomosci</button></div>
        <div class="hint" id="status"></div>
      </div>
      <div class="fab" id="fab" title="Kolor czatu">🎨</div>
    `;

    (document.body || document.documentElement).appendChild(host);
    panelRoot = shadow;

    ui = {
      panel: shadow.getElementById('panel'),
      fab: shadow.getElementById('fab'),
      swatches: shadow.getElementById('swatches'),
      picker: shadow.getElementById('picker'),
      hex: shadow.getElementById('hex'),
      enabled: shadow.getElementById('enabled'),
      whole: shadow.getElementById('whole'),
      ws: shadow.getElementById('ws'),
      pickInput: shadow.getElementById('pickInput'),
      pickList: shadow.getElementById('pickList'),
      status: shadow.getElementById('status'),
    };

    PALETTE.forEach((color) => {
      const sw = document.createElement('div');
      sw.className = 'sw';
      sw.style.background = color;
      sw.dataset.color = color;
      sw.addEventListener('click', () => setColor(color));
      ui.swatches.appendChild(sw);
    });

    ui.fab.addEventListener('click', () => ui.panel.classList.toggle('open'));
    ui.picker.addEventListener('input', (e) => setColor(e.target.value));
    ui.hex.addEventListener('change', (e) => {
      const color = normalizeHex(e.target.value);
      if (color) setColor(color); else refreshUI();
    });
    ui.enabled.addEventListener('change', (e) => { cfg.enabled = e.target.checked; saveConfig(); refreshUI(); });
    ui.whole.addEventListener('change', (e) => { cfg.colorWholeLine = e.target.checked; saveConfig(); });
    ui.ws.addEventListener('change', (e) => { cfg.wsHook = e.target.checked; saveConfig(); });
    ui.pickInput.addEventListener('click', () => startPick('input'));
    ui.pickList.addEventListener('click', () => startPick('list'));

    refreshUI();
  }

  function setColor(color) {
    const hex = normalizeHex(color);
    if (!hex) return;
    cfg.color = hex;
    saveConfig();
    refreshUI();
  }

  function refreshUI() {
    if (!panelRoot) return;
    ui.fab.style.background = cfg.enabled ? cfg.color : '#555';
    ui.picker.value = cfg.color;
    ui.hex.value = cfg.color;
    ui.enabled.checked = cfg.enabled;
    ui.whole.checked = cfg.colorWholeLine;
    ui.ws.checked = cfg.wsHook;
    ui.pickInput.classList.toggle('on', pickMode === 'input');
    ui.pickList.classList.toggle('on', pickMode === 'list');

    const hasInput = !!getChatInput();
    const hasList = !!queryOne(cfg.listSelector);
    ui.status.innerHTML =
      'Pole czatu: <span class="' + (hasInput ? 'ok">wykryte' : 'bad">brak') + '</span><br>' +
      'Lista wiadomosci: <span class="' + (hasList ? 'ok">wskazana' : 'bad">cala strona') + '</span>' +
      (pickMode ? '<br><b>Kliknij element na stronie…</b>' : '');
  }

  // ------------------------------------------------------- tryb wskazywania

  let highlightBox = null;

  function startPick(mode) {
    pickMode = mode;
    ui.panel.classList.remove('open');
    if (!highlightBox) {
      highlightBox = document.createElement('div');
      highlightBox.style.cssText =
        'position:fixed;z-index:2147483646;pointer-events:none;border:2px solid #4d96ff;' +
        'background:rgba(77,150,255,.15);border-radius:3px;';
      document.body.appendChild(highlightBox);
    }
    highlightBox.style.display = 'block';
    document.addEventListener('mousemove', onPickMove, true);
    document.addEventListener('click', onPickClick, true);
    document.addEventListener('keydown', onPickEsc, true);
    refreshUI();
  }

  function stopPick() {
    pickMode = null;
    if (highlightBox) highlightBox.style.display = 'none';
    document.removeEventListener('mousemove', onPickMove, true);
    document.removeEventListener('click', onPickClick, true);
    document.removeEventListener('keydown', onPickEsc, true);
    ui.panel.classList.add('open');
    refreshUI();
  }

  function onPickMove(e) {
    const el = e.target;
    if (!el || el.id === 'br-chat-color-host') return;
    const r = el.getBoundingClientRect();
    highlightBox.style.left = r.left + 'px';
    highlightBox.style.top = r.top + 'px';
    highlightBox.style.width = r.width + 'px';
    highlightBox.style.height = r.height + 'px';
  }

  function onPickEsc(e) {
    if (e.key === 'Escape') {
      e.preventDefault();
      e.stopPropagation();
      stopPick();
    }
  }

  function onPickClick(e) {
    const el = e.target;
    if (!el || el.id === 'br-chat-color-host') return;
    e.preventDefault();
    e.stopPropagation();

    const selector = cssPath(el);
    if (pickMode === 'input') {
      cfg.inputSelector = selector;
    } else {
      cfg.listSelector = selector;
      saveConfig();
      stopPick();
      attachListObserver();
      return;
    }
    saveConfig();
    stopPick();
  }

  // ---------------------------------------------------------------- start

  installWebSocketHook();

  function init() {
    document.addEventListener('keydown', onKeyDownCapture, true);
    document.addEventListener('submit', onSubmitCapture, true);
    document.addEventListener('pointerdown', onPointerDownCapture, true);
    document.addEventListener('input', onInputCapture, true);
    buildUI();
    attachListObserver();

    // Gra potrafi przebudowac DOM (nowa runda, powrot do lobby). Gdy obserwowany
    // kontener wypadnie z drzewa, podpinamy obserwatora ponownie.
    setInterval(() => {
      if (observedNode && !observedNode.isConnected) attachListObserver();
      else if (cfg.listSelector && observedNode === document.body) attachListObserver();
    }, 5000);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init, { once: true });
  } else {
    init();
  }
})();
