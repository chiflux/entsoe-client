package entsoe;

public class Utils {

    public static String padIntegerWithZeros(int number, int minWidth) {
        return String.format("%0" + minWidth + "d", number);
    }

    public static String padIntegerWithSpaces(int number, int minWidth) {
        return String.format("%" + minWidth + "d", number);
    }

}
