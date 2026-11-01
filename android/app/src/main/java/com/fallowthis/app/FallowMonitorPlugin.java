package com.fallowthis.app;
import android.Manifest;
import android.content.*;
import android.os.Build;
import androidx.core.content.FileProvider;
import com.getcapacitor.*;
import com.getcapacitor.annotation.*;
import org.json.*;
import java.io.*;
import java.util.concurrent.*;

@CapacitorPlugin(name="FallowMonitor",permissions={@Permission(alias="notifications",strings={Manifest.permission.POST_NOTIFICATIONS})})
public class FallowMonitorPlugin extends Plugin {
    private final ExecutorService executor=Executors.newSingleThreadExecutor();
    @Override public void load(){executor.execute(()->{try(PhoneStore store=new PhoneStore(getContext())){MonitorWorker.schedule(getContext(),store.settings());}catch(Exception e){android.util.Log.e("Fallow","Local initialization failed",e);}});}
    private void snapshotResult(PluginCall call){try(PhoneStore store=new PhoneStore(getContext())){call.resolve(new JSObject().put("data",store.snapshot()));}catch(Exception e){call.reject("Yerel kayıt okunamadı: "+e.getMessage(),e);}}
    @PluginMethod public void snapshot(PluginCall call){executor.execute(()->snapshotResult(call));}
    @PluginMethod public void refresh(PluginCall call){executor.execute(()->{try{PhoneEngine.run(getContext());snapshotResult(call);}catch(Exception e){call.reject("Kontrol tamamlanamadı: "+e.getMessage(),e);}});}
    @PluginMethod public void saveSettings(PluginCall call){executor.execute(()->{try(PhoneStore store=new PhoneStore(getContext())){JSONObject settings=call.getObject("settings");if(settings==null)throw new Exception("Ayarlar eksik");int interval=settings.optInt("pollMinutes");if(interval!=15&&interval!=30&&interval!=60&&interval!=180&&interval!=240)throw new Exception("Geçersiz aralık");store.setMeta("settings",settings.toString());MonitorWorker.schedule(getContext(),settings);PhoneEngine.deliver(getContext(),store,System.currentTimeMillis());snapshotResult(call);}catch(Exception e){call.reject("Ayar kaydedilemedi: "+e.getMessage(),e);}});}
    @PluginMethod public void markRead(PluginCall call){executor.execute(()->{try(PhoneStore store=new PhoneStore(getContext())){store.markRead(call.getString("id",""),call.getBoolean("read",true));snapshotResult(call);}catch(Exception e){call.reject("Okundu bilgisi kaydedilemedi",e);}});}
    @PluginMethod public void notificationPermission(PluginCall call){call.resolve(new JSObject().put("granted",PhoneEngine.allowed(getContext())));}
    @PluginMethod public void requestNotificationPermission(PluginCall call){if(Build.VERSION.SDK_INT>=33&&getPermissionState("notifications")!=PermissionState.GRANTED)requestPermissionForAlias("notifications",call,"notificationPermissionResult");else notificationPermission(call);}
    @PermissionCallback private void notificationPermissionResult(PluginCall call){notificationPermission(call);}
    @PluginMethod public void testNotification(PluginCall call){if(!PhoneEngine.allowed(getContext())){call.reject("Önce Android bildirim izni ver.");return;}PhoneEngine.notify(getContext(),"test","YoriNovi · Yerel test","Bu bildirimi cihazında görüyorsan gösterimi doğrulayabilirsin.");call.resolve(new JSObject().put("posted",true));}
    @PluginMethod public void exportBackup(PluginCall call){executor.execute(()->{try(PhoneStore store=new PhoneStore(getContext())){File directory=new File(getContext().getCacheDir(),"backups");if(!directory.exists()&&!directory.mkdirs())throw new IOException("Yedek dizini açılamadı");File file=new File(directory,"yorinovi-backup.json");try(Writer w=new OutputStreamWriter(new FileOutputStream(file),"UTF-8")){w.write(store.snapshot().toString());}Intent intent=new Intent(Intent.ACTION_SEND).setType("application/json").putExtra(Intent.EXTRA_STREAM,FileProvider.getUriForFile(getContext(),getContext().getPackageName()+".fileprovider",file)).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);getActivity().runOnUiThread(()->{try{getActivity().startActivity(Intent.createChooser(intent,"YoriNovi yedeğini kaydet veya paylaş"));call.resolve();}catch(Exception e){call.reject("Yedek paylaşımı açılamadı",e);}});}catch(Exception e){call.reject("Yedek oluşturulamadı",e);}});}
    @PluginMethod public void importBackup(PluginCall call){executor.execute(()->{try(PhoneStore store=new PhoneStore(getContext())){JSONObject data=call.getObject("data");if(data==null||!data.optString("format").equals("fallow-local")||data.optInt("version")!=1)throw new Exception("Geçersiz yedek");store.replaceFromBackup(data);MonitorWorker.schedule(getContext(),store.settings());snapshotResult(call);}catch(Exception e){call.reject("Yedek içe alınamadı: "+e.getMessage(),e);}});}
    @Override protected void handleOnNewIntent(Intent intent){super.handleOnNewIntent(intent);String id=intent.getStringExtra("eventId");if(id!=null)notifyListeners("notificationOpened",new JSObject().put("eventId",id),true);}
    @PluginMethod public void pendingNotification(PluginCall call){String id=getActivity().getIntent().getStringExtra("eventId");getActivity().getIntent().removeExtra("eventId");call.resolve(new JSObject().put("eventId",id));}
    @Override protected void handleOnDestroy(){executor.shutdown();super.handleOnDestroy();}
}
