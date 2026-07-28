// Test skryptu w symulowanym DOM-ie: udajemy czat gry i sprawdzamy oba kierunki.
const fs = require('fs');
const { JSDOM, VirtualConsole } = require('jsdom');

const SCRIPT = fs.readFileSync(require('path').join(__dirname, '..', 'bubleroyal-chat-color.user.js'), 'utf8');

const vc = new VirtualConsole();
vc.on('jsdomError', (e) => { console.log('BLAD JS:', e.message); process.exitCode = 1; });

const dom = new JSDOM(`<!doctype html><html><body>
  <div id="chat-log"></div>
  <input id="chat-input" placeholder="Napisz wiadomosc...">
</body></html>`, {
  runScripts: 'dangerously', pretendToBeVisual: true,
  url: 'https://bubleroyal.com/', virtualConsole: vc,
});

const w = dom.window;
const d = w.document;

// Konfiguracja taka, jaka powstaje po wskazaniu elementow w panelu.
w.localStorage.setItem('brChatColor.cfg.v1', JSON.stringify({
  enabled: true, color: '#ff5555',
  inputSelector: '#chat-input', listSelector: '#chat-log',
  colorWholeLine: false, wsHook: false,
}));

// jsdom nie liczy layoutu - podajemy rozmiary, zeby autodetekcja pola dzialala.
w.Element.prototype.getBoundingClientRect = () => ({
  width: 300, height: 30, top: 500, left: 0, right: 0, bottom: 0,
});

// Gra: po Enterze czyta pole i "wysyla" tresc na serwer.
const wyslane = [];
d.getElementById('chat-input').addEventListener('keydown', (e) => {
  if (e.key === 'Enter' && !e.shiftKey) { wyslane.push(e.target.value); e.target.value = ''; }
});

const tag = d.createElement('script');
tag.textContent = SCRIPT;
d.head.appendChild(tag);

let fail = 0;
const check = (name, ok, extra = '') => {
  if (!ok) fail++;
  console.log((ok ? 'OK  ' : 'BLAD') + '  ' + name + (extra ? '  ' + extra : ''));
};

w.addEventListener('load', () => {
  check('panel UI wstrzykniety', !!d.getElementById('br-chat-color-host'));

  // --- kierunek 1: wysylanie ---
  const input = d.getElementById('chat-input');
  input.value = 'siema wszystkim';
  input.dispatchEvent(new w.Event('input', { bubbles: true }));
  input.dispatchEvent(new w.KeyboardEvent('keydown', { key: 'Enter', bubbles: true }));
  check('znacznik doklejony do wychodzacej wiadomosci',
    wyslane[0] === '[c#ff5555]siema wszystkim', JSON.stringify(wyslane[0]));

  // Shift+Enter (nowa linia) nie moze stemplowac.
  input.value = 'druga linia';
  input.dispatchEvent(new w.KeyboardEvent('keydown', { key: 'Enter', shiftKey: true, bubbles: true }));
  check('Shift+Enter nie stempluje', input.value === 'druga linia', JSON.stringify(input.value));
  input.value = '';

  // --- kierunek 2: odbieranie ---
  const log = d.getElementById('chat-log');
  const wiersz = d.createElement('div');
  wiersz.innerHTML = '<b>Kuba:</b> <span class="msg">[c#4d96ff]hej ludzie</span>';
  log.appendChild(wiersz);

  const zwykla = d.createElement('div');
  zwykla.textContent = 'zwykla wiadomosc';
  log.appendChild(zwykla);

  setTimeout(() => {
    const msg = wiersz.querySelector('.msg');
    check('znacznik wyciety z odebranej wiadomosci',
      msg.textContent === 'hej ludzie', JSON.stringify(msg.textContent));
    check('kolor nadany odebranej wiadomosci',
      msg.style.color === 'rgb(77, 150, 255)', msg.style.color);
    check('nick nadawcy nie zmienia koloru',
      !wiersz.querySelector('b').style.color);
    check('wiadomosc bez znacznika bez zmian',
      zwykla.textContent === 'zwykla wiadomosc' && !zwykla.style.color);

    console.log(fail === 0 ? '\nWSZYSTKIE TESTY DOM PRZESZLY' : '\nBLEDOW: ' + fail);
    w.close();
    process.exit(fail ? 1 : 0);
  }, 50);
});
