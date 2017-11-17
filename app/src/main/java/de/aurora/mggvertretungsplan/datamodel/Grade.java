package de.aurora.mggvertretungsplan.datamodel;

import android.util.Log;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Rico on 16.11.2017.
 */

public class Grade {
    private String classLayer, classTitle;

    public Grade(String grade) {
        Log.d("Grade", String.format("Creating new Grade object for class: %s", grade));
        if (grade.equals("K1") || grade.equals("K2")) {
            classLayer = grade;
            classTitle = "";
        } else {
            try {
                Matcher matcher = Pattern.compile("([0-9]+)([a-fA-F])").matcher(grade);
                matcher.find();
                classLayer = matcher.group(1);
                classTitle = matcher.group(2);
            } catch (IllegalStateException e) {
                Log.e("Grade", String.format("IllegalStateException: %s", e.getMessage()));
            } catch (IndexOutOfBoundsException e) {
                Log.e("Grade", String.format("IndexOutOfBoundsException: %s", e.getMessage()));
            }
        }
    }

    public Grade(String classLayer, String classTitle) {
        this.classLayer = classLayer;
        this.classTitle = classTitle;
    }

    public boolean matches(String classString) {
        Matcher matcher = Pattern.compile("([0-9]+)([a-f]+)").matcher(classString);

        if (matcher.find()) {
            try {
                String inputClassLayer = matcher.group(1);
                String inputClassTitle = matcher.group(2);

                if (null == inputClassLayer || null == inputClassTitle) {
                    return false;
                }

                return inputClassLayer.equals(classLayer) && inputClassTitle.contains(classTitle);
            } catch (IllegalStateException e) {
                Log.e("Grade", String.format("IllegalStateException: %s", e.getMessage()));
                return false;
            } catch (IndexOutOfBoundsException e) {
                Log.e("Grade", String.format("IndexOutOfBoundsException: %s", e.getMessage()));
                return false;
            }
        }

        return false;
    }
}