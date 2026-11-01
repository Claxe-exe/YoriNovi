import { classifyAnime, hash } from './anime.js';
import { matchesLocal } from './state.js';
export { initialData, matchesLocal, quietNow, validateBackup } from './state.js';
export function ingestArticle(data,source,article,{baseline=false,now=new Date()}={}){
  if(data.articles.includes(article.url))return 0;
  data.articles.push(article.url);let count=0;
  for(const e of classifyAnime(article)){
    let event=data.events.find(x=>x.identity===e.identity);
    if(!event){event={id:hash(e.identity).slice(0,32),identity:e.identity,category_id:e.categoryId,topic_id:e.topicId,type:e.type,title:e.title,summary:e.summary,published_at:article.publishedAt,release_date:e.releaseDate,release_precision:e.releasePrecision,original_title:article.title,read_at:null,sources:[]};data.events.push(event);count++;
      const age=now.getTime()-Date.parse(article.publishedAt);
      if(!baseline&&age>=0&&age<=48*3600000&&data.settings.notifications&&data.settings.types.includes(e.type)&&matchesLocal(data.settings,event))data.pending.push(event.id);
    }
    if(!event.sources.some(s=>s.url===article.url))event.sources.push({source_id:source.id,url:article.url,published_at:article.publishedAt});
    if(Date.parse(article.publishedAt)<Date.parse(event.published_at))event.published_at=article.publishedAt;
  }
  return count;
}
