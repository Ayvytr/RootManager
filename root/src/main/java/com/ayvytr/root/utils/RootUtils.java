package com.ayvytr.root.utils;

import android.os.Build;

import com.ayvytr.root.container.Command;

/**
 * This class is a set of methods used in {@link com.ayvytr.root.Roots}.
 *
 * @author Chris
 */
public class RootUtils
{
    private static int cmdID = 0;

    /**
     * Get a command Id for each {@link Command}.
     *
     * @return the actual ID.
     */
    public static int generateCommandID()
    {
        cmdID = cmdID + 1;
        return cmdID;
    }

    /**
     * Check if Android 4.4 and upper.
     *
     * @return true for 4.4 upper.
     */
    public static boolean isKitKatUpper()
    {
        return Build.VERSION.SDK_INT >= 19;
    }
}
