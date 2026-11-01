import { sha256 } from '@noble/hashes/sha2.js';
import { parseHTML } from 'linkedom';
import { topics } from '../shared/catalog.js';
export const hash = value => Array.from(sha256(new TextEncoder().encode(value)), n => n.toString(16).padStart(2, '0')).join('');
export const clean = value => parseHTML(`<div>${value || ''}</div>`).document.documentElement.textContent.replace(/\s+/g, ' ').trim();
export function canonicalUrl(value, base) {
  const u = new URL(value, base);
  if (!['http:', 'https:'].includes(u.protocol)) throw new Error('Geçersiz bağlantı');
  u.hash = '';
  for (const k of [...u.searchParams.keys()]) if (/^(utm_|fbclid|ref$)/.test(k)) u.searchParams.delete(k);
  u.searchParams.sort(); return u.href;
}
export function parseDate(value) {
  const m = value.match(/(20\d{2})[.\-/年](\d{1,2})[.\-/月](\d{1,2})/);
  if (m) { const result = `${m[1]}-${m[2].padStart(2, '0')}-${m[3].padStart(2, '0')}`; if (new Date(result).toISOString().startsWith(result)) return `${result}T00:00:00+09:00`; return null; }
  const n = Date.parse(value); return Number.isFinite(n) ? new Date(n).toISOString() : null;
}
function releaseDate(text) {
  // Only dates adjacent to release/broadcast wording; never infer from article date.
  const jp = text.match(/(20\d{2})年\s*(?:(\d{1,2})月\s*)?(?:(\d{1,2})日\s*)?(?:[（(][^）)]{0,12}[）)]\s*)?(?:より|から|に|全国|劇場|TVアニメ|テレビアニメ|TV|日本|全世界|配信|放送|公開|ロードショー|スタート|予定|決定|開始|[\s!！、])*?(?:放送|配信|公開|ロードショー|スタート)/);
  if (jp) { const [, y, m, d] = jp; if (m && (+m < 1 || +m > 12) || d && (+d < 1 || +d > 31)) return null; return { value: [y, m?.padStart(2, '0'), d?.padStart(2, '0')].filter(Boolean).join('-'), precision: d ? 'day' : m ? 'month' : 'year' }; }
  const en = text.match(/(?:premier(?:e|es|ing)|air(?:s|ing)?|broadcast(?:s|ing)?|releas(?:e|es|ing)|available|launch(?:es|ing)?)(?:\s+(?:on|in|from|for|is|set|to|will|be))*\s+((?:January|February|March|April|May|June|July|August|September|October|November|December)\s+\d{1,2}(?:st|nd|rd|th)?(?:,?\s+20\d{2})?|20\d{2})/i);
  if (en && /^20\d{2}$/.test(en[1])) return { value: en[1], precision: 'year' };
  if (en && /20\d{2}/.test(en[1])) { const d = Date.parse(en[1].replace(/(\d)(st|nd|rd|th)/g, '$1')); if (Number.isFinite(d)) return { value: new Date(d).toISOString().slice(0, 10), precision: 'day' }; }
  return null;
}
const normalTitle = t => t.normalize('NFKC').toLowerCase().replace(/【[^】]*】|[!！\s「」『』・:：.,。]/g, '');
export function classifyAnime(article) {
  const title = clean(article.title), body = clean(article.body || ''), text = `${title} ${body}`;
  const topic = topics.find(t => t.id === article.topicId) || topics.find(t => t.aliases.some(a => text.toLowerCase().includes(a.toLowerCase())));
  if (!topic) return [];
  // Do not turn game/merch/music/event/recap headlines into animation news.
  if (/Blu.?ray|DVD|グッズ|チケット|イベント|ライブ|コラボ|キャンペーン|一挙|再放送|振り返|完結記念|ゲーム|ノンクレジット|第[0-9０-９一二三四五六七八九十]+話.*(?:予告|先行|PV)|主題歌|CD|アルバム|舞台|ラジオ|発売|イラスト|連動企画|Sparking|DLC|licensing|collection|award|game|concert|livestream/i.test(title)) return [];
  if (/rumou?r|leak|unconfirmed|噂|リーク/i.test(title)) return [];
  const season = text.match(/(?:第\s*)?([0-9０-９]+)\s*期|(?:season|シーズン)\s*([0-9０-９]+)/i);
  const seasonId = season ? `season-${(season[1] || season[2]).normalize('NFKC')}` : null;
  const film = /映画|劇場版|movie|film/i.test(title);
  const project = seasonId || (film ? `film-${hash(normalTitle(title).replace(/20\d{2}年.*|公開.*|制作.*|製作.*/g, '')).slice(0, 12)}` : 'series');
  const result = [];
  function add(type, signature, release = null) {
    const label = type === 'project' ? `${topic.name}: ${film ? 'yeni film' : seasonId ? `${Number(seasonId.split('-')[1])}. sezon` : 'yeni animasyon projesi'} onaylandı` : type === 'release' ? `${topic.name}: yayın takvimi açıklandı` : `${topic.name}: resmî fragman yayımlandı`;
    const dateText = release ? release.precision === 'year' ? `${release.value} yılı` : release.precision === 'month' ? new Intl.DateTimeFormat('tr-TR', { month: 'long', year: 'numeric', timeZone: 'UTC' }).format(new Date(`${release.value}-01`)) : new Intl.DateTimeFormat('tr-TR', { dateStyle: 'long', timeZone: 'UTC' }).format(new Date(release.value)) : '';
    result.push({ categoryId: 'anime', topicId: topic.id, type, title: label,
      summary: type === 'project' ? `${topic.name} için ${film ? 'yeni bir anime filmi' : seasonId ? `${Number(seasonId.split('-')[1])}. sezon` : 'yeni bir animasyon projesi'} resmî kaynakta duyuruldu. Hikâye ayrıntıları özete alınmadı.` : type === 'release' ? `Resmî duyuruda ${topic.name} için ${dateText} yayın aralığı paylaşıldı. Bölge ve platform bilgisi için kaynağı kontrol et.` : `${topic.name} için resmî tanıtım videosu paylaşıldı. Spoiler içermemesi için video içeriği özetlenmedi.`,
      identity: `${topic.id}:${type}:${signature}`, releaseDate: release?.value || null, releasePrecision: release?.precision || null });
  }
  if (/続編.*(?:制作|製作).*決定|新(?:作|アニメーション|アニメ).*?(?:制作|製作|プロジェクト).*?(?:決定|始動)|(?:制作|製作)決定|(?:season\s*\d+|sequel|new\s+(?:anime|animation)).*?(?:confirm|announc|green.?lit)/i.test(title)) {
    // Season numbers give cross-source identity. Ambiguous projects only merge identical normalized titles.
    add('project', seasonId || hash(normalTitle(title)).slice(0, 24));
  }
  const release = releaseDate(title) || (/放送|配信|公開|premiere|broadcast|release date/i.test(title) ? releaseDate(body) : null);
  if (release) add('release', `${seasonId || (film ? project : 'series')}:${release.value}`, release);
  if (/(?:PV|ＰＶ|ティザー|トレーラー|特報|予告|trailer|teaser|プロモーションビデオ).*?(?:公開|解禁|発表|released|reveal)|(?:released|new|official).*?(?:trailer|teaser)|新.*PV/i.test(title)) {
    const video = (article.videoUrls || []).map(u => u.match(/(?:youtu\.be\/|embed\/|[?&]v=)([\w-]{11})/)?.[1]).find(Boolean);
    const ordinal = title.match(/第\s*(\d+)\s*弾/)?.[1];
    add('trailer', video ? `youtube-${video}` : ordinal && seasonId ? `${seasonId}:pv-${ordinal}` : hash(normalTitle(title)).slice(0, 24));
  }
  return result;
}
