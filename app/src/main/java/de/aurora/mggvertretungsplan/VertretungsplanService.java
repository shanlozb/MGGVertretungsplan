package de.aurora.mggvertretungsplan;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.Arrays;


public class VertretungsplanService extends Service implements AsyncTaskCompleteListener<String> {

    private SharedPreferences sp;
    private String klasse;

    public VertretungsplanService() {

    }

    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateData();
        //TODO Vibration zu Permissions hinzufügen
        stopSelf();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
    }

    private void updateData() {
        Log.v("VertretungsplanService", "UpdateData");
        if (aktiveVerbindung()) {
            sp = PreferenceManager.getDefaultSharedPreferences(this);
            klasse = sp.getString("KlasseGesamt", "5a");

            try {
                new DownloadWebPageTask(this).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, getString(R.string.vertretungsplan_url));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void notification(String ticker, String titel, String text) {
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        PendingIntent pIntent = PendingIntent.getActivity(getApplicationContext(), 0, intent, 0);
        Log.i("VertretungsplanService", "Notification!");
        @SuppressWarnings("deprecation")
        android.support.v7.app.NotificationCompat.Builder notification = (android.support.v7.app.NotificationCompat.Builder) new android.support.v7.app.NotificationCompat.Builder(this)
                .setContentTitle(titel)
                .setContentText(text)
                .setTicker(ticker)
                .setColor(getResources().getColor(R.color.accentColor))
                .setSmallIcon(R.drawable.icon)
                .setContentIntent(pIntent)
                .setAutoCancel(true);

        //.setVibrate(new long[]{0,300,200,300})
        //.setLights(Color.WHITE, 1000, 5000)

        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(0, notification.build());
    }

    //Überprüft Internetverbindung (true = vorhandene Verbindung, false = keine Verbindung)
    private boolean aktiveVerbindung() {
        final ConnectivityManager conMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo activeNetwork = conMgr.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnected();
    }


    public void onTaskComplete(String html) {
        Log.v("VertretungsplanService", "Check auf Vertretungen");
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        ArrayList<ArrayList<String>> tableOne, tableTwo;

        html = html.replace("&auml;", "ä").replace("&ouml;", "ö").replace("&uuml;", "ü");
        Document doc = Jsoup.parse(html);

        try {
            tableOne = hilfsMethoden.extractTable(doc, 0);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            tableOne = new ArrayList<>();
            tableOne.add(new ArrayList<>(Arrays.asList("", "", "", "", "", "", "")));
        }

        try {
            tableTwo = hilfsMethoden.extractTable(doc, 1);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            tableTwo = new ArrayList<>();
            tableTwo.add(new ArrayList<>(Arrays.asList("", "", "", "", "", "", "")));
        }

        tableOne = hilfsMethoden.datenAufbereiten(tableOne, klasse);
        tableTwo = hilfsMethoden.datenAufbereiten(tableTwo, klasse);

        ArrayList<ArrayList<String>> tableOne_saved = hilfsMethoden.getArrayList(sp.getString("ersteTabelle", ""));
        ArrayList<ArrayList<String>> tableTwo_saved = hilfsMethoden.getArrayList(sp.getString("zweiteTabelle", ""));

        int anzahlAusfaelle = tableOne.size() + tableTwo.size();

        if (anzahlAusfaelle > 0) {
            int count1 = hilfsMethoden.getDifferencesCount(tableOne, tableOne_saved);
            int count2 = hilfsMethoden.getDifferencesCount(tableTwo, tableTwo_saved);
            int gesamt = (count1 + count2);
//            Log.v("MyTag", "Gesamt: " + gesamt);
//            Log.v("MyTag", "tableOne: " + tableOne.toString() + " | " + tableOne_saved.toString());
//            Log.v("MyTag", "tableTwo: " + tableTwo.toString() + " | " + tableTwo_saved.toString());

            if (gesamt > 1) {
                notification("Stundenplan Änderung!", "MGG Vertretungsplan", gesamt + " Änderungen!"); //Push mit der Nachricht "Es fällt etwas aus!"
            } else if (gesamt == 1) {
                notification("Stundenplan Änderung!", "MGG Vertretungsplan", "Eine Änderung!"); //Push mit der Nachricht "Es fällt etwas aus!"
            }
        }
        //TODO neue Listen speichern?

    }

}