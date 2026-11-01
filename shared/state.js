import { defaultState, topics, sources } from './catalog.js';

export function initialData() {return {format:'fallow-local',version:1,settings:{...defaultState(),theme:'dark',backgroundEnabled:true,pollMinutes:240,phoneScheduleVersion:2,wifiOnly:false},events:[],articles:[],status:{},pending:[],lastCheck:null};}
export function matchesLocal(settings,event){const topic=topics.find(t=>t.id===event.topic_id);return settings.favorites.includes(event.topic_id)||(settings.categories.includes(event.category_id)&&(settings.scope==='all'||topic?.tier==='established'));}
export function quietNow(s,now=new Date()){if(!s.quietEnabled||s.quietStart===s.quietEnd)return false;const t=new Intl.DateTimeFormat('en-GB',{timeZone:s.timezone,hour:'2-digit',minute:'2-digit',hourCycle:'h23'}).format(now);return s.quietStart<s.quietEnd?t>=s.quietStart&&t<s.quietEnd:t>=s.quietStart||t<s.quietEnd;}
export function validateBackup(input){
  if(!input||input.format!=='fallow-local'||input.version!==1||!Array.isArray(input.events)||input.events.length>10000||!Array.isArray(input.articles)||input.articles.length>100000)throw new Error('Geçerli bir YoriNovi yedeği seç.');
  const base=initialData();const p={...base.settings,...input.settings};
  for(const k of ['categories','favorites','types'])if(!Array.isArray(p[k])||p[k].some(v=>typeof v!=='string')||p[k].length>1000)throw new Error('Yedek tercihleri geçersiz.');
  for(const k of ['notifications','quietEnabled','backgroundEnabled','wifiOnly'])if(typeof p[k]!=='boolean')throw new Error('Yedek ayarları geçersiz.');
  if(!['light','dark','system'].includes(p.theme)||!['all','established'].includes(p.scope)||![15,30,60,180,240].includes(p.pollMinutes)||!/^([01]\d|2[0-3]):[0-5]\d$/.test(p.quietStart)||!/^([01]\d|2[0-3]):[0-5]\d$/.test(p.quietEnd))throw new Error('Yedek ayarları geçersiz.');
  try{new Intl.DateTimeFormat('tr',{timeZone:p.timezone});}catch{throw new Error('Yedek saat dilimi geçersiz.');}
  const safeUrl=(v,id)=>{try{const source=sources.find(s=>s.id===id);return source&&new URL(v).protocol==='https:'&&new URL(v).origin===new URL(source.url).origin;}catch{return false;}};
  for(const e of input.events)if(!e||!['project','release','trailer'].includes(e.type)||typeof e.id!=='string'||e.id.length>100||typeof e.identity!=='string'||!topics.some(t=>t.id===e.topic_id)||e.category_id!=='anime'||!Number.isFinite(Date.parse(e.published_at))||!['title','summary','original_title'].every(k=>typeof e[k]==='string'&&e[k].length<20000)||!Array.isArray(e.sources)||e.sources.length>100||e.sources.some(s=>typeof s.source_id!=='string'||!safeUrl(s.url,s.source_id)||!Number.isFinite(Date.parse(s.published_at))))throw new Error('Yedekteki haber kaydı geçersiz.');
  // Imported history must never generate fresh notifications or trusted source-health claims.
  return {...base,settings:p,events:input.events.map(e=>({...e,read_at:typeof e.read_at==='string'?e.read_at:null})),articles:input.articles.filter(a=>typeof a==='string'&&a.length<4096),status:{},pending:[],lastCheck:null};
}
