package com.fallowthis.app;

import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.*;
import androidx.core.content.ContextCompat;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.concurrent.locks.ReentrantLock;

public final class PhoneEngine {
    private static final ReentrantLock LOCK=new ReentrantLock();
    public static boolean allowed(Context c){return (Build.VERSION.SDK_INT<33||ContextCompat.checkSelfPermission(c,android.Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)&&NotificationManagerCompat.from(c).areNotificationsEnabled();}
    public static void channel(Context c){if(Build.VERSION.SDK_INT>=26){NotificationChannel channel=new NotificationChannel("developments","Önemli gelişmeler",NotificationManager.IMPORTANCE_DEFAULT);channel.setDescription("Telefonda bulunan resmî duyurular");c.getSystemService(NotificationManager.class).createNotificationChannel(channel);}}
    public static void notify(Context c,String id,String title,String body){channel(c);Intent intent=new Intent(c,MainActivity.class).putExtra("eventId",id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);PendingIntent pi=PendingIntent.getActivity(c,id.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);NotificationCompat.Builder n=new NotificationCompat.Builder(c,"developments").setSmallIcon(R.drawable.ic_notification).setContentTitle(title).setContentText(body).setStyle(new NotificationCompat.BigTextStyle().bigText(body)).setContentIntent(pi).setAutoCancel(true);NotificationManagerCompat.from(c).notify(id,0,n.build());}
    private static JSONObject fetch(String url,String etag)throws Exception{HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();connection.setConnectTimeout(15000);connection.setReadTimeout(15000);connection.setInstanceFollowRedirects(false);connection.setRequestProperty("User-Agent","FallowThis/0.2 (phone official-announcement-monitor)");if(etag!=null&&!etag.isEmpty())connection.setRequestProperty("If-None-Match",etag);
        try{int code=connection.getResponseCode();if(code==304)return new JSONObject().put("notModified",true);if(code!=200)throw new IOException("HTTP "+code);try(InputStream in=connection.getInputStream()){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1){if(out.size()+n>3000000)throw new IOException("Kaynak 3 MB sınırını aştı");out.write(b,0,n);}return new JSONObject().put("text",out.toString("UTF-8")).put("etag",connection.getHeaderField("ETag"));}}finally{connection.disconnect();}}
    public static void run(Context context)throws Exception{
        if(!LOCK.tryLock())return;
        try(PhoneStore store=new PhoneStore(context)){long now=System.currentTimeMillis();JSONArray sources=store.catalog.getJSONArray("sources");for(int s=0;s<sources.length();s++){
            JSONObject source=sources.getJSONObject(s),previous=store.status(source.getString("id"));int count=0,added=0;String warning=null;
            try{JSONObject response=fetch(source.getString("url"),previous.optString("etag",null));if(response.optBoolean("notModified")){previous.put("checked_at",PhoneCore.iso(now)).put("success_at",PhoneCore.iso(now)).put("error",JSONObject.NULL);store.status(source.getString("id"),previous);continue;}
                JSONArray articles=PhoneCore.parseSource(source,response.getString("text"));if(!previous.optBoolean("initialized"))for(int p=2;p<=source.optInt("pages",1);p++)try{JSONArray more=PhoneCore.parseSource(source,fetch(source.getString("url")+"?p="+p,null).getString("text"));for(int i=0;i<more.length();i++)articles.put(more.get(i));}catch(Exception e){warning="Arşiv: "+e.getMessage();}
                for(int i=0;i<articles.length();i++){JSONObject article=articles.getJSONObject(i);count++;if(store.seen(article.getString("url")))continue;if(article.optString("body").isEmpty()&&PhoneCore.classify(article,store.catalog).length()>0)try{PhoneCore.detail(source,article,fetch(article.getString("url"),null).getString("text"));}catch(Exception e){warning="Detay: "+e.getMessage();}added+=store.ingest(source,article,!previous.optBoolean("initialized"),now);}
                store.status(source.getString("id"),new JSONObject().put("checked_at",PhoneCore.iso(now)).put("success_at",PhoneCore.iso(now)).put("error",warning==null?JSONObject.NULL:warning).put("fetched",count).put("accepted",added).put("initialized",true).put("etag",response.opt("etag")));
            }catch(Exception e){previous.put("checked_at",PhoneCore.iso(now)).put("error",e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());store.status(source.getString("id"),previous);}
        }store.setMeta("lastCheck",PhoneCore.iso(now));deliver(context,store,now);
        }finally{LOCK.unlock();}
    }
    public static void deliver(Context context,PhoneStore store,long now)throws Exception{JSONObject settings=store.settings();if(!settings.optBoolean("notifications"))return;JSONArray pending=store.snapshot().getJSONArray("pending");for(int i=0;i<pending.length();i++){String id=pending.getString(i);JSONObject event=store.event(id);String state=null;if(event==null||now-PhoneCore.epoch(event.optString("published_at"))>48*3600000L||!PhoneCore.matches(settings,event,store.catalog)||!PhoneCore.contains(settings.getJSONArray("types"),event.getString("type")))state="cancelled";else if(!PhoneCore.quiet(settings,now)&&allowed(context)){try{notify(context,id,event.getString("title"),event.getString("summary"));state="sent";}catch(SecurityException ignored){}}
            if(state!=null){ContentValues v=new ContentValues();v.put("state",state);store.getWritableDatabase().update("pending",v,"id=?",new String[]{id});}
        }}
}
