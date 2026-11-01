import {Capacitor,registerPlugin} from '@capacitor/core';
import {categories,topics,sources,newsTypes,unavailableSources} from '../shared/catalog.js';
import {initialData,validateBackup,matchesLocal,quietNow} from '../shared/state.js';
import type {News,Preferences,Source} from './types';
export type LocalSettings=Preferences&{backgroundEnabled:boolean;pollMinutes:number;wifiOnly:boolean;phoneScheduleVersion?:number};
export type LocalData={format:'fallow-local';version:1;settings:LocalSettings;events:News[];articles:string[];status:Record<string,NonNullable<Source['status']>&{etag?:string|null}>;pending:string[];lastCheck:string|null};
type NativeMonitor={snapshot:()=>Promise<{data:LocalData}>;refresh:()=>Promise<{data:LocalData}>;saveSettings:(o:{settings:LocalSettings})=>Promise<{data:LocalData}>;markRead:(o:{id:string;read:boolean})=>Promise<{data:LocalData}>;notificationPermission:()=>Promise<{granted:boolean}>;requestNotificationPermission:()=>Promise<{granted:boolean}>;testNotification:()=>Promise<{posted:boolean}>;exportBackup:()=>Promise<void>;importBackup:(o:{data:LocalData})=>Promise<{data:LocalData}>;pendingNotification:()=>Promise<{eventId:string|null}>;addListener:(name:string,fn:(e:{eventId:string})=>void)=>Promise<{remove:()=>Promise<void>}>};
const Monitor=registerPlugin<NativeMonitor>('FallowMonitor');
export const native=Capacitor.getPlatform()==='android';
export async function watchOpen(fn:(id:string)=>void){if(!native)return {remove:async()=>{}};const handle=await Monitor.addListener('notificationOpened',e=>fn(e.eventId));const pending=await Monitor.pendingNotification();if(pending.eventId&&pending.eventId!=='test')fn(pending.eventId);return handle;}
export const catalog={categories,topics,sources,newsTypes,unavailableSources};
let database:Promise<IDBDatabase>|null=null;
function db(){if(!database)database=new Promise((resolve,reject)=>{const req=indexedDB.open('fallow-local',1);req.onupgradeneeded=()=>req.result.createObjectStore('data');req.onsuccess=()=>resolve(req.result);req.onerror=()=>reject(new Error('Cihaz depolaması açılamadı.'));});return database;}
async function webRead():Promise<LocalData>{const d=await db();return new Promise((resolve,reject)=>{const r=d.transaction('data').objectStore('data').get('main');r.onsuccess=()=>{const value=r.result||initialData();if(value.settings.phoneScheduleVersion!==2){value.settings.pollMinutes=240;value.settings.phoneScheduleVersion=2;}resolve(value);};r.onerror=()=>reject(r.error);});}
async function webMutate(fn:(d:LocalData)=>LocalData):Promise<LocalData>{const d=await db();return new Promise((resolve,reject)=>{const tx=d.transaction('data','readwrite'),store=tx.objectStore('data'),r=store.get('main');let result:LocalData;r.onsuccess=()=>{try{const value=r.result||initialData();if(value.settings.phoneScheduleVersion!==2){value.settings.pollMinutes=240;value.settings.phoneScheduleVersion=2;}result=fn(value);store.put(result,'main');}catch(e){tx.abort();reject(e);}};tx.oncomplete=()=>resolve(result);tx.onerror=()=>reject(tx.error);});}
export async function readLocal(){return native?(await Monitor.snapshot()).data:webRead();}
export async function saveSettings(settings:LocalSettings){const valid=validateBackup({...initialData(),settings}).settings as LocalSettings;return native?(await Monitor.saveSettings({settings:valid})).data:webMutate(d=>({...d,settings:valid}));}
export async function markRead(id:string,read=true){if(native)return (await Monitor.markRead({id,read})).data;return webMutate(d=>({...d,events:d.events.map(e=>e.id===id?{...e,read_at:read?new Date().toISOString():null}:e)}));}
async function fetchSource(url:string,headers:Record<string,string>={}){
  const local=['localhost','127.0.0.1'].includes(location.hostname);
  const target=local?`/local-source?url=${encodeURIComponent(url)}`:url;
  const r=await fetch(target,{headers,signal:AbortSignal.timeout(20000),redirect:'error'});if(r.status===304)return {notModified:true};
  if(!r.ok){const body=await r.json().catch(()=>null);throw new Error(body?.error||`HTTP ${r.status}`);}const text=await r.text();if(text.length>3_000_000)throw new Error('Kaynak boyut sınırını aştı.');return {text,etag:r.headers.get('etag')};
}
let scan:Promise<LocalData>|null=null;
export async function refreshLocal(onProgress:(name:string)=>void=()=>{}):Promise<LocalData>{
  if(scan)return scan;
  scan=(async()=>{if(native)return (await Monitor.refresh()).data;
    const run=async()=>{const {collectLocal}=await import('../shared/collect.js');const data=await webRead();await collectLocal(data,fetchSource,{onProgress});
      return webMutate(current=>{const reads=new Map(current.events.map(e=>[e.id,e.read_at]));return {...data,settings:current.settings,events:data.events.map(e=>({...e,read_at:reads.has(e.id)?reads.get(e.id)!:e.read_at}))};});};
    const data=navigator.locks?await navigator.locks.request('fallow-source-scan',run):await run();await deliverBrowser(data);return webRead();
  })();try{return await scan;}finally{scan=null;}
}
async function deliverBrowser(data:LocalData){if(!data.settings.notifications||!('Notification'in window)||Notification.permission!=='granted'||quietNow(data.settings))return;
  const done:string[]=[];
  for(const id of data.pending){const e=data.events.find(e=>e.id===id);if(!e)continue;if(Date.now()-Date.parse(e.published_at)>48*3600000||!matchesLocal(data.settings,e)||!data.settings.types.includes(e.type)){done.push(id);continue;}
    const n=new Notification(e.title,{body:e.summary,tag:e.id,icon:'/icon-192.png'});n.onclick=()=>{window.focus();window.dispatchEvent(new CustomEvent('fallow:open',{detail:e.id}));};done.push(id);
  }if(done.length)await webMutate(d=>({...d,pending:d.pending.filter(id=>!done.includes(id))}));
}
export async function notificationPermission(){if(native)return (await Monitor.notificationPermission()).granted;return 'Notification'in window&&Notification.permission==='granted';}
export async function requestNotificationPermission(){if(native)return (await Monitor.requestNotificationPermission()).granted;if(!('Notification'in window))throw new Error('Bu tarayıcı bildirim desteklemiyor.');return await Notification.requestPermission()==='granted';}
export async function testNotification(){if(native)return (await Monitor.testNotification()).posted;if(await notificationPermission()){new Notification('YoriNovi · Yerel test',{body:'Bu bildirimi cihazında görüyorsan gösterimi doğrulayabilirsin.',icon:'/icon-192.png',tag:'fallow-test'});return true;}throw new Error('Önce bildirim izni ver.');}
export async function exportBackup(){if(native)return Monitor.exportBackup();const data=await webRead();const blob=new Blob([JSON.stringify(data)],{type:'application/json'});const url=URL.createObjectURL(blob);const a=document.createElement('a');a.href=url;a.download=`yorinovi-${new Date().toISOString().slice(0,10)}.json`;a.click();setTimeout(()=>URL.revokeObjectURL(url),10000);}
export async function importBackup(file:File){if(file.size>15_000_000)throw new Error('Yedek en fazla 15 MB olabilir.');const data=validateBackup(JSON.parse(await file.text())) as LocalData;return native?(await Monitor.importBackup({data})).data:webMutate(()=>data);}
