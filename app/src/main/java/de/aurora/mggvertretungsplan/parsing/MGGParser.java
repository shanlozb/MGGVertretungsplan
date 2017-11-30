package de.aurora.mggvertretungsplan.parsing;

import android.util.Log;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import de.aurora.mggvertretungsplan.datamodel.TimeTable;
import de.aurora.mggvertretungsplan.datamodel.TimeTableDay;

/**
 * Created by Rico on 22.09.2017.
 */

public class MGGParser extends BaseParser {
    private static final String timeTable_url = "https://www.mgg.karlsruhe.de/stupla/stupla.php";
    private static final String timeTable_url_2 = "https://www.mgg.karlsruhe.de/stupla/stuplamorgen.php";
    private ParsingCompleteListener callback;

    // Extracts the two tables from the html code
    private static ArrayList<ArrayList<String>> extractTable(Document doc) {
        Element table = doc.select("table.mon_list").get(0);
        Iterator<Element> rowIterator = table.select("tr").iterator();

        //TODO check if these selectors are present. Otherwise stop parsing and throw error!

        ArrayList<ArrayList<String>> tableArrayList = new ArrayList<>();

        while (rowIterator.hasNext()) {
            Iterator<Element> colIterator = rowIterator.next().select("td").iterator();
            ArrayList<String> tableRow = new ArrayList<>();
            while (colIterator.hasNext()) {
                tableRow.add(colIterator.next().text());
            }

            if (!tableRow.isEmpty()) {
                Collections.swap(tableRow, 3, 4);
                tableArrayList.add(tableRow);
            }
        }

        return tableArrayList;
    }

    // Remove double lines
    private static ArrayList<ArrayList<String>> deleteDoubles(ArrayList<ArrayList<String>> inputList) {
        Set<ArrayList<String>> set = new LinkedHashSet<>(inputList);
        inputList.clear();
        inputList.addAll(set);
        return inputList;
    }

    @Override
    public String getTimeTable_url() {
        return timeTable_url;
    }

    private TimeTableDay parseDay(String website, int index) {
        ArrayList<String> datesList = new ArrayList<>();

        website = website.replace("&nbsp;", "");
        Document doc = Jsoup.parse(website);

        Elements dates = doc.select("div.mon_title");

        for (Element date : dates) {
            datesList.add(date.text()); // parse the dates on the website
        }

        TimeTableDay day = null;
        try {
            ArrayList<ArrayList<String>> table = extractTable(doc);
            table = deleteDoubles(table);

            if (index >= datesList.size()) {
                return day;
            }

            if (table != null) {
                day = new TimeTableDay(datesList.get(index), table);
            } else
                day = new TimeTableDay(datesList.get(index), new ArrayList<ArrayList<String>>());
        } catch (IndexOutOfBoundsException e) {
            Log.e("MGGparser", "parseDay(): There is probably no content to extract!");
            Log.e("MGGparser", e.getMessage());
        }

        return day;
    }

    private TimeTable parse(ArrayList<String> websites) {
        TimeTable timeTable = new TimeTable();
        int index = 0;

        for (String website : websites) {
            try {
                TimeTableDay day = parseDay(website, index);

                if (null == day) {
                    index++;
                    continue;
                }

                timeTable.addDay(day);
            } catch (Exception e) {
                Log.e("MGGParser", e.getMessage());
            }
            index++;
        }

        return timeTable;
    }

    @Override
    public void startParsing(ParsingCompleteListener callback) {
        this.callback = callback;
        downloadWebsite(timeTable_url, timeTable_url_2);
    }

    @Override
    public void onTaskComplete(ArrayList<String> websites) {
        TimeTable timeTable = parse(websites);
        this.callback.onParsingComplete(timeTable);
    }
}
