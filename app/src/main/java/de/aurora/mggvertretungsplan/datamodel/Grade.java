package de.aurora.mggvertretungsplan.datamodel;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.aurora.mggvertretungsplan.util.Logger;

/**
 * Created by Rico on 16.11.2017.
 */

public class Grade {
    private static final String TAG = "Grade";
    private String classLayer, classTitle;

    Grade(String grade) {
        Logger.d(TAG, String.format("Creating new Grade object for class: %s", grade));
        if ("K1".equals(grade) || "K2".equals(grade)) {
            classLayer = grade;
            classTitle = "";
        } else {
            try {
                Matcher matcher = Pattern.compile("([0-9]+)([a-fA-F])").matcher(grade);

                if (!matcher.find()) {
                    initializeOnException();
                    return;
                }

                classLayer = matcher.group(1);
                classTitle = matcher.group(2);
            } catch (IllegalStateException e) {
                initializeOnException();
                Logger.e(TAG, String.format("IllegalStateException: %s", e.getMessage()));
            } catch (IndexOutOfBoundsException e) {
                initializeOnException();
                Logger.e(TAG, String.format("IndexOutOfBoundsException: %s", e.getMessage()));
            } catch (NullPointerException e) {
                initializeOnException();
                Logger.e(TAG, String.format("NullPointerException: %s", e.getMessage()));
            }
        }
    }

    Grade(String classLayer, String classTitle) {
        this.classLayer = classLayer;
        this.classTitle = classTitle;
    }

    boolean matches(String classString) {
        if (null == classString) {
            return false;
        }

        if (classString.contains("K1")) {
            return "K1".equals(this.toString());
        }
        if (classString.contains("K2")) {
            return "K2".equals(this.toString());
        }

        Matcher matcher = Pattern.compile("([0-9]+)([a-f]+)").matcher(classString);

        if (matcher.find()) {
            try {
                String inputClassLayer = matcher.group(1);
                String inputClassTitle = matcher.group(2);

                //noinspection SimplifiableIfStatement
                if (null == inputClassLayer || null == inputClassTitle) {
                    return false;
                }

                return inputClassLayer.equals(classLayer) && inputClassTitle.contains(classTitle);
            } catch (IllegalStateException e) {
                Logger.e("Grade", String.format("IllegalStateException: %s", e.getMessage()));
                return false;
            } catch (IndexOutOfBoundsException e) {
                Logger.e("Grade", String.format("IndexOutOfBoundsException: %s", e.getMessage()));
                return false;
            }
        }

        return false;
    }

    @Override
    public String toString() {
        return String.format("%s%s", classLayer, classTitle);
    }

    private void initializeOnException() {
        this.classLayer = "5";
        this.classTitle = "a";
    }
}
