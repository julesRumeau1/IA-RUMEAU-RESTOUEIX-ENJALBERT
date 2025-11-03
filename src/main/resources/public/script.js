// ==================== Config UI ====================
const THEMES = [
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
  { key: 'idees', label: 'Idées', rss: 'https://www.lemonde.fr/idees/rss_full.xml' }
];

const grid = document.getElementById('grid');

// ==================== Cartes thèmes ====================
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

// NEW: payload typé { ts, themes: { sciences: {level, rss}, ... } }
function getPayloadTyped(){
  const themes = {};
  for(const card of grid.children){
    const k = card._theme.key;
    themes[k] = { level: Number(card._range.value), rss: card._theme.rss };
  }
  return { ts: new Date().toISOString(), themes };
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

document.getElementById('clearBtn').addEventListener('click', () => {
  [...grid.children].forEach(card => { card._range.value = 1; card._chip.textContent = '1'; updateTrack(card._range, card._fill); });
});

const out = document.getElementById('out');

document.getElementById('copyJson').addEventListener('click', async () => {
  const payload = getPayloadTyped();
  const json = JSON.stringify(payload, null, 2);
  try{ await navigator.clipboard.writeText(json); toast('JSON copié dans le presse-papiers'); }
  catch{ toast('Impossible de copier automatiquement. Le JSON est affiché ci-dessous.'); out.textContent = json; }
});

document.getElementById('previewBtn').addEventListener('click', () => {
  const payload = getPayloadTyped();
  const body = document.getElementById('modalBody');
  body.innerHTML = '';
  const list = document.createElement('div');
  list.style.display = 'grid'; list.style.gridTemplateColumns = '1fr auto'; list.style.gap = '8px 12px';
  Object.entries(payload.themes).forEach(([k,v])=>{
    const label = THEMES.find(t=>t.key===k)?.label ?? k;
    const l = document.createElement('div'); l.textContent = label;
    const r = document.createElement('div'); r.textContent = v.level; r.style.textAlign='right'; r.style.opacity=.8;
    list.append(l,r);
  });
  body.appendChild(list);
  document.getElementById('modal').showModal();
});

// ==================== Loader + Toast ====================
let toastTimer = null;
function toast(msg){
  const el = document.getElementById('toast');
  el.textContent = msg; el.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(()=> el.classList.remove('show'), 2200);
}
function showLoading(text='Analyse en cours…'){
  const el = document.getElementById('loading');
  el.querySelector('.loading-text').textContent = text;
  el.hidden = false;
}
function hideLoading(){
  document.getElementById('loading').hidden = true;
}

// ==================== Normalisation des données backend ====================
// On attend un objet { newsCollection: [{ title, link, description }, ...] }
function normalizeNews(apiData){
  const raw = apiData?.newsCollection ?? [];
  return raw.map(item => {
    const title = item.title || 'Article';
    const link = item.link || '#';
    const summary = item.description || '';

    return { title, link, summary };
  });
}

// ==================== Fenêtre de résultats ====================
function openResults(news, opts = {}) {
  const body = document.getElementById('resultsBody');
  body.innerHTML = '';

  const items = Array.isArray(news) ? news : [];
  const emptyMessage = opts.emptyMessage || 'Aucun article ne correspond à vos préférences.';
  const emptyHint = opts.emptyHint || 'Essayez d’augmenter le niveau d’un autre thème ou de revenir à des valeurs plus générales.';

  if (!items.length) {
    const box = document.createElement('div');
    box.className = 'empty-state';
    box.innerHTML = `
      <h3 style="margin-bottom:8px">Aucun résultat</h3>
      <p style="margin-bottom:4px">${emptyMessage}</p>
      <p style="font-size:.85rem; opacity:.7">${emptyHint}</p>
    `;
    body.appendChild(box);
    document.getElementById('results').showModal();
    return;
  }

  const list = document.createElement('div');
  list.className = 'news-list';

  items.forEach(item => {
    const card = document.createElement('article');
    card.className = 'news-card';

    const title = document.createElement('h3');
    title.className = 'news-title';

    if (item.link && item.link !== '#') {
      const a = document.createElement('a');
      a.href = item.link;
      a.target = '_blank';
      a.rel = 'noopener noreferrer';
      a.textContent = item.title;
      a.className = 'link';
      title.appendChild(a);
    } else {
      title.textContent = item.title;
    }

    const summary = document.createElement('p');
    summary.className = 'news-summary';
    summary.textContent = item.summary || '';

    card.append(title, summary);
    list.append(card);
  });

  body.appendChild(list);
  document.getElementById('results').showModal();
}


// ==================== Appel API (réel, sans mock) ====================
document.getElementById('fetchBtn').addEventListener('click', async () => {
  const payload = getPayloadTyped();
  out.textContent = '';
  showLoading('Analyse de vos préférences…');

  try {
    const res = await fetch('/api/preferences', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    const news = normalizeNews(data);

    hideLoading();

    openResults(news, {
      emptyMessage: 'Pas d’articles trouvés pour ces préférences.',
      emptyHint: 'Essayez d’augmenter un autre thème ou de baisser vos filtres.'
    });

  } catch (e) {
    console.error(e);
    hideLoading();
    toast('Impossible de contacter l’API locale');
  }
});