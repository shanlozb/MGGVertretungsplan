package de.aurora.mggvertretungsplan.datamodel;

import java.util.ArrayList;

/**
 * Created by Rico on 19.09.2017.
 */

public class TimeTable {
    private ArrayList<TimeTableDay> timeTableDays = new ArrayList<>();

    public TimeTable() {

    }

    public void addTimeTableDay(TimeTableDay ttd) {
        this.timeTableDays.add(ttd);
    }

    public TimeTableDay getDay(int index) {
        if (index <= (timeTableDays.size() - 1)) {
            return timeTableDays.get(index);
        }

        return null;
        //TODO error
    }

    public void switchDays() {
        //TODO
    }
}