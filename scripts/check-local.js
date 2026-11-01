import {readFile,writeFile,mkdir} from 'node:fs/promises';
import {initialData} from '../shared/local.js';
import {collectLocal} from '../shared/collect.js';
let data;try{data=JSON.parse(await readFile('data/local-check.json','utf8'));}catch{data=initialData();}
const before=data.events.length;
const fetcher=async(url,headers={})=>{const r=await fetch(url,{headers:{'User-Agent':'FallowThis/0.2 (local verification)',...headers},signal:AbortSignal.timeout(20000),redirect:'error'});if(r.status===304)return {notModified:true};if(!r.ok)throw new Error(`HTTP ${r.status}`);const text=await r.text();if(text.length>3_000_000)throw new Error('Kaynak boyutu sınırı aşıldı');return {text,etag:r.headers.get('etag')};};
const report=await collectLocal(data,fetcher);await mkdir('data',{recursive:true});await writeFile('data/local-check.json',JSON.stringify(data,null,2));await mkdir('artifacts',{recursive:true});await writeFile('artifacts/local-live-check.json',JSON.stringify({at:new Date().toISOString(),before,after:data.events.length,report},null,2));console.log(JSON.stringify({before,after:data.events.length,report},null,2));
if(!Object.values(data.status).some(s=>s.success_at&&!s.error))process.exitCode=1;
