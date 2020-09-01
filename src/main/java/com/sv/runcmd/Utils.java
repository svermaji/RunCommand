package com.sv.runcmd;

import java.nio.file.FileSystems;
import java.nio.file.Path;

/**
 * Created by svg on 11-Oct-2017
 */
public class Utils {

    public static final String EMPTY = "";
    public static final String SPACE = " ";
    public static final String DOUBLE_SPACE = SPACE+SPACE;
    public static final String DASH = "-";
    public static final String SP_DASH_SP = SPACE + DASH + SPACE;

    // Set of values that imply a true value.
    private static final String[] trueValues = {"Y", "YES", "TRUE", "T"};

    // Set of values that imply a false value.
    private static final String[] falseValues = {"N", "NO", "FALSE", "F"};

    /**
     * return true if param has non-null value
     *
     * @param item string to be checked
     * @return boolean status of operation
     */
    public static boolean hasValue ( String item )
    {
        return( (item != null) && (item.length() > 0) );
    }

    public static Path createPath (String path) {
        return FileSystems.getDefault().getPath(path);
    }

    /**
     * Return the boolean equivalent of the string argument.
     *
     * @param value Value containing string representation of a boolean value.
     * @return Boolean true/false depending on the value of the input.
     * @throws Exception Thrown if input does not have a valid value.
     */
    public static boolean getBoolean(String value) throws Exception {
        if (!hasValue(value)) {
            throw new Exception("ERROR: Can't convert a null/empty string value to a boolean.");
        }

        value = value.trim();

        for (String trueValue : trueValues) {
            if (value.equalsIgnoreCase(trueValue))
                return true;
        }

        for (String falseValue1 : falseValues) {
            if (value.equalsIgnoreCase(falseValue1))
                return false;
        }

        //Construct error message containing list of valid values
        StringBuilder validValues = new StringBuilder();

        for (int Ix = 0; Ix < trueValues.length; Ix++) {
            if (Ix > 0)
                validValues.append(", ");

            validValues.append(trueValues[Ix]);
        }

        for (String falseValue : falseValues) {
            validValues.append(", ");
            validValues.append(falseValue);
        }

        throw new Exception("ERROR: Candidate boolean value [" + value
            + "] not in valid-value set [" + validValues.toString() + "].");
    }


    /**
     * Return the boolean equivalent of the string argument.
     *
     * @param value       Value containing string representation of a boolean value.
     * @param defaultBool Default boolean to use if the value is empty
     *                    or if it is an invalid value.
     * @return Boolean true/false depending on the value of the input.
     * @throws Exception Thrown if input does not have a valid value.
     */
    public static boolean getBoolean(String value, boolean defaultBool) throws Exception {
        if (!hasValue(value))
            return defaultBool;

        try {
            return getBoolean(value);
        } catch (Exception e) {
            return defaultBool;
        }
    }

    /**
     * returns true if char is numeric, else false
     *
     * @param ch char to check
     * @return boolean status of operation
     */
    public static boolean isNumeric(char ch) {
        //final String log = "isNumeric: ";
        int zero = (int) '0';
        int nine = (int) '9';
        int chVal = (int) ch;
        if (chVal <= nine && chVal >= zero) {
            //printMsg ( log + "Return TRUE for ch [" + ch + "]" );
            return true;
        }
        //printMsg ( log + "Return FALSE for ch [" + ch + "]" );
        return false;
    }

    /**
     * returns true if char is alphabetic, else false
     *
     * @param ch char to check
     * @return boolean status of operation
     */
    public static boolean isAlphabet(char ch) {
        //final String log = "isAlphabet: ";
        int a = (int) 'a';
        int A = (int) 'A';
        int z = (int) 'z';
        int Z = (int) 'Z';
        int chVal = (int) ch;

        if ((chVal <= z && chVal >= a) || ((chVal <= Z && chVal >= A))) {
            //printMsg ( log + "Return TRUE for ch [" + ch + "]" );
            return true;
        }
        //printMsg ( log + "Return FALSE for ch [" + ch + "]" );
        return false;
    }


    public static String getFileNameNoExtn(String file, String fileType) {
        if (!hasValue(file))
            return "";
        if (!hasValue(fileType))
            return file;
        return (file.endsWith(fileType)) ? file.substring(0, file.indexOf(fileType)-1) : file;
    }

    public static void sleep (long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public static String getFileSizeString(long fs) {
        long KB = 1024;
        float inKB = (float) fs/KB;
        float inMB = inKB/KB;
        float inGB = inMB/KB;
        if (inGB > 1) {
            return String.format("[%sGB]", formatFloat (inGB));
        } else if (inMB > 1) {
            return String.format("[%sMB]", formatFloat (inMB));
        } else if (inKB > 1) {
            return String.format("[%sKB]", formatFloat (inKB));
        }
        return String.format("[%sBytes]", fs);
    }

    private static String formatFloat(float size) {
        return String.format("%.2f", size);
    }
}
