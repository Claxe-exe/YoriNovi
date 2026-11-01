import {sources} from '../shared/catalog.js';
import {classifyAnime} from './anime.js';
import {parseSource,parseDetail} from './parsers.js';
import {ingestArticle} from './local.js';
export async function collectLocal(data,fetcher,{now=new Date(),onProgress=(_name)=>{}}={}){
  const report=[];
  for(const source of sources){
    onProgress(source.name);const previous=data.status[source.id];let added=0,count=0;const warnings=[];
    try{
      const response=await fetcher(source.url,previous?.etag?{'If-None-Match':previous.etag}:{});
      if(response.notModified){data.status[source.id]={...previous,checked_at:now.toISOString(),success_at:now.toISOString(),error:null};continue;}
      let articles=parseSource(source,response.text);
      if(!previous?.initialized)for(let p=2;p<=source.pages;p++){const u=new URL(source.url);u.searchParams.set('p',String(p));try{articles.push(...parseSource(source,(await fetcher(u.href)).text));}catch(e){warnings.push(`Arşiv: ${e.message}`);}}
      for(const a of [...new Map(articles.map(a=>[a.url,a])).values()]){count++;if(data.articles.includes(a.url))continue;if(!a.body&&classifyAnime(a).length)try{Object.assign(a,parseDetail(source,(await fetcher(a.url)).text));}catch(e){warnings.push(`Detay: ${e.message}`);}added+=ingestArticle(data,source,a,{baseline:!previous?.initialized,now});}
      data.status[source.id]={checked_at:now.toISOString(),success_at:now.toISOString(),error:warnings.length?warnings.slice(0,2).join('; '):null,initialized:true,fetched:count,accepted:added,etag:response.etag||null};report.push({id:source.id,fetched:count,added});
    }catch(e){data.status[source.id]={...previous,checked_at:now.toISOString(),error:e.message};report.push({id:source.id,error:e.message});}
  }
  data.lastCheck=now.toISOString();return report;
}
