package dreamteam.focus.server;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import dreamteam.focus.Profile;
import dreamteam.focus.ProfileInSchedule;
import dreamteam.focus.Repeat_Enum;
import dreamteam.focus.Schedule;

public class DatabaseConnector extends SQLiteOpenHelper {

    // All Static variables
    // Database Version
    private static final int DATABASE_VERSION =13;

    // Database Name
    private static final String DATABASE_NAME = "database";

    // Profiles table name
    private static final String TABLE_PROFILES = "profiles";

    // Profiles Table Columns names
    private static final String KEY_NAME = "name";
    private static final String KEY_ACTIVE = "active";

    // Blocked Apps Table Name
    private static final String TABLE_BLOCKED_APPS = "blocked_apps";

    //Blocked Apps Columns name
    private static final String KEY_PROFILE_NAME = "profile_name";
    private static final String KEY_APP_NAME = "app_name";

    // Blocked Notifications Table Name
    private static final String TABLE_BLOCKED_NOTIFICATIONS = "blocked_notifications";

    // Profile In Schedules Table Name
    private static final String TABLE_PROFILE_IN_SCHEDULE = "profile_in_schedule";

    // Profile In Schedules Columns name
    private static final String KEY_PROFILE_IN_SCHEDULE_ID = "profile_in_schedule_id";
    private static final String KEY_START_TIME = "start_time";
    private static final String KEY_END_TIME = "end_time";
    private static final String KEY_SCHEDULE_NAME = "schedule_name";

    // Schedules Table Name
    private static final String TABLE_SCHEDULES = "schedules";


    // Profile In Schedule Repeats Table Name
    private static final String TABLE_PROFILE_IN_SCHEDULE_REPEATS = "profile_in_schedule_repeats";

    // Profile In Schedule Repeats Column Name
    private static final String KEY_REPEAT_ENUM = "repeat_enum";


    public DatabaseConnector(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    //creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_PROFILES_TABLE = "CREATE TABLE " + TABLE_PROFILES + "("
                + KEY_NAME + " TEXT NOT NULL PRIMARY KEY UNIQUE,"
                + KEY_ACTIVE + " TEXT NOT NULL" + ")";

        String CREATE_BLOCKED_APPS_TABLE = "CREATE TABLE " + TABLE_BLOCKED_APPS + "("
                + KEY_PROFILE_NAME + " TEXT NOT NULL,"
                + KEY_APP_NAME + " TEXT NOT NULL" + ")";

        String CREATE_BLOCKED_NOTIFICATIONS_TABLE = "CREATE TABLE " + TABLE_BLOCKED_NOTIFICATIONS + "("
                + KEY_APP_NAME + " TEXT NOT NULL" + ")";

        String CREATE_PROFILE_IN_SCHEDULE_TABLE = "CREATE TABLE " + TABLE_PROFILE_IN_SCHEDULE + "("
                + KEY_PROFILE_IN_SCHEDULE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_PROFILE_NAME + " TEXT NOT NULL,"
                + KEY_START_TIME + " TEXT NOT NULL,"
                + KEY_END_TIME + " TEXT NOT NULL,"
                + KEY_SCHEDULE_NAME + " TEXT NOT NULL" + ")";

        String CREATE_SCHEDULES_TABLE = "CREATE TABLE " + TABLE_SCHEDULES + "("
                + KEY_SCHEDULE_NAME + " TEXT NOT NULL PRIMARY KEY UNIQUE,"
                + KEY_ACTIVE + " TEXT NOT NULL" + ")";

        String CREATE_PROFILE_IN_SCHEDULE_REPEATS_TABLE = "CREATE TABLE " + TABLE_PROFILE_IN_SCHEDULE_REPEATS + "("
                + KEY_PROFILE_IN_SCHEDULE_ID + " INTEGER NOT NULL,"
                + KEY_REPEAT_ENUM + " TEXT NOT NULL" + ")";

        db.execSQL(CREATE_PROFILES_TABLE);
        db.execSQL(CREATE_BLOCKED_APPS_TABLE);
        db.execSQL(CREATE_BLOCKED_NOTIFICATIONS_TABLE);
        db.execSQL(CREATE_PROFILE_IN_SCHEDULE_TABLE);
        db.execSQL(CREATE_SCHEDULES_TABLE);
        db.execSQL(CREATE_PROFILE_IN_SCHEDULE_REPEATS_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROFILES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BLOCKED_APPS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BLOCKED_NOTIFICATIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROFILE_IN_SCHEDULE);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROFILE_IN_SCHEDULE_REPEATS);
        // Create tables again
        onCreate(db);
    }

    /**
     * All CRUD(Create, Read, Update, Delete) Operations
     */

    public boolean createProfile(Profile profile) throws SQLException {

        SQLiteDatabase db = this.getWritableDatabase();

        //Throws on profile duplicacy
        ContentValues values = new ContentValues();

        values.put(KEY_NAME, profile.getName());
        values.put(KEY_ACTIVE, profile.isActive());
        //values.put(KEY_ACTIVATE_NOW_FOR_TIME, profile.getTimeActivate());

        // Inserting Row
        try {
            db.insertOrThrow(TABLE_PROFILES, null, values);
        } catch (SQLException e) {
            Log.d("error", e.getMessage());
            db.close();
            throw e;
        }

        addBlockedApps(profile);


        db.close();
        return true;
    }

    public boolean addBlockedApps(Profile profile) throws SQLException {

        SQLiteDatabase db = this.getWritableDatabase();

        ArrayList<String> profileBlockedApps = profile.getApps();
        for(int i=0; i<profileBlockedApps.size(); i++) {
            ContentValues values = new ContentValues();

            values.put(KEY_PROFILE_NAME, profile.getName());
            values.put(KEY_APP_NAME, profileBlockedApps.get(i));

            try {
                db.insertOrThrow(TABLE_BLOCKED_APPS, null, values);
            } catch (SQLException e) {
                Log.d("error", e.getMessage());
                db.close();
                throw e;
            }
        }
        return true;
    }

    public boolean updateProfile(String originalProfileName, Profile updatedProfile) {

        SQLiteDatabase db = this.getWritableDatabase();

        return db.delete(TABLE_PROFILES, KEY_NAME + "='" + originalProfileName + "'", null) > 0 &&
                db.delete(TABLE_BLOCKED_APPS, KEY_PROFILE_NAME + "='" + originalProfileName + "'", null) > 0 &&
                createProfile(updatedProfile);
    }


//    public boolean updateStudentInfo(int updId, int updEnrolNo, String updName, String updPhoneNo) {
//
//        SQLiteDatabase db = this.getWritableDatabase();
//
//        ContentValues args = new ContentValues();
//
//        args.put(KEY_ENROLL_NO, updEnrolNo);
//        args.put(KEY_NAME, updName);
//        args.put(KEY_PHONE_NO, updPhoneNo);
//
//        return db.update(TABLE_STUDENT_DETAIL, args, KEY_ID + "=" + updId, null) > 0;
//    }

    public boolean removeProfile(String profileName) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_PROFILES, KEY_NAME + "='" + profileName + "'", null) > 0 &&
                db.delete(TABLE_BLOCKED_APPS, KEY_PROFILE_NAME + "='" + profileName + "'", null) > 0 &&
                removeProfileFromAllSchedules(profileName);
    }

    private boolean removeProfileFromAllSchedules(String profileName) {
        SQLiteDatabase db = this.getWritableDatabase();

        return db.delete(TABLE_PROFILE_IN_SCHEDULE, KEY_PROFILE_NAME + "='" + profileName + "'", null) > 0;
    }

    //ACTIVATE PROFILE IS ONLY CALLED FOR INSTANT ACTIVATION
    public boolean activateProfile(ProfileInSchedule pis) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_ACTIVE, true);

        return db.update(TABLE_PROFILES, values, KEY_NAME + "='" + pis.getProfile().getName() + "'", null) > 0 &&
                addProfileInSchedule(pis, new Schedule("AnonymousSchedule", new ArrayList<ProfileInSchedule>(), true));
    }

    public boolean deactivateProfile(ProfileInSchedule pis) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_ACTIVE, false);

        return db.update(TABLE_PROFILES, values, KEY_NAME + "='" + pis.getProfile().getName() + "'", null) > 0 &&
                removeProfileFromSchedule(pis, "AnonymousSchedule");
    }

    private ArrayList<Repeat_Enum> getProfileInScheduleRepeats(Integer profileInScheduleID) {
        ArrayList<Repeat_Enum> repeats = new ArrayList<Repeat_Enum>();

        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_PROFILE_IN_SCHEDULE_REPEATS + " WHERE "
                + KEY_PROFILE_IN_SCHEDULE_ID + "=" + profileInScheduleID;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {

                Repeat_Enum repeatEnum = Repeat_Enum.valueOf(cursor.getString(1));
                // Adding contact to list
                repeats.add(repeatEnum);

            } while (cursor.moveToNext());
        }

        // return contact list
        return repeats;
    }

    private boolean addProfileInScheduleRepeats(ProfileInSchedule pis, Integer profileInScheduleID) {
        SQLiteDatabase db = this.getWritableDatabase();
        ArrayList<Repeat_Enum> repeats = pis.repeatsOn();
        for(int i=0; i<repeats.size(); i++) {
            ContentValues values = new ContentValues();

            values.put(KEY_PROFILE_IN_SCHEDULE_ID, profileInScheduleID);
            values.put(KEY_REPEAT_ENUM, repeats.get(i).toString());

            try {
                db.insertOrThrow(TABLE_PROFILE_IN_SCHEDULE_REPEATS, null, values);
            } catch (SQLException e) {
                Log.d("error", e.getMessage());
                db.close();
                throw e;
            }
        }

        return true;
    }

    public boolean addProfileInSchedule(ProfileInSchedule pis, Schedule schedule) throws SQLException {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_PROFILE_NAME, pis.getProfile().getName());
        values.put(KEY_START_TIME, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(pis.getStartTime()));
        values.put(KEY_END_TIME, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").format(pis.getEndTime()));
        values.put(KEY_SCHEDULE_NAME, schedule.getName());

        try {
            db.insertOrThrow(TABLE_PROFILE_IN_SCHEDULE, null, values);
        } catch (SQLException e) {
            Log.d("error", e.getMessage());
            db.close();
            throw e;
        }

        String selectQuery = "SELECT  * FROM " + TABLE_PROFILE_IN_SCHEDULE + " WHERE " + KEY_PROFILE_NAME
                + "='" + pis.getProfile().getName() + "'";

        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()) {
            addProfileInScheduleRepeats(pis, cursor.getInt(0));
        }
        return true;
    }

    public boolean removeProfileFromSchedule(ProfileInSchedule pis, String scheduleName) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_PROFILE_IN_SCHEDULE,
                KEY_PROFILE_NAME + "='" + pis.getProfile().getName() + "' AND "
                        + KEY_SCHEDULE_NAME + "='" + scheduleName + "'", null) > 0;
    }

    public ArrayList<String> getBlockedApps(String profileName) {
        ArrayList<String> blockedApps = new ArrayList<String>();

        String selectQuery = "SELECT * FROM " + TABLE_BLOCKED_APPS + " WHERE "
                + KEY_PROFILE_NAME + "='" + profileName + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if(cursor.moveToFirst()) {
            do {

                String appName = cursor.getString(1);
                // Adding contact to list
                blockedApps.add(appName);

            } while (cursor.moveToNext());
        }

        return blockedApps;
    }

    public ArrayList<Profile> getProfiles() {
        ArrayList<Profile> profiles = new ArrayList<Profile>();

        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_PROFILES;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {

                Profile profile = new Profile(cursor.getString(0), getBlockedApps(cursor.getString(0)));
                profile.setActive(Boolean.parseBoolean(cursor.getString(1)));

                // Adding contact to list
                profiles.add(profile);

            } while (cursor.moveToNext());
        }

        // return contact list
        return profiles;
    }

    public ArrayList<Schedule> getSchedules() throws ParseException {
        ArrayList<Schedule> schedules = new ArrayList<Schedule>();

        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_SCHEDULES;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {

                Schedule schedule = new Schedule(cursor.getString(0), getProfilesInSchedule(cursor.getString(0)), Boolean.parseBoolean(cursor.getString(2)));
                schedule.setActive(Boolean.parseBoolean(cursor.getString(1)));

                // Adding contact to list
                schedules.add(schedule);

            } while (cursor.moveToNext());
        }

        // return contact list
        return schedules;
    }

    public ArrayList<ProfileInSchedule> getProfilesInSchedule(String scheduleName) throws ParseException {
        ArrayList<ProfileInSchedule> profilesInSchedule = new ArrayList<ProfileInSchedule>();

        // Select All Query
        String selectQuery = "SELECT  * FROM " + TABLE_PROFILE_IN_SCHEDULE + " WHERE "
                + KEY_SCHEDULE_NAME + "='" + scheduleName + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        // looping through all rows and adding to list
        if (cursor.moveToFirst()) {
            do {

                Profile profile = new Profile(cursor.getString(1), getBlockedApps(cursor.getString(1)));
                ProfileInSchedule profileInSchedule = new ProfileInSchedule(profile, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(cursor.getString(3)),
                        new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(cursor.getString(3)), getProfileInScheduleRepeats(cursor.getInt(0)));

                // Adding contact to list
                profilesInSchedule.add(profileInSchedule);

            } while (cursor.moveToNext());
        }

        // return contact list
        return profilesInSchedule;
    }

    public boolean addBlockedNotification(String notificationAppName) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_APP_NAME, notificationAppName);

        try {
            db.insertOrThrow(TABLE_BLOCKED_NOTIFICATIONS, null, values);
        } catch (SQLException e) {
            Log.d("error", e.getMessage());
            db.close();
            throw e;
        }
        return true;
    }

    public boolean activateSchedule(Schedule schedule) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_ACTIVE, true);

        return db.update(TABLE_SCHEDULES, values, KEY_SCHEDULE_NAME + "='" + schedule.getName() + "'", null) > 0;
    }

    public boolean deactivateSchedule(Schedule schedule) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();

        values.put(KEY_ACTIVE, false);

        return db.update(TABLE_SCHEDULES, values, KEY_SCHEDULE_NAME + "='" + schedule.getName() + "'", null) > 0;
    }

    public Integer getNotificationsCountForApp(String appName) {

        String selectQuery = "SELECT COUNT(*) FROM " + TABLE_BLOCKED_NOTIFICATIONS + " WHERE " + KEY_APP_NAME + "='" + appName + "'";

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        db.delete(TABLE_BLOCKED_NOTIFICATIONS, KEY_APP_NAME + "='" + appName + "'", null);
        return cursor.getCount();
    }

    public HashMap<String, Integer> getNotificationsForSchedule(Schedule schedule) {
        HashMap<String, Integer> notifications = new HashMap<String, Integer>();

        String selectAppQuery = "SELECT " + KEY_APP_NAME + " FROM " + TABLE_BLOCKED_APPS;

        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursorApps = db.rawQuery(selectAppQuery, null);

        if(cursorApps.moveToFirst()) {
            do {
                String appName = cursorApps.getString(0);
                Integer notificationsCount = getNotificationsCountForApp(appName);
                // Adding contact to list
                notifications.put(appName, notificationsCount);

            } while (cursorApps.moveToNext());
        }

        return notifications;
    }

    public boolean addSchedule(Schedule schedule) {
        SQLiteDatabase db = this.getWritableDatabase();

        //Throws on profile duplicacy
        ContentValues values = new ContentValues();

        values.put(KEY_SCHEDULE_NAME, schedule.getName());
        values.put(KEY_ACTIVE, schedule.isActive());


        // Inserting Row
        try {
            db.insertOrThrow(TABLE_SCHEDULES, null, values);
        } catch (SQLException e) {
            Log.d("error", e.getMessage());
            db.close();
            throw e;
        }

        ArrayList<ProfileInSchedule> calendar = schedule.getCalendar();
        for(int i=0; i<calendar.size(); i++) {
            addProfileInSchedule(calendar.get(i), schedule);
        }

        return true;
    }

    public boolean removeSchedule(String scheduleName) throws ParseException {
        SQLiteDatabase db = this.getWritableDatabase();

        //Delete profileinschedule
        ArrayList<ProfileInSchedule> profilesInSchedule = getProfilesInSchedule(scheduleName);

        for(int i=0; i<profilesInSchedule.size(); i++) {
            removeProfileFromSchedule(profilesInSchedule.get(i), scheduleName);
        }

        return db.delete(TABLE_SCHEDULES, KEY_SCHEDULE_NAME + "='" + scheduleName + "'", null) > 0 &&
                db.delete(TABLE_PROFILE_IN_SCHEDULE, KEY_SCHEDULE_NAME + "='" + scheduleName + "'", null) > 0;
    }

    public Integer getDatabaseVersion() {
        return DATABASE_VERSION;
    }

//    // Getting All Students
//    public List<Student> getAllStudentList() {
//
//
//        List<Student> studentList = new ArrayList<Student>();
//
//        // Select All Query
//        String selectQuery = "SELECT  * FROM " + TABLE_STUDENT_DETAIL;
//
//        SQLiteDatabase db = this.getWritableDatabase();
//        Cursor cursor = db.rawQuery(selectQuery, null);
//
//        // looping through all rows and adding to list
//        if (cursor.moveToFirst()) {
//            do {
//
//                Student stdnt = new Student();
//                stdnt.set_id(Integer.parseInt(cursor.getString(0)));
//                stdnt.set_enroll_no(Integer.parseInt(cursor.getString(1)));
//                stdnt.set_name(cursor.getString(2));
//                stdnt.set_phone_number(cursor.getString(3));
//
//                // Adding contact to list
//                studentList.add(stdnt);
//
//            } while (cursor.moveToNext());
//        }
//
//        // return contact list
//        return studentList;
//    }



}

//public class DatabaseConnector {
//    public boolean createProfile(Profile profile) {} DONE for profiles, blockedApps TESTED
//
//    public boolean removeProfile(Profile profile) {} DONE for profiles, blockedApps, blockedNotifications, profileinschedule TESTED
//
//    public boolean updateProfile(Profile profile) {} DONE for remove Profile, blockedApps, and updated createProfile TESTED
//
//    public boolean activateProfile(ProfileInSchedule pis) {} DONE
//
//    public boolean deactivateProfile(Profile profile) {} DONE
//
//    public HashMap<String, Integer> getNotificationsCountForApp(String appName) {} DONE
//
//    public boolean addSchedule(Schedule schedule) {} DONE
//
//    public boolean removeSchedule(Schedule schedule) {} DONE
//
//    public boolean updateSchedule(Schedule schedule) {}
//
//    public boolean addProfileInSchedule(ProfileInSchedule pis, Schedule schedule) {} DONE
//
//    public boolean removeProfileFromSchedule(ProfileInSchedule pis, Schedule schedule) {} DONE
//
//    public boolean activateSchedule(Schedule schedule) {} DONE
//
//    public boolean deactivateSchedule(Schedule schedule) {} DONE
//
//    public HashMap<String, Integer> getNotificationsForSchedule(Schedule schedule) {} DONE

//public Array(Profile) getProfiles() {} DONE

//public Array(Profile) getSchedules() {} DONE
//}
