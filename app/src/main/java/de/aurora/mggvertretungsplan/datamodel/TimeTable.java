package de.aurora.mggvertretungsplan.datamodel;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;

/**
 * Created by Rico on 19.09.2017.
 */

public class TimeTable {
    private final ArrayList<TimeTableDay> timeTableDays = new ArrayList<>();

    public TimeTable() {

    }

    // Adds a day right at the right place via insertionsort
    public void addDay(TimeTableDay ttd) {
        int index = 0;
        for (int i = 0; i < timeTableDays.size(); i++) {
            if (ttd.getDate().before(timeTableDays.get(i).getDate())) {
                break;
            }
            index++;
        }

        timeTableDays.add(index, ttd);
    }

    public TimeTableDay getDay(int index) {
        if (index <= (timeTableDays.size() - 1)) {
            return timeTableDays.get(index);
        }

        return null;
        //TODO error
    }

    public int getCount() {
        return timeTableDays.size();
    }

    public ArrayList<TimeTableDay> getAllDays() {
        return timeTableDays;
    }

    public int getTotalCancellations() {
        int cancellations = 0;

        for (TimeTableDay day : timeTableDays) {
            cancellations += day.getCancellations();
        }

        return cancellations;
    }

    public int getTotalDifferences(TimeTable savedTimeTable) {
        int differences = 0;
        Date currentDate = new Date();
        ArrayList<TimeTableDay> savedDays = savedTimeTable.getAllDays();

        for (TimeTableDay ttd : timeTableDays) {
            boolean sameDaySaved = false;
            int sixteenHrsInSecs = 60 * 60 * 16;
            if (currentDate.getTime() > ttd.getDate().getTime() + sixteenHrsInSecs) {
                Log.d("BackgroundService", String.format("Date in the past: %s", ttd.getDateString()));
                continue;
            }

            for (TimeTableDay saved_ttd : savedDays) {

                if (ttd.isSameDay(saved_ttd)) {
                    Log.d("TimeTable", String.format("Dates are the same - %s | %s", ttd.getDateString(), saved_ttd.getDateString()));
                    Log.d("BackgroundService", String.format("%s", ttd.toString()));
                    Log.d("BackgroundService", String.format("%s", saved_ttd.toString()));
                    differences += ttd.getDifferences(saved_ttd);
                    sameDaySaved = true;
                    break;
                }
            }

            if (!sameDaySaved)
                differences += ttd.getCancellations();
        }

        return differences;
    }

    @Override
    public String toString() {
        String result = "";

        for (TimeTableDay ttd : timeTableDays) {
            result += ttd.toString();
        }

        return result;
    }
}
