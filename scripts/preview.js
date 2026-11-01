// Optional PC preview only. Android never connects to this process.
import express from 'express';
import {resolve} from 'node:path';
import {sources} from '../shared/catalog.js';
async function fetchText(url,headers){const r=await fetch(url,{headers:{'User-Agent':'FallowThis/0.2 (local preview)',...headers},signal:AbortSignal.timeout(20000),redirect:'error'});if(r.status===304)return {notModified:true};if(!r.ok)throw new Error(`HTTP ${r.status}`);const body=await r.text();if(body.length>3_000_000)throw new Error('Kaynak boyutu sınırı aştı.');return {text:body,etag:r.headers.get('etag')};}
const app=express();
app.get('/local-source',async(req,res)=>{
  try{const u=new URL(String(req.query.url));if(!sources.some(s=>new URL(s.url).origin===u.origin)||u.username||u.password||u.port)throw new Error('Kaynak izin listesinde değil.');const result=await fetchText(u.href,req.headers['if-none-match']?{'If-None-Match':req.headers['if-none-match']}:{});if(result.notModified)return res.sendStatus(304);if(result.etag)res.set('ETag',result.etag);res.type('text/plain').send(result.text);}catch(e){res.status(502).json({error:e.message+(e.cause?.code?` (${e.cause.code})`:'')});}
});
app.use(express.static(resolve('dist')));app.get('/{*path}',(_req,res)=>res.sendFile(resolve('dist/index.html')));
app.listen(3001,'127.0.0.1',()=>console.log('YoriNovi local preview: http://localhost:3001 — Android does not require this process.'));
