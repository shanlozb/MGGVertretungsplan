package de.aurora.mggvertretungsplan.datamodel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;

import de.aurora.mggvertretungsplan.util.Logger;

/**
 * Created by Rico on 19.09.2017.
 */

public class TimeTableDay {
    private static final String TAG = "TimeTableDay";

    private final ArrayList<TimeTableElement> timeTableElements = new ArrayList<>();
    private Date date = new Date();
    private Week week;

    public TimeTableDay(String date, String week, ArrayList<ArrayList<String>> timeTableDay_List) {
        this.week = new Week(week);
        setDate(date);

        for (ArrayList<String> zeile : timeTableDay_List) {
            TimeTableElement timeTableElement = new TimeTableElement(zeile.get(0), zeile.get(1), zeile.get(2), zeile.get(3), zeile.get(4), zeile.get(5), zeile.get(6));
            addElement(timeTableElement);
        }

        mergeConsecutiveCancellations();
    }

    public TimeTableDay(JSONObject jsonObject) {
        try {
            String date = jsonObject.getString("date");
            setDate(date);

            String mWeek = jsonObject.getString("week");
            this.week = new Week(mWeek);

            JSONArray jsonArray = jsonObject.getJSONArray("elements");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonDay = (JSONObject) jsonArray.get(i);
                TimeTableElement tte = new TimeTableElement(jsonDay);
                addElement(tte);
            }
        } catch (JSONException e) {
            Logger.e(TAG, e.getMessage());
        }
    }

    private void addElement(TimeTableElement tte) {
        int index = 0;
        for (int i = 0; i < timeTableElements.size(); i++) {
            if (tte.getHour_I() < timeTableElements.get(i).getHour_I()) {
                break;
            }
            index++;
        }

        timeTableElements.add(index, tte);
    }

    public boolean isInFuture(Date currentDate) {
        int sixteenHrsInMillis = 16 * 60 * 60 * 1000;
        return (getDate().getTime() + sixteenHrsInMillis >= currentDate.getTime());
    }

    public boolean isInFuture() {
        return isInFuture(new Date());
    }

    public Week getWeek() {
        return this.week;
    }

    public Date getDate() {
        return date;
    }

    private void setDate(String date) {
        SimpleDateFormat fullDateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
        int currentYear = new GregorianCalendar().get(GregorianCalendar.YEAR);

        try {
            if (date.length() == 6)
                this.date = fullDateFormat.parse(date + currentYear);
            else {
                this.date = fullDateFormat.parse(date);
            }
        } catch (ParseException e) {
            Logger.e(TAG, e.getMessage());
            this.date = new Date();
        }
    }

    public String getDateString() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY);
        return dateFormat.format(date);
    }

    public String getFullDateString() {
        SimpleDateFormat fullDateFormat = new SimpleDateFormat("EEEE, dd.MM.yyyy", Locale.GERMANY);
        return fullDateFormat.format(date);
    }

    public ArrayList<TimeTableElement> getElements(String className) {
        ArrayList<TimeTableElement> elementsOfClass = new ArrayList<>();
        Grade grade = new Grade(className);

        for (TimeTableElement tte : timeTableElements) {
            String elementClassName = tte.getClass_name();
            // TODO Remove the "contains" part
            if (grade.matches(elementClassName) || elementClassName.contains(className))
                elementsOfClass.add(tte);
        }

        return elementsOfClass;
    }

    public ArrayList<ArrayList<String>> getArrayList() {
        ArrayList<ArrayList<String>> elements = new ArrayList<>();

        for (TimeTableElement tte : timeTableElements) {
            elements.add(tte.getElementAsList());
        }
        return elements;
    }

    public ArrayList<ArrayList<String>> getArrayList(String className) {
        ArrayList<ArrayList<String>> elements = new ArrayList<>();

        for (TimeTableElement tte : getElements(className)) {
            elements.add(tte.getElementAsList());
        }

        return elements;
    }

    public int getElementsCount(String className) {
        return getElements(className).size();
    }

    // Returns the number of differences between two lists
    public int getDifferences(TimeTableDay ttd, String className) {
        ArrayList<TimeTableElement> savedElements = ttd.getElements(className);
        ArrayList<TimeTableElement> newElements = this.getElements(className);

        // Remove same elements, which are contained in both lists
        for (int i = 0; i < newElements.size(); i++) {
            TimeTableElement element1 = newElements.get(i);
            for (int j = 0; j < savedElements.size(); j++) {
                TimeTableElement element2 = savedElements.get(j);

                if (element1.equals(element2)) {
                    newElements.remove(i);
                    savedElements.remove(j);
                    i--;
                    break;
                }
            }
        }

        // Remove similar elements, which are contained in both lists
        for (int i = 0; i < newElements.size(); i++) {
            TimeTableElement element1 = newElements.get(i);
            for (int j = 0; j < savedElements.size(); j++) {
                TimeTableElement element2 = savedElements.get(j);

                if (element1.getDiffAmount(element2) == 1) {
                    // This else part catches elements where only one part (hour, subject, etc.) has changed
                    // Without it, every *change* of an existing element would be counted twice
                    savedElements.remove(j);
                    break;
                }
            }
        }

        // savedElements now contains only those elements which are no longer in the TimeTable
        // newElements now only contains those elements which are new (not saved yet) or have changed in a single part
        int changesToOldCancellations = savedElements.size();
        int newCancellations = newElements.size();

        return changesToOldCancellations + newCancellations;
    }

    // Checks if this and the given day are at the same date
    public boolean isSameDay(TimeTableDay ttd) {
        return date.getTime() == ttd.getDate().getTime();
    }

    // Merges cancellations together (3. & 4. -> 3-4)
    private void mergeConsecutiveCancellations() {
        if (timeTableElements.size() <= 1) {
            return;
        }

        for (int i = 0; i < timeTableElements.size() - 1; i++) {
            TimeTableElement tte = timeTableElements.get(i);

            for (int j = i + 1; j < timeTableElements.size(); j++) {
                TimeTableElement tte2 = timeTableElements.get(j);

                if (tte.getHour().length() <= 2 && tte2.getHour().length() <= 2 &&
                        tte.getHour_I() == (tte2.getHour_I() - 1)) {
                    if (tte.getType() == tte2.getType() &&
                            tte.getClass_name().equals(tte2.getClass_name()) &&
                            tte.getRoom().equals(tte2.getRoom()) &&
                            tte.getNewRoom().equals(tte2.getNewRoom()) &&
                            tte.getSubject().equals(tte2.getSubject()) &&
                            tte.getNewSubject().equals(tte2.getNewSubject())) {

                        String newTime = String.format("%s-%s", tte.getHour(), tte2.getHour());
                        String newInfo;

                        if (tte.getInfo().equals(tte2.getInfo()))
                            newInfo = tte.getInfo();
                        else if (tte.getInfo().isEmpty() || tte2.getInfo().isEmpty())
                            newInfo = String.format("%s%s", tte.getInfo(), tte2.getInfo());
                        else
                            newInfo = String.format("%s - %s", tte.getInfo(), tte2.getInfo());

                        TimeTableElement replacement = new TimeTableElement(newTime, tte.getClass_name(), tte.getSubject(), tte.getNewSubject(), tte.getRoom(), tte.getNewRoom(), newInfo);

                        timeTableElements.remove(tte);
                        timeTableElements.remove(tte2);
                        addElement(replacement);

                        i--;
                        break;
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append(getFullDateString());

        for (TimeTableElement tte : timeTableElements) {
            result.append("\n").append(tte.toString());
        }

        return result.toString().trim();
    }

    /**
     * Formats the TTD in a beautiful way to share the TT via other apps
     * @return Beautiful formatted string of the TTD's content
     */
    public String toShareString() {
        StringBuilder result = new StringBuilder();
        result.append(getFullDateString());
        result.append(", ");
        result.append(this.week.toString());
        result.append("-Woche\n");

        if (timeTableElements.isEmpty()) {
            //result.append(getString(R.string.card_no_information));
            //TODO Remove hardcoded string
            result.append("Keine Ausfälle!");
            result.append("\n\n");
            return result.toString();
        }

        for (TimeTableElement tte : timeTableElements) {
            result.append(tte.toShareString());
        }
        result.append("\n");

        return result.toString();
    }

    public JSONObject toJSON() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("date", getDateString());
        jsonObject.put("week", week.toString());

        JSONArray jsonArray = new JSONArray();

        for (TimeTableElement tte : timeTableElements) {
            jsonArray.put(tte.toJSON());
        }

        jsonObject.put("elements", jsonArray);
        return jsonObject;
    }
}
