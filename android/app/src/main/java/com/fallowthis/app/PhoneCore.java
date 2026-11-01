package com.fallowthis.app;

import org.json.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.parser.Parser;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.*;
import java.util.*;
import java.util.regex.*;

/** Platform-independent rules; JVM tests use the exact class used by WorkManager. */
public final class PhoneCore {
    private static Matcher find(String pattern,String text){return Pattern.compile(pattern,Pattern.CASE_INSENSITIVE|Pattern.UNICODE_CASE).matcher(text);}
    public static String hash(String value){try{byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();for(byte v:bytes)b.append(String.format(Locale.ROOT,"%02x",v&255));return b.toString();}catch(Exception e){throw new IllegalStateException(e);}}
    public static String normalized(String s){return Normalizer.normalize(s,Normalizer.Form.NFKC).toLowerCase(Locale.ROOT).replaceAll("【[^】]*】|[!！\\s「」『』・:：.,。]","");}
    public static String iso(long ms){SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.ROOT);f.setTimeZone(TimeZone.getTimeZone("UTC"));return f.format(new Date(ms));}
    public static long epoch(String text){for(String pattern:new String[]{"yyyy-MM-dd'T'HH:mm:ss.SSSX","yyyy-MM-dd'T'HH:mm:ssX","EEE, dd MMM yyyy HH:mm:ss Z"})try{SimpleDateFormat f=new SimpleDateFormat(pattern,Locale.US);f.setLenient(false);return Objects.requireNonNull(f.parse(text)).getTime();}catch(Exception ignored){}return 0;}
    public static String published(String text){Matcher m=find("(20\\d{2})[.\\-/年](\\d{1,2})[.\\-/月](\\d{1,2})",text);if(m.find())try{SimpleDateFormat f=new SimpleDateFormat("yyyy-M-d",Locale.ROOT);f.setLenient(false);f.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));return iso(Objects.requireNonNull(f.parse(m.group(1)+"-"+m.group(2)+"-"+m.group(3))).getTime());}catch(Exception ignored){}long n=epoch(text);return n>0?iso(n):null;}
    public static String url(String href,String base){URI u=URI.create(base).resolve(href);if(!"https".equals(u.getScheme())||u.getUserInfo()!=null)throw new IllegalArgumentException("Geçersiz kaynak adresi");String s=u.toString().split("#",2)[0];return s;}
    public static boolean contains(JSONArray list,String value){for(int i=0;i<list.length();i++)if(value.equals(list.optString(i)))return true;return false;}
    public static JSONObject topic(JSONObject catalog,String id){JSONArray all=catalog.optJSONArray("topics");for(int i=0;i<all.length();i++){JSONObject t=all.optJSONObject(i);if(t.optString("id").equals(id))return t;}return null;}
    public static boolean matches(JSONObject settings,JSONObject event,JSONObject catalog){if(contains(settings.optJSONArray("favorites"),event.optString("topic_id")))return true;JSONObject t=topic(catalog,event.optString("topic_id"));return contains(settings.optJSONArray("categories"),event.optString("category_id"))&&("all".equals(settings.optString("scope"))||t!=null&&"established".equals(t.optString("tier")));}
    public static boolean quiet(JSONObject settings,long now){if(!settings.optBoolean("quietEnabled"))return false;String start=settings.optString("quietStart"),end=settings.optString("quietEnd");if(start.equals(end))return false;SimpleDateFormat f=new SimpleDateFormat("HH:mm",Locale.ROOT);f.setTimeZone(TimeZone.getTimeZone(settings.optString("timezone","Europe/Istanbul")));String time=f.format(new Date(now));return start.compareTo(end)<0?time.compareTo(start)>=0&&time.compareTo(end)<0:time.compareTo(start)>=0||time.compareTo(end)<0;}
    private static JSONObject release(String text)throws JSONException{
        Matcher m=find("(20\\d{2})年\\s*(?:(\\d{1,2})月\\s*)?(?:(\\d{1,2})日\\s*)?(?:[（(][^）)]{0,12}[）)]\\s*)?(?:より|から|に|全国|劇場|TVアニメ|テレビアニメ|TV|日本|全世界|配信|放送|公開|ロードショー|スタート|予定|決定|開始|[\\s!！、])*?(?:放送|配信|公開|ロードショー|スタート)",text);
        if(m.find()){String year=m.group(1),month=m.group(2),day=m.group(3);if(month!=null&&(Integer.parseInt(month)<1||Integer.parseInt(month)>12)||day!=null&&(Integer.parseInt(day)<1||Integer.parseInt(day)>31))return null;String value=year+(month==null?"":String.format(Locale.ROOT,"-%02d",Integer.parseInt(month)))+(day==null?"":String.format(Locale.ROOT,"-%02d",Integer.parseInt(day)));return new JSONObject().put("value",value).put("precision",day!=null?"day":month!=null?"month":"year");}
        m=find("(?:premier(?:e|es|ing)|air(?:s|ing)?|broadcast(?:s|ing)?|releas(?:e|es|ing)|available|launch(?:es|ing)?)(?:\\s+(?:on|in|from|for|is|set|to|will|be))*\\s+((?:January|February|March|April|May|June|July|August|September|October|November|December)\\s+\\d{1,2}(?:st|nd|rd|th)?(?:,?\\s+20\\d{2})?|20\\d{2})",text);
        if(m.find()){String v=m.group(1);if(v.matches("20\\d{2}"))return new JSONObject().put("value",v).put("precision","year");if(v.matches(".*20\\d{2}.*"))for(String p:new String[]{"MMMM d, yyyy","MMMM d yyyy"})try{SimpleDateFormat f=new SimpleDateFormat(p,Locale.US);f.setTimeZone(TimeZone.getTimeZone("UTC"));f.setLenient(false);Date d=f.parse(v.replaceAll("(\\d)(st|nd|rd|th)","$1"));return new JSONObject().put("value",iso(d.getTime()).substring(0,10)).put("precision","day");}catch(Exception ignored){}}
        return null;
    }
    public static JSONArray classify(JSONObject article,JSONObject catalog)throws JSONException{
        String title=Jsoup.parse(article.optString("title")).text(),body=Jsoup.parse(article.optString("body")).text(),text=title+" "+body;JSONObject t=topic(catalog,article.optString("topicId"));
        if(t==null){JSONArray all=catalog.getJSONArray("topics");for(int i=0;i<all.length()&&t==null;i++){JSONObject candidate=all.getJSONObject(i);JSONArray aliases=candidate.getJSONArray("aliases");for(int j=0;j<aliases.length();j++)if(text.toLowerCase(Locale.ROOT).contains(aliases.getString(j).toLowerCase(Locale.ROOT))){t=candidate;break;}}}
        JSONArray result=new JSONArray();if(t==null)return result;
        if(find("Blu.?ray|DVD|グッズ|チケット|イベント|ライブ|コラボ|キャンペーン|一挙|再放送|振り返|完結記念|ゲーム|ノンクレジット|第[0-9０-９一二三四五六七八九十]+話.*(?:予告|先行|PV)|主題歌|CD|アルバム|舞台|ラジオ|発売|イラスト|連動企画|Sparking|DLC|licensing|collection|award|game|concert|livestream|rumou?r|leak|unconfirmed|噂|リーク",title).find())return result;
        Matcher season=find("(?:第\\s*)?([0-9０-９]+)\\s*期|(?:season|シーズン)\\s*([0-9０-９]+)",text);String seasonId=null;if(season.find())seasonId="season-"+Normalizer.normalize(season.group(1)!=null?season.group(1):season.group(2),Normalizer.Form.NFKC);
        boolean film=find("映画|劇場版|movie|film",title).find();String project=seasonId!=null?seasonId:film?"film-"+hash(normalized(title).replaceAll("20\\d{2}年.*|公開.*|制作.*|製作.*","")).substring(0,12):"series";
        if(find("続編.*(?:制作|製作).*決定|新(?:作|アニメーション|アニメ).*?(?:制作|製作|プロジェクト).*?(?:決定|始動)|(?:制作|製作)決定|(?:season\\s*\\d+|sequel|new\\s+(?:anime|animation)).*?(?:confirm|announc|green.?lit)",title).find())result.put(event(t,"project",seasonId!=null?seasonId:hash(normalized(title)).substring(0,24),seasonId,film,null));
        JSONObject release=release(title);if(release==null&&find("放送|配信|公開|premiere|broadcast|release date",title).find())release=release(body);
        if(release!=null)result.put(event(t,"release",project+":"+release.getString("value"),seasonId,film,release));
        if(find("(?:PV|ＰＶ|ティザー|トレーラー|特報|予告|trailer|teaser|プロモーションビデオ).*?(?:公開|解禁|発表|released|reveal)|(?:released|new|official).*?(?:trailer|teaser)|新.*PV",title).find()){
            String signature=null;JSONArray videos=article.optJSONArray("videoUrls");if(videos!=null)for(int i=0;i<videos.length();i++){Matcher v=find("(?:youtu\\.be/|embed/|[?&]v=)([\\w-]{11})",videos.optString(i));if(v.find()){signature="youtube-"+v.group(1);break;}}
            Matcher ordinal=find("第\\s*(\\d+)\\s*弾",title);if(signature==null)signature=seasonId!=null&&ordinal.find()?seasonId+":pv-"+ordinal.group(1):hash(normalized(title)).substring(0,24);result.put(event(t,"trailer",signature,seasonId,film,null));
        }return result;
    }
    private static JSONObject event(JSONObject topic,String type,String signature,String season,boolean film,JSONObject release)throws JSONException{
        String name=topic.getString("name"),what=film?"yeni bir anime filmi":season!=null?Integer.parseInt(season.substring(7))+". sezon":"yeni bir animasyon projesi";
        String title=type.equals("project")?name+": "+(film?"yeni film":season!=null?Integer.parseInt(season.substring(7))+". sezon":"yeni animasyon projesi")+" onaylandı":type.equals("release")?name+": yayın takvimi açıklandı":name+": resmî fragman yayımlandı";
        String dateText="";if(release!=null){String value=release.getString("value"),precision=release.getString("precision");if(precision.equals("year"))dateText=value+" yılı";else try{SimpleDateFormat input=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT),output=new SimpleDateFormat(precision.equals("month")?"MMMM yyyy":"d MMMM yyyy",new Locale("tr","TR"));dateText=output.format(input.parse(value+(precision.equals("month")?"-01":"")));}catch(Exception ignored){dateText=value;}}
        String summary=type.equals("project")?name+" için "+what+" resmî kaynakta duyuruldu. Hikâye ayrıntıları özete alınmadı.":type.equals("release")?"Resmî duyuruda "+name+" için "+dateText+" yayın aralığı paylaşıldı. Bölge ve platform bilgisi için kaynağı kontrol et.":name+" için resmî tanıtım videosu paylaşıldı. Spoiler içermemesi için video içeriği özetlenmedi.";
        String identity=topic.getString("id")+":"+type+":"+signature;
        return new JSONObject().put("id",hash(identity).substring(0,32)).put("identity",identity).put("topic_id",topic.getString("id")).put("category_id","anime").put("type",type).put("title",title).put("summary",summary).put("release_date",release==null?JSONObject.NULL:release.getString("value")).put("release_precision",release==null?JSONObject.NULL:release.getString("precision"));
    }
    public static JSONArray parseSource(JSONObject source,String text)throws JSONException{
        JSONArray result=new JSONArray();String base=source.getString("url"),adapter=source.getString("adapter");Document doc=Jsoup.parse(text,base,adapter.equals("rss")?Parser.xmlParser():Parser.htmlParser());
        if(adapter.equals("rss")){for(Element e:doc.select("item")){String pub=published(e.select("pubDate").text());if(pub==null)continue;String body=e.select("content|encoded").text();if(body.isEmpty())body=e.select("description").text();JSONObject a=new JSONObject().put("title",e.select("title").text()).put("body",body).put("url",url(e.select("link").text(),base)).put("publishedAt",pub);a.put("videoUrls",videoUrls(Jsoup.parse(body)));result.put(a);}}
        else if(adapter.equals("rezero")){for(Element e:doc.select(".content-entry[id]")){String pub=published(e.select(".entry-date").text());if(pub!=null)result.put(new JSONObject().put("title",e.select(".entry-title").text()).put("body",e.select(".entry-body").text()).put("url",base+"#"+e.id()).put("publishedAt",pub).put("topicId",source.getString("topicId")).put("videoUrls",videoUrls(e)));}}
        else {String row=adapter.equals("aniplex")?".p-news__list-item":adapter.equals("bocchi")?".newslist__item":".news_list__item";String title=adapter.equals("aniplex")?".p-news_data__title":adapter.equals("bocchi")?".newslist__title":".news_list__item-title";String date=adapter.equals("aniplex")?".p-news_data__date":".newslist__date";
            if(source.has("selectors")){JSONArray selectors=source.getJSONArray("selectors");row=selectors.getString(0);title=selectors.getString(1);date=selectors.getString(2);}for(Element e:doc.select(row)){Element a=e.selectFirst("a[href]");String pub=published(adapter.equals("kimetsu")?e.select(".-year").text()+"."+e.select(".-day").text():e.select(date).first()!=null?e.select(date).first().text():"");String head=e.select(title).text();if(a==null||pub==null||head.isEmpty())continue;String link=url(a.attr("href"),base);if(!URI.create(link).getHost().equals(URI.create(base).getHost()))continue;result.put(new JSONObject().put("title",head).put("url",link).put("publishedAt",pub).put("body","").put("topicId",source.getString("topicId")).put("videoUrls",new JSONArray()));}}
        if(result.length()==0)throw new IllegalArgumentException("Haber seçicisi eşleşmedi; kaynak değişmiş olabilir.");LinkedHashMap<String,JSONObject> unique=new LinkedHashMap<>();for(int i=0;i<result.length();i++){JSONObject a=result.getJSONObject(i);if(!unique.containsKey(a.getString("url")))unique.put(a.getString("url"),a);}return new JSONArray(unique.values());
    }
    public static JSONArray videoUrls(Element element){JSONArray list=new JSONArray();for(Element e:element.select("iframe[src],a[href]"))list.put(e.hasAttr("src")?e.attr("src"):e.attr("href"));return list;}
    public static void detail(JSONObject source,JSONObject article,String html)throws JSONException{Element e=Jsoup.parse(html).selectFirst(source.getString("detail"));if(e!=null){article.put("body",e.text().substring(0,Math.min(e.text().length(),15000)));article.put("videoUrls",videoUrls(e));}}
}
