import {sources} from '../shared/catalog.js';
import {parseSource} from '../shared/parsers.js';
import {classifyAnime} from '../shared/anime.js';
import {mkdir,writeFile} from 'node:fs/promises';
const fixtures=await Promise.all(sources.map(async source=>{const r=await fetch(source.url,{signal:AbortSignal.timeout(20000),redirect:'error'});if(!r.ok)throw new Error(`${source.id}: HTTP ${r.status}`);const text=await r.text();const articles=parseSource(source,text);return {source,text,count:articles.length,identities:[...new Set(articles.flatMap(a=>classifyAnime(a).map(e=>e.identity)))].sort()};}));
await mkdir('artifacts/native-test',{recursive:true});await writeFile('artifacts/native-test/live.json',JSON.stringify(fixtures));console.log('Saved real source fixtures:',fixtures.map(f=>({id:f.source.id,articles:f.count,events:f.identities.length})));
