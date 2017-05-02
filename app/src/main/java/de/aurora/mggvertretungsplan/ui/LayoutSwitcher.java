package de.aurora.mggvertretungsplan.ui;

import android.support.annotation.StyleRes;

import de.aurora.mggvertretungsplan.R;

public class LayoutSwitcher {
    private static final int stdPrimary = 0xFF757575;
    private static final int bluePrimary = 0xFF64B5F6;
    private static final int pinkPrimary = 0xFFf06292;
    private static final int greenPrimary = 0xFF9ccc65;
    private static final int orangePrimary = 0xFFff9800;

    @StyleRes
    public static int getTheme(int themeID) {
        switch (themeID) {
            case 0:
                return R.style.AppTheme_Light;
            case 1:
                return R.style.BlueTheme;
            case 2:
                return R.style.PinkTheme;
            case 3:
                return R.style.GreenTheme;
            case 4:
                return R.style.OrangeTheme;
            default:
                return R.style.AppTheme_Light;
        }
    }

    public static int getID(int color){
        switch (color) {
            case stdPrimary:
                return 0;
            case bluePrimary:
                return 1;
            case pinkPrimary:
                return 2;
            case greenPrimary:
                return 3;
            case orangePrimary:
                return 4;
            default:
                return 0;
        }
    }
}
