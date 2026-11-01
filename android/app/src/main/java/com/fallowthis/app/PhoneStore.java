package com.fallowthis.app;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import org.json.*;
import java.io.*;
import java.util.*;

public final class PhoneStore extends SQLiteOpenHelper {
    public final JSONObject catalog;
    public PhoneStore(Context context)throws Exception{super(context,"fallow-phone.sqlite",null,1);try(InputStream in=context.getAssets().open("fallow-catalog.json")){ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;while((n=in.read(b))!=-1)out.write(b,0,n);catalog=new JSONObject(out.toString("UTF-8"));}setWriteAheadLoggingEnabled(true);}
    @Override public void onCreate(SQLiteDatabase db){db.execSQL("CREATE TABLE meta(k TEXT PRIMARY KEY,v TEXT NOT NULL)");db.execSQL("CREATE TABLE articles(url TEXT PRIMARY KEY)");db.execSQL("CREATE TABLE events(id TEXT PRIMARY KEY,identity TEXT UNIQUE NOT NULL,json TEXT NOT NULL)");db.execSQL("CREATE TABLE status(id TEXT PRIMARY KEY,json TEXT NOT NULL)");db.execSQL("CREATE TABLE pending(id TEXT PRIMARY KEY,state TEXT NOT NULL DEFAULT 'pending')");}
    @Override public void onUpgrade(SQLiteDatabase db,int old,int next){throw new IllegalStateException("Desteklenmeyen veritabanı sürümü");}
    public String getMeta(String key,String fallback){try(Cursor c=getReadableDatabase().rawQuery("SELECT v FROM meta WHERE k=?",new String[]{key})){return c.moveToFirst()?c.getString(0):fallback;}}
    public void setMeta(String key,String value){ContentValues v=new ContentValues();v.put("k",key);v.put("v",value);getWritableDatabase().insertWithOnConflict("meta",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public JSONObject settings()throws JSONException{JSONObject s=new JSONObject(getMeta("settings",catalog.getJSONObject("defaults").toString()));if(s.optInt("phoneScheduleVersion")!=2){s.put("pollMinutes",240).put("phoneScheduleVersion",2);setMeta("settings",s.toString());}return s;}
    public JSONObject status(String id)throws JSONException{try(Cursor c=getReadableDatabase().rawQuery("SELECT json FROM status WHERE id=?",new String[]{id})){return c.moveToFirst()?new JSONObject(c.getString(0)):new JSONObject();}}
    public void status(String id,JSONObject value){ContentValues v=new ContentValues();v.put("id",id);v.put("json",value.toString());getWritableDatabase().insertWithOnConflict("status",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public boolean seen(String url){try(Cursor c=getReadableDatabase().rawQuery("SELECT 1 FROM articles WHERE url=?",new String[]{url})){return c.moveToFirst();}}
    public void event(JSONObject event)throws JSONException{ContentValues v=new ContentValues();v.put("id",event.getString("id"));v.put("identity",event.getString("identity"));v.put("json",event.toString());getWritableDatabase().insertWithOnConflict("events",null,v,SQLiteDatabase.CONFLICT_REPLACE);}
    public JSONObject event(String id)throws JSONException{try(Cursor c=getReadableDatabase().rawQuery("SELECT json FROM events WHERE id=?",new String[]{id})){return c.moveToFirst()?new JSONObject(c.getString(0)):null;}}
    public void markRead(String id,boolean read)throws Exception{SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{JSONObject e=event(id);if(e==null)throw new Exception("Haber bulunamadı");e.put("read_at",read?PhoneCore.iso(System.currentTimeMillis()):JSONObject.NULL);event(e);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public int ingest(JSONObject source,JSONObject article,boolean baseline,long now)throws Exception{
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();int added=0;try{String url=article.getString("url");if(seen(url)){db.setTransactionSuccessful();return 0;}ContentValues a=new ContentValues();a.put("url",url);db.insertOrThrow("articles",null,a);JSONArray candidates=PhoneCore.classify(article,catalog);
            for(int i=0;i<candidates.length();i++){JSONObject candidate=candidates.getJSONObject(i),existing=event(candidate.getString("id"));boolean fresh=existing==null;if(fresh){existing=candidate;existing.put("published_at",article.getString("publishedAt")).put("original_title",article.getString("title")).put("read_at",JSONObject.NULL).put("sources",new JSONArray());added++;}
                JSONArray links=existing.getJSONArray("sources");boolean found=false;for(int j=0;j<links.length();j++)if(links.getJSONObject(j).getString("url").equals(url))found=true;if(!found)links.put(new JSONObject().put("source_id",source.getString("id")).put("url",url).put("published_at",article.getString("publishedAt")));
                if(PhoneCore.epoch(article.getString("publishedAt"))<PhoneCore.epoch(existing.getString("published_at")))existing.put("published_at",article.getString("publishedAt"));event(existing);
                JSONObject settings=settings();long age=now-PhoneCore.epoch(article.getString("publishedAt"));if(fresh&&!baseline&&age>=0&&age<=48*3600000L&&settings.optBoolean("notifications")&&PhoneCore.contains(settings.getJSONArray("types"),candidate.getString("type"))&&PhoneCore.matches(settings,candidate,catalog)){ContentValues p=new ContentValues();p.put("id",candidate.getString("id"));p.put("state","pending");db.insertWithOnConflict("pending",null,p,SQLiteDatabase.CONFLICT_IGNORE);}
            }db.setTransactionSuccessful();return added;
        }finally{db.endTransaction();}
    }
    public JSONObject snapshot()throws Exception{JSONObject d=new JSONObject().put("format","fallow-local").put("version",1).put("settings",settings()).put("lastCheck",getMeta("lastCheck",null));JSONArray events=new JSONArray(),articles=new JSONArray(),pending=new JSONArray();JSONObject status=new JSONObject();
        try(Cursor c=getReadableDatabase().rawQuery("SELECT json FROM events",null)){while(c.moveToNext())events.put(new JSONObject(c.getString(0)));}try(Cursor c=getReadableDatabase().rawQuery("SELECT url FROM articles",null)){while(c.moveToNext())articles.put(c.getString(0));}try(Cursor c=getReadableDatabase().rawQuery("SELECT id,json FROM status",null)){while(c.moveToNext())status.put(c.getString(0),new JSONObject(c.getString(1)));}try(Cursor c=getReadableDatabase().rawQuery("SELECT id FROM pending WHERE state='pending'",null)){while(c.moveToNext())pending.put(c.getString(0));}return d.put("events",events).put("articles",articles).put("status",status).put("pending",pending);}
    public void replaceFromBackup(JSONObject data)throws Exception{SQLiteDatabase db=getWritableDatabase();db.beginTransaction();try{db.delete("events",null,null);db.delete("articles",null,null);db.delete("pending",null,null);db.delete("status",null,null);setMeta("settings",data.getJSONObject("settings").toString());JSONArray events=data.getJSONArray("events"),articles=data.getJSONArray("articles");for(int i=0;i<events.length();i++)event(events.getJSONObject(i));for(int i=0;i<articles.length();i++){ContentValues v=new ContentValues();v.put("url",articles.getString(i));db.insertWithOnConflict("articles",null,v,SQLiteDatabase.CONFLICT_IGNORE);}getWritableDatabase().delete("meta","k='lastCheck'",null);db.setTransactionSuccessful();}finally{db.endTransaction();}}
}
