const THEMES = [
  { key: 'a_la_une', label: 'À la une', rss: 'https://www.lemonde.fr/rss/une.xml' },
  { key: 'politique', label: 'Politique', rss: 'https://www.lemonde.fr/politique/rss_full.xml' },
  { key: 'international', label: 'International', rss: 'https://www.lemonde.fr/international/rss_full.xml' },
  { key: 'economie', label: 'Économie', rss: 'https://www.lemonde.fr/economie/rss_full.xml' },
  { key: 'societe', label: 'Société', rss: 'https://www.lemonde.fr/societe/rss_full.xml' },
  { key: 'sport', label: 'Sport', rss: 'https://www.lemonde.fr/sport/rss_full.xml' },
  { key: 'culture', label: 'Culture', rss: 'https://www.lemonde.fr/culture/rss_full.xml' },
  { key: 'sciences', label: 'Sciences', rss: 'https://www.lemonde.fr/sciences/rss_full.xml' },
  { key: 'planete', label: 'Planète', rss: 'https://www.lemonde.fr/planete/rss_full.xml' },
  { key: 'technologies', label: 'Tech', rss: 'https://www.lemonde.fr/pixels/rss_full.xml' },
  { key: 'sante', label: 'Santé', rss: 'https://www.lemonde.fr/sante/rss_full.xml' },
  { key: 'education', label: 'Éducation', rss: 'https://www.lemonde.fr/education/rss_full.xml' },
  { key: 'idees', label: 'Idées', rss: 'https://www.lemonde.fr/idees/rss_full.xml' },
  { key: 'm_afrique', label: 'Afrique', rss: 'https://www.lemonde.fr/afrique/rss_full.xml' }
];

const grid = document.getElementById('grid');

function createCard(theme){
  const card = document.createElement('article');
  card.className = 'card fade-in';

  const head = document.createElement('div');
  head.className = 'topic-head';
  const title = document.createElement('div');
  title.className = 'topic-name';
  title.textContent = theme.label;

  const chip = document.createElement('span');
  chip.className = 'value-chip';
  chip.textContent = '3';
  chip.setAttribute('aria-live','polite');
  head.append(title, chip);

  const box = document.createElement('div');
  box.style.position = 'relative';

  const fill = document.createElement('div');
  fill.className = 'track-fill';
  fill.style.width = '50%';

  const range = document.createElement('input');
  range.type = 'range';
  range.className = 'range';
  range.min = '1'; range.max = '5'; range.step = '1'; range.value = '3';
  range.setAttribute('aria-label', `Niveau pour ${theme.label}`);
  range.addEventListener('input', () => {
    chip.textContent = range.value;
    updateTrack(range, fill);
  });

  box.append(fill, range);

  const legend = document.createElement('div');
  legend.className = 'legend';
  legend.innerHTML = '<span>1</span><span>2</span><span>3</span><span>4</span><span>5</span>';

  card.append(head, box, legend);

  // keep refs
  card._range = range; card._fill = fill; card._chip = chip; card._theme = theme;
  return card;
}

function updateTrack(range, fill){
  const min = +range.min, max = +range.max, val = +range.value;
  const pct = ((val - min) / (max - min)) * 100;
  fill.style.width = pct + '%';
}

function getPayload(){
  const weights = {};
  const feeds = {};
  for(const card of grid.children){
    const k = card._theme.key;
    weights[k] = Number(card._range.value);
    feeds[k] = card._theme.rss;
  }
  return { weights, feeds, ts: new Date().toISOString() };
}

// Build grid
THEMES.forEach(t => grid.appendChild(createCard(t)));

// Reveal on scroll animation
const io = new IntersectionObserver((entries)=>{
  for(const e of entries){
    if(e.isIntersecting){ e.target.classList.add('reveal'); io.unobserve(e.target); }
  }
}, {threshold:.1});
[...grid.children].forEach(el=>io.observe(el));

// Initialize track fills
[...grid.children].forEach(card => updateTrack(card._range, card._fill));

// Toolbar actions
document.getElementById('resetBtn').addEventListener('click', () => {
  [...grid.children].forEach(card => { card._range.value = 3; card._chip.textContent = '3'; updateTrack(card._range, card._fill); });
});

document.getElementById('randomBtn').addEventListener('click', () => {
  [...grid.children].forEach(card => { const v = Math.floor(Math.random()*5)+1; card._range.value = v; card._chip.textContent = String(v); updateTrack(card._range, card._fill); });
});

const out = document.getElementById('out');

document.getElementById('copyJson').addEventListener('click', async () => {
  const payload = getPayload();
  const json = JSON.stringify(payload, null, 2);
  try{
    await navigator.clipboard.writeText(json);
    toast('JSON copié dans le presse‑papiers');
  }catch{
    toast('Impossible de copier automatiquement. Le JSON est affiché ci‑dessous.');
    out.textContent = json;
  }
});

document.getElementById('previewBtn').addEventListener('click', () => {
  const payload = getPayload();
  const body = document.getElementById('modalBody');
  body.innerHTML = '';
  const list = document.createElement('div');
  list.style.display = 'grid';
  list.style.gridTemplateColumns = '1fr auto';
  list.style.gap = '8px 12px';
  Object.entries(payload.weights).forEach(([k,v])=>{
    const label = THEMES.find(t=>t.key===k)?.label ?? k;
    const l = document.createElement('div'); l.textContent = label;
    const r = document.createElement('div'); r.textContent = v; r.style.textAlign='right'; r.style.opacity=.8;
    list.append(l,r);
  });
  body.appendChild(list);
  document.getElementById('modal').showModal();
});

// Fetch button — placeholder AJAX
document.getElementById('fetchBtn').addEventListener('click', async () => {
  const payload = getPayload();
  out.textContent = 'Envoi…';
  try{
    // 🔧 À brancher côté serveur : remplacez l’URL par votre endpoint
    const res = await fetch('/api/fetch-news', {
      method: 'POST', headers: {'Content-Type':'application/json'}, body: JSON.stringify(payload)
    });
    if(res.ok){
      toast('Préférences envoyées. Traitement des flux en cours côté serveur.');
      out.textContent = '';
    }else{
      toast('Requête envoyée, mais le serveur a répondu avec une erreur.');
      out.textContent = '';
    }
  }catch(err){
    console.error(err);
    toast('Impossible de contacter le serveur (mode démo).');
    out.textContent = '';
  }
});

// Toast helper
let toastTimer = null;
function toast(msg){
  const el = document.getElementById('toast');
  el.textContent = msg; el.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(()=> el.classList.remove('show'), 2200);
}
