import {parseHTML} from 'linkedom';
import {XMLParser} from 'fast-xml-parser';
import {canonicalUrl,clean,parseDate} from './anime.js';
export function parseSource(source,text){
  if(source.adapter==='rss'){
    let items=new XMLParser().parse(text).rss?.channel?.item;
    if(!items)throw new Error('Geçerli RSS öğesi bulunamadı.');if(!Array.isArray(items))items=[items];
    return items.map(i=>({title:clean(i.title),body:i['content:encoded']||i.description||'',url:canonicalUrl(i.link,source.url),publishedAt:parseDate(i.pubDate||''),videoUrls:[...String(i['content:encoded']||'').matchAll(/(?:src|href)="([^"]+)"/g)].map(m=>m[1])})).filter(a=>a.publishedAt);
  }
  const doc=parseHTML(text).document;const rows=[];
  if(source.adapter==='rezero'){
    for(const e of doc.querySelectorAll('.content-entry[id]')){const publishedAt=parseDate(e.querySelector('.entry-date')?.textContent||'');if(publishedAt)rows.push({title:e.querySelector('.entry-title')?.textContent.trim()||'',body:e.querySelector('.entry-body')?.textContent||'',url:`${source.url}#${e.id}`,publishedAt,topicId:source.topicId,videoUrls:[...e.querySelectorAll('iframe[src],a[href]')].map(a=>a.getAttribute('src')||a.getAttribute('href'))});}
  }else{
    const selectors={aniplex:['.p-news__list-item','.p-news_data__title','.p-news_data__date'],bocchi:['.newslist__item','.newslist__title','.newslist__date'],kimetsu:['.news_list__item','.news_list__item-title','.news_list__item-date']};
    const [row,title,date]=source.selectors||selectors[source.adapter]||[];if(!row)throw new Error('Kaynak adaptörü bulunamadı.');
    for(const e of doc.querySelectorAll(row)){
      const href=e.querySelector('a[href]')?.getAttribute('href');const dateText=source.adapter==='kimetsu'?`${e.querySelector('.-year')?.textContent}.${e.querySelector('.-day')?.textContent}`:e.querySelector(date)?.textContent;
      const publishedAt=parseDate(dateText?.trim()||'');const t=e.querySelector(title)?.textContent.trim();if(!href||!publishedAt||!t)continue;
      const url=canonicalUrl(href,source.url);if(new URL(url).origin!==new URL(source.url).origin)continue;
      rows.push({title:t,url,publishedAt,topicId:source.topicId,body:'',videoUrls:[]});
    }
  }
  if(!rows.length)throw new Error('Haber seçicisi eşleşmedi; kaynak değişmiş olabilir.');
  return [...new Map(rows.map(a=>[a.url,a])).values()];
}
export function parseDetail(source,text){const doc=parseHTML(text).document;const el=doc.querySelector(source.detail);return {body:el?.textContent.slice(0,15000)||'',videoUrls:el?[...el.querySelectorAll('iframe[src],a[href]')].map(e=>e.getAttribute('src')||e.getAttribute('href')):[]};}
