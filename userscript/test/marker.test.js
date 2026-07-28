// Test czystej logiki znacznika, wyciagnietej 1:1 ze skryptu.
const MARKER_RE = /^\s*(?:\[c#|<c#|\{c#)([0-9a-fA-F]{6}|[0-9a-fA-F]{3})(?:\]|>|\})\s?/;
const MARKER_RE_ANY = /(?:\[c#|<c#|\{c#)([0-9a-fA-F]{6}|[0-9a-fA-F]{3})(?:\]|>|\})\s?/;

function normalizeHex(value) {
  let hex = String(value || '').trim().replace(/^#/, '');
  if (hex.length === 3) hex = hex.split('').map((c) => c + c).join('');
  return /^[0-9a-fA-F]{6}$/.test(hex) ? '#' + hex.toLowerCase() : null;
}
const buildMarker = (c) => '[c' + normalizeHex(c) + ']';

function decode(line) {
  const m = line.match(MARKER_RE) || line.match(MARKER_RE_ANY);
  if (!m) return { color: null, text: line };
  return { color: normalizeHex(m[1]), text: line.replace(m[0], '') };
}

const cases = [
  ['[c#ff5555]siema', '#ff5555', 'siema'],
  ['Kuba: [c#00d4c8]hej wszystkim', '#00d4c8', 'Kuba: hej wszystkim'],   // nick doklejony przez serwer
  ['<c#4d96ff>alt nawias', '#4d96ff', 'alt nawias'],
  ['{c#f5a}skrocony hex', '#ff55aa', 'skrocony hex'],
  ['zwykla wiadomosc', null, 'zwykla wiadomosc'],
  ['[c#zzzzzz]zly hex', null, '[c#zzzzzz]zly hex'],
  ['[c#ff5555]', '#ff5555', ''],
];

let fail = 0;
for (const [input, color, text] of cases) {
  const got = decode(input);
  const ok = got.color === color && got.text === text;
  if (!ok) fail++;
  console.log((ok ? 'OK  ' : 'BLAD') + '  ' + JSON.stringify(input) +
    ' -> ' + JSON.stringify(got));
}

// round-trip: to co wysylamy musi dac sie odczytac
for (const c of ['#ff5555', '#6bcB77', '#9b5de5']) {
  const line = buildMarker(c) + 'test';
  const got = decode(line);
  const ok = got.color === c.toLowerCase() && got.text === 'test';
  if (!ok) fail++;
  console.log((ok ? 'OK  ' : 'BLAD') + '  round-trip ' + c + ' -> ' + JSON.stringify(got));
}

// podwojne stemplowanie nie moze wystapic
const already = buildMarker('#ff5555') + 'siema';
console.log((MARKER_RE.test(already) ? 'OK  ' : 'BLAD') + '  wykrycie istniejacego znacznika');
if (!MARKER_RE.test(already)) fail++;

console.log(fail === 0 ? '\nWSZYSTKIE TESTY PRZESZLY' : '\nBLEDOW: ' + fail);
process.exit(fail ? 1 : 0);
