package com.fallowthis.app;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.*;
import java.util.concurrent.TimeUnit;
import org.json.JSONObject;

public final class MonitorWorker extends Worker {
    public MonitorWorker(@NonNull Context context,@NonNull WorkerParameters parameters){super(context,parameters);}
    @NonNull @Override public Result doWork(){try{try(PhoneStore store=new PhoneStore(getApplicationContext())){if(!store.settings().optBoolean("backgroundEnabled"))return Result.success();}PhoneEngine.run(getApplicationContext());return Result.success();}catch(Exception e){return getRunAttemptCount()<3?Result.retry():Result.failure();}}
    public static void schedule(Context context,JSONObject settings){WorkManager wm=WorkManager.getInstance(context);if(!settings.optBoolean("backgroundEnabled")){wm.cancelUniqueWork("fallow-monitor");return;}int minutes=Math.max(15,settings.optInt("pollMinutes",240));Constraints constraints=new Constraints.Builder().setRequiredNetworkType(settings.optBoolean("wifiOnly")?NetworkType.UNMETERED:NetworkType.CONNECTED).setRequiresBatteryNotLow(true).build();PeriodicWorkRequest work=new PeriodicWorkRequest.Builder(MonitorWorker.class,minutes,TimeUnit.MINUTES).setConstraints(constraints).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,1,TimeUnit.MINUTES).build();wm.enqueueUniquePeriodicWork("fallow-monitor",ExistingPeriodicWorkPolicy.UPDATE,work);}
}
