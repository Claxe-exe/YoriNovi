import {mkdir,writeFile} from 'node:fs/promises';
import {classifyAnime} from '../shared/anime.js';
const directory='artifacts/native-test';await mkdir(directory,{recursive:true});
for(const [url,file] of [['https://repo.maven.apache.org/maven2/org/jsoup/jsoup/1.21.2/jsoup-1.21.2.jar','jsoup.jar'],['https://repo.maven.apache.org/maven2/org/json/json/20250517/json-20250517.jar','json.jar']]){const r=await fetch(url);if(!r.ok)throw new Error(`HTTP ${r.status}`);await writeFile(`${directory}/${file}`,Buffer.from(await r.arrayBuffer()));}
const articles=[
  {title:'TVアニメ「リコリス・リコイル」第2期 制作決定！',topicId:'lycoris'},
  {title:'Lycoris Recoil season 2 confirmed!',topicId:'lycoris'},
  {title:'「映画ぼっち・ざ・ろっく！ 星のあしあと」2027年公開決定！',topicId:'bocchi'},
  {title:'第2期 2027年4月3日より放送開始！',topicId:'lycoris'},
  {title:'第2期 PV公開！',topicId:'lycoris',videoUrls:['https://youtu.be/abcdefghijk']},
  {title:'Lycoris Recoil official trailer revealed',topicId:'lycoris',videoUrls:['https://www.youtube.com/watch?v=abcdefghijk']},
  {title:'新キャラクターデザイン・小田景門による2期制作決定イラスト到着！',topicId:'bocchi'},
  {title:'アニメ「ぼっち・ざ・ろっく！」２期制作決定！',topicId:'bocchi'},
  {title:'Blu-ray&DVD 第2期PV公開！',topicId:'lycoris'},
  {title:'DRAGON BALL: Sparking! ZERO gets a new DLC!',topicId:'dragonball'},
  {title:'Lycoris Recoil season 2 rumour leaked',topicId:'lycoris'},
  {title:'アニメ連動企画「結束バンドへの道」ティザー映像公開！',topicId:'bocchi'},
];
await writeFile(`${directory}/golden.json`,JSON.stringify(articles.map(article=>({article,expected:classifyAnime(article)}))));console.log(`Prepared ${articles.length} JS/native parity cases and Maven test dependencies.`);
