import {mkdir,writeFile,access} from 'node:fs/promises';
import {createWriteStream} from 'node:fs';
import {Readable,Transform} from 'node:stream';
import {pipeline} from 'node:stream/promises';
import {createHash} from 'node:crypto';
const root='.android-build';await mkdir(root,{recursive:true});
async function download(url,path,expected){let exists=false;try{await access(path);exists=true;}catch{}
  if(exists){console.log(`Using existing ${path}`);return;}
  console.log(`Downloading ${path}`);const r=await fetch(url,{signal:AbortSignal.timeout(300000)});if(!r.ok)throw new Error(`HTTP ${r.status} downloading ${path}`);
  const sha=createHash('sha256');let bytes=0,last=0;const meter=new Transform({transform(chunk,_encoding,done){sha.update(chunk);bytes+=chunk.length;if(bytes-last>30_000_000){last=bytes;console.log(`${path}: ${Math.round(bytes/1e6)} MB`);}done(null,chunk);}});await pipeline(Readable.fromWeb(r.body),meter,createWriteStream(path));
  const digest=sha.digest('hex');if(expected&&expected.toLowerCase()!==digest)throw new Error(`SHA-256 mismatch: ${path}`);console.log(`Verified ${path} (${Math.round(bytes/1e6)} MB)`);
}
const packages=await fetch('https://api.adoptium.net/v3/assets/latest/21/hotspot?architecture=x64&image_type=jdk&os=windows&vendor=eclipse').then(r=>{if(!r.ok)throw new Error(`Adoptium HTTP ${r.status}`);return r.json();});
const jdk=packages[0].binary.package;
await download(jdk.link,`${root}/jdk.zip`,jdk.checksum);
await download('https://dl.google.com/android/repository/commandlinetools-win-15859902_latest.zip',`${root}/cmdline.zip`,'90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a');
