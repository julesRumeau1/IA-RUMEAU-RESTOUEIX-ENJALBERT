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
// On attend un objet { newsCollection: [{ title, link, description, categoryScores: [{category, score}] }, ...] }
function normalizeNews(apiData){
  const raw = apiData?.newsCollection ?? [];
  return raw.map(item => {
    const title = item.title || 'Article';
    const link = item.link || '#';
    const summary = item.description || '';

    const themeLabel = pickThemeLabel(item.categoryScores);
    const tone = pickTone(item.categoryScores); // 'positive' | 'negative' | 'neutral'

    return { title, link, summary, themeLabel, tone };
  });
}

// Choisit un thème lisible à partir des catégories (on ignore Positif/Négatif/Neutre)
const NON_THEME_LABELS = new Set(['positif','négatif','negatif','neutre','neutral','positive','negative']);
function pickThemeLabel(categoryScores){
  if(!Array.isArray(categoryScores) || !categoryScores.length) return 'Général';
  // prend la catégorie avec le meilleur score, hors sentiment
  const filtered = categoryScores
    .filter(c => !NON_THEME_LABELS.has((c.category || '').toLowerCase()))
    .sort((a,b) => (b.score||0) - (a.score||0));
  return (filtered[0]?.category) || (categoryScores[0]?.category) || 'Général';
}

// Déduit la tonalité à partir des catégories "Positif"/"Négatif"/"Neutre"
function pickTone(categoryScores){
  if(!Array.isArray(categoryScores)) return 'neutral';
  const byName = {};
  for(const c of categoryScores){
    byName[(c.category || '').toLowerCase()] = c.score || 0;
  }
  const pos = byName['positif'] || byName['positive'] || 0;
  const neg = byName['négatif'] || byName['negatif'] || byName['negative'] || 0;
  const neu = byName['neutre']  || byName['neutral']  || 0;

  if(pos > neg && pos > neu) return 'positive';
  if(neg > pos && neg > neu) return 'negative';
  if(neu > pos && neu > neg) return 'neutral';
  // fallback simple : si rien, neutre
  return 'neutral';
}

// ==================== Fenêtre de résultats ====================
function openResults(news){
  const body = document.getElementById('resultsBody');
  body.innerHTML = '';

  const list = document.createElement('div');
  list.className = 'news-list';

  news.forEach(item => {
    const card = document.createElement('article');
    card.className = 'news-card';

    const title = document.createElement('h3');
    title.className = 'news-title';

    if(item.link && item.link !== '#'){
      const a = document.createElement('a');
      a.href = item.link; a.target = '_blank'; a.rel = 'noopener noreferrer';
      a.textContent = item.title;
      a.className = 'link';
      title.appendChild(a);
    } else {
      title.textContent = item.title;
    }

    const meta = document.createElement('div');
    meta.className = 'news-meta';

    const theme = document.createElement('span');
    theme.className = 'badge';
    theme.textContent = item.themeLabel ?? 'Général';

    const tone = document.createElement('span');
    tone.className = 'badge tone ' + (item.tone || 'neutral');
    tone.textContent = (item.tone ? item.tone.toUpperCase() : 'NEUTRE');

    meta.append(theme, tone);

    const summary = document.createElement('p');
    summary.className = 'news-summary';
    summary.textContent = item.summary || '';

    card.append(title, meta, summary);
    list.append(card);
  });

  body.appendChild(list);
  document.getElementById('results').showModal();
}

function getNewsCategoryScoreCollectionPayload() {
  const scores = [];

  for (const card of grid.children) {
    const category = card._theme.key;
    const score = Number(card._range.value);

    scores.push({ category, score });
  }

  return { scores };
}


// ==================== Appel API (réel, sans mock) ====================
document.getElementById('fetchBtn').addEventListener('click', async () => {
  const payload = getNewsCategoryScoreCollectionPayload();
  out.textContent = '';
  showLoading('Analyse de vos préférences…');

  try {
    const res = await fetch('http://localhost:8080/api/preferences', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      hideLoading();
      toast('Erreur côté serveur');
      return;
    }

    const data = await res.json();
    const news = normalizeNews(data);

    hideLoading();
    if (!news.length) {
      toast('Pas d’articles trouvés pour ces préférences');
      return;
    }
    openResults(news);

  } catch (e) {
    console.error(e);
    hideLoading();
    toast('Impossible de contacter l’API locale');
  }
});
