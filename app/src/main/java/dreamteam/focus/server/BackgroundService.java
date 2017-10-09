package dreamteam.focus.server;

import android.app.ActivityManager;
import android.app.NotificationManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.IBinder;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import dreamteam.focus.R;

/*
    src: https://stackoverflow.com/questions/41425986/call-a-notification-listener-inside-a-background-service-in-android-studio
 */

public class BackgroundService extends NotificationListenerService {
    private static final String TAG = "BackgroundService";

    private Runnable scheduleThread = null;
    private Runnable blockingThread = null;
    private final int SCHEDULE_TIMEOUT = 60;
    private final int BLOCKING_TIMEOUT = 1;
    private DatabaseConnector DBConnector;

    public static final String ACTION_STATUS_BROADCAST = "com.example.notifyservice.NotificationService_Status";
    private NLServiceReceiver nlservicereciver;

    private int nAdded=0;               //The number of notifications added (since the service started)

    private int nRemoved=0;             //The number of notifications removed (since the service started)

    //global variable for blockApps so that overlap works

    public BackgroundService() {
        DBConnector = new DatabaseConnector();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    /**
     * src= https://stackoverflow.com/questions/28292682/using-an-sqlite-database-from-a-service-in-android
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final String TAG = "onStartCommand";
        final Handler mHandler = new Handler();
        if (scheduleThread == null) {
            Log.d(TAG, "scheduleThread created");
            scheduleThread = new Runnable() {
                @Override
                public void run() {
                    // TODO: round to next minute-2sec
                    mHandler.postDelayed(scheduleThread, SCHEDULE_TIMEOUT * 1000);
                    Log.d(TAG, "scheduleThread");
                }
            };
            mHandler.postDelayed(scheduleThread, SCHEDULE_TIMEOUT * 1000);
        }

        if (blockingThread == null) {
            blockingThread = new Runnable() {
                @Override
                public void run() {
                    mHandler.postDelayed(blockingThread, BLOCKING_TIMEOUT * 1000);
                    Log.d(TAG, "blockingThread");
                    printForegroundTask();
                }
            };

            mHandler.postDelayed(blockingThread, BLOCKING_TIMEOUT * 1000);
        }
        if(intent!= null) {
            if (intent.hasExtra("command")) {
                Log.i("NotificationService", "Started for command '" + intent.getStringExtra("command"));
                broadcastStatus();
            } else if (intent.hasExtra("id")) {
                int id = intent.getIntExtra("id", 0);
                String message = intent.getStringExtra("msg");
                Log.i("NotificationService", "Requested to start explicitly - id : " + id + " message : " + message);
            }
        }
         super.onStartCommand(intent, flags, startId);

        // NOTE: We return STICKY to prevent the automatic service termination
        return START_STICKY;
    }


    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        final String TAG = "Notification service";

        Log.i(TAG,"**********  onNotificationPosted");
        Log.i(TAG,"ID :" + sbn.getId() + "t" + sbn.getNotification().tickerText + "\t" + sbn.getPackageName());


        String packageName = getNameFromSBN(sbn);
        //Intent i = new  Intent("notification received");
        //i.putExtra("notification_event","onNotificationPosted :" + sbn.getPackageName() + "\n");
        //i.putExtra("notification_event", packageName);
        //sendBroadcast(i);

        if(packageName.equals("com.whatsapp"))
            dismissNotification(sbn);
        nAdded++;

        broadcastStatus();
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {

        final String TAG = "Notification service";
        Log.i(TAG,"********** onNOtificationRemoved");
        Log.i(TAG,"ID :" + sbn.getId() + "t" + sbn.getNotification().tickerText +"\t" + sbn.getPackageName());
//        Intent i = new  Intent("com.example.notify.NOTIFICATION_LISTENER_EXAMPLE");
//        i.putExtra("notification_event","onNotificationRemoved :" + sbn.getPackageName() + "\n");
//        sendBroadcast(i);

        nRemoved++;
        broadcastStatus();
    }

    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        Log.w("NotificationService", "Notification listener DISCONNECTED from the notification service! Scheduling a reconnect...");
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        Log.w("NotificationService", "Notification listener connected with the notification service!");
    }

    private String getNameFromSBN(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        Log.d(TAG, packageName);
        return packageName;
    }

    /**
     * to dismiss notifications
     */

    public void dismissNotification(StatusBarNotification sbn) {
        Log.d(TAG, "dismiss " + sbn.getPackageName() +"notification");
        if(getAppNameFromPackage(sbn.getPackageName()).equals("Whatsapp"))
            cancelNotification(sbn.getKey());
    }

    private void broadcastStatus() {
        Log.i("NotificationService", "Broadcasting status added("+nAdded+")/removed("+nRemoved+")");
        Intent i1 = new  Intent(ACTION_STATUS_BROADCAST);
        i1.putExtra("serviceMessage","Added: "+nAdded+" | Removed: "+nRemoved);
        LocalBroadcastManager.getInstance(this).sendBroadcast(i1);

    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i("NotificationService", "NotificationService created!");
        nlservicereciver = new NLServiceReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.example.notifyservice.NOTIFICATION_LISTENER_SERVICE_EXAMPLE");
        registerReceiver(nlservicereciver,filter);
        Log.i("NotificationService", "NotificationService created!");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(nlservicereciver);
        Log.i("NotificationService", "NotificationService destroyed!");
    }

    /**
     * Notification Broadcast Receiver
     * */

    class NLServiceReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getStringExtra("command").equals("list")){
                Intent i1 = new  Intent("com.example.notify.NOTIFICATION_LISTENER_EXAMPLE");
                i1.putExtra("notification_event","=====================");
                sendBroadcast(i1);
                int i=1;
                for (StatusBarNotification sbn : BackgroundService.this.getActiveNotifications()) {
                    Intent i2 = new  Intent("com.example.notify.NOTIFICATION_LISTENER_EXAMPLE");
                    i2.putExtra("notification_event",i +" " + sbn.getPackageName() + "\n");
                    sendBroadcast(i2);
                    i++;
                }
                Intent i3 = new  Intent("com.example.notify.NOTIFICATION_LISTENER_EXAMPLE");
                i3.putExtra("notification_event","===== Notification List ====");
                sendBroadcast(i3);

            }

        }
    }


    public void tick() {
        // TODO
        // read database
        // get schedule objects from db
        // get systemTime
        // list appsToBlock
        // list appsToUnblock
        // loop through Schedules
        //  for each Schedule:
        //      get Map of ProfileInSchedule
        //      for each ProfileInSchedule:
        //          if ProfileInSchedule.getStartTime() == systemTime:
        //              get profile object
        //              get apps from profile
        //              push all apps to block onto appsToBlock
        //          else if ProfileInSchedule.getEndTime() == systemTime:
        //              get profile object
        //              get apps from profile
        //              push all apps to unblock onto appsToUnblock
        //      set Schedule to active/inactive, update database
        // write to appBlock module: appsToBlock, appsToUnblock
        // write to notificationBlock module: appsToBlock, appsToUnblock
    }

    //function to check for instant profile activation -- how to know when to check

    /**
     * src = https://stackoverflow.com/questions/19604097/killbackgroundprocesses-no-working
     *
     * @param app: string of app to be killed
     */
    public void blockApps(String app) {
        final String TAG = "blockApps";

        // some in-built exceptions to the kill app function
        if (app.equals("com.htc.launcher") || app.equals("dreamteam.focus") || app.equals("com.google.android.apps.nexuslauncher"))
            return;
        else if(app.equals("com.whatsapp")){
        Log.i(TAG, app);
        ActivityManager am = (ActivityManager) this.getSystemService(Context.ACTIVITY_SERVICE);

        // go back to main screen
        Intent startMain = new Intent(Intent.ACTION_MAIN);
        startMain.addCategory(Intent.CATEGORY_HOME);
        startMain.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(startMain);

        // kill process
        am.killBackgroundProcesses(app);
        Toast.makeText(getBaseContext(),
                "Focus! has blocked " + getAppNameFromPackage(app),
                Toast.LENGTH_SHORT
        ).show();}
    }

    /**
     * src = https://stackoverflow.com/questions/41054355/how-to-get-app-name-by-package-name-in-android
     *
     * @param packageName: string of package name from which app name is derived
     */
    public String getAppNameFromPackage(String packageName) {
        PackageManager manager = getApplicationContext().getPackageManager();
        ApplicationInfo info;
        try {
            info = manager.getApplicationInfo(packageName, 0);
        } catch (final Exception e) {
            info = null;
        }
        String appName = (String) (info != null ? manager.getApplicationLabel(info) : "this app");
        return appName;
    }

    /**
     * src = https://stackoverflow.com/questions/2166961/determining-the-current-foreground-application-from-a-background-task-or-service
     * check which app comes to foreground,
     * TODO: need to define a way so that this is fast and there is no delay(doesnt rely on original runnable),
     * Todo: and also make killing app faster so that app main screen doesn't show up.
     */
    public void printForegroundTask() {
        final String TAG = "printForegroundTask";
        String currentApp = "NULL";

        if(android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {

            UsageStatsManager usm = (UsageStatsManager) this.getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();
            List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  time - 1000*1000, time);

            if (appList != null && appList.size() > 0) {
                SortedMap<Long, UsageStats> mySortedMap = new TreeMap<Long, UsageStats>();
                for (UsageStats usageStats : appList) {
                    mySortedMap.put(usageStats.getLastTimeUsed(), usageStats);
                }
                if (mySortedMap != null && !mySortedMap.isEmpty()) {
                    currentApp = mySortedMap.get(mySortedMap.lastKey()).getPackageName();
                }
            }

        } else {
            ActivityManager am = (ActivityManager)this.getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> tasks = am.getRunningAppProcesses();
            currentApp = tasks.get(0).processName;
        }

        Log.i(TAG, currentApp);

        blockApps(currentApp);
    }

}


/**
 * TODO
 * - blockApp(String): Toast user the app is blocked
 * - unblockApp(String)
 * - check SQLite version number
 * - request user to grant permission :   src: https://developer.android.com/training/permissions/requesting.html
 * -
 * -
 */