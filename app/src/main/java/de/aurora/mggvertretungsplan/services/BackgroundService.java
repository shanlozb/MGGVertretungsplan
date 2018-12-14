package de.aurora.mggvertretungsplan.services;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.JobIntentService;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;

import de.aurora.mggvertretungsplan.R;
import de.aurora.mggvertretungsplan.StorageUtilities;
import de.aurora.mggvertretungsplan.datamodel.TimeTable;
import de.aurora.mggvertretungsplan.parsing.BaseParser;
import de.aurora.mggvertretungsplan.parsing.BaseParser.ParsingCompleteListener;
import de.aurora.mggvertretungsplan.parsing.MGGParser;
import de.aurora.mggvertretungsplan.parsing.ParsingTask;
import de.aurora.mggvertretungsplan.util.NotificationHelper;

import static de.aurora.mggvertretungsplan.networking.ConnectionManager.isConnectionActive;


public class BackgroundService extends JobIntentService implements ParsingCompleteListener {
    private final static String TAG = "BackgroundService";
    private final static String CHANNEL_NAME = "default";
    public static final int JOB_ID = 0x01;
    private BaseParser websiteParser;
    private SharedPreferences sp;

    public BackgroundService() {

    }

    public static void enqueueWork(Context context, Intent intent) {
        enqueueWork(context, BackgroundService.class, JOB_ID, intent);
    }

    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        Log.d(TAG, "Start Service");
        websiteParser = new MGGParser();
        updateData();
        stopSelf();
    }

    private void updateData() {
        Log.d(TAG, "UpdateData");
        if (isConnectionActive(this)) {
            sp = PreferenceManager.getDefaultSharedPreferences(this);

            try {
                ParsingTask parsingTask = new ParsingTask(this, websiteParser);
                parsingTask.startParsing();
            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        } else {
            Log.d(TAG, "No internet Connection. Scheduling next alarm in 10 mins.");
            long tenMinsInMillis = 60 * 10 * 1000;
            ServiceScheduler serviceScheduler = new ServiceScheduler();
            serviceScheduler.setAlarmManager(getApplicationContext(), tenMinsInMillis);
        }
    }

    @Override
    public void onParsingComplete(TimeTable timeTable) {
        Log.d(TAG, "Parsing complete - Checking for changes");

        if (timeTable == null) {
            Log.d(TAG, "TimeTable is null");
            return;
        }

        sp = PreferenceManager.getDefaultSharedPreferences(this);
        String class_name = sp.getString("KlasseGesamt", "5a");


        TimeTable timeTable_saved = new TimeTable();
        String data = StorageUtilities.readFile(this);

        if (!data.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(data);
                timeTable_saved = new TimeTable(jsonArray);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
                return;
            }
        }

        // Compare new data with old data
        int totalDiffs = timeTable.getTotalDifferences(timeTable_saved, class_name);

        // Get new cancellations
        // new_cancellations = ...

        // Get removed cancellations
        // removed_cancellations = ...

        // Get changed cancellations
        // changed_cancellations = ...

        // "x neue Ausfälle"
        // "x Änderung/en am Vertretugnsplan"
        // "..."


        Log.d(TAG, String.format("Total differences: %d", totalDiffs));

        String ticker = getResources().getString(R.string.notification_cancellations_ticker);
        String title = getResources().getString(R.string.notification_cancellations_title);
        String infoOne = getResources().getString(R.string.notification_cancellations_infoOne);
        String infoMany = getResources().getString(R.string.notification_cancellations_infoMany);

        NotificationHelper notificationHelper = new NotificationHelper(this);

        if (totalDiffs == 1) {
            notificationHelper.notifyChanges(ticker, title, String.format(infoOne, 1));
        } else if (totalDiffs > 1) {
            notificationHelper.notifyChanges(ticker, title, String.format(infoMany, totalDiffs));
        }

        saveData(timeTable);
    }

    private void saveData(TimeTable timeTable) {
        Log.d(TAG, "Saving data.json to disk");
        try {
            StorageUtilities.writeToFile(this, timeTable.toJSON().toString());
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }

}