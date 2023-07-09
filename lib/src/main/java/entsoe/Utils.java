package entsoe;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class Utils {

    private static final double GERMAN_TAX = 1.19d;

    private static final double TRANSNET_NET_FEE_PER_KWH = 15.39d;

    /**
     * Gross price example calculation for TransnetBW (Germany)
     * @param spotPrice the spot price in cent/kWh
     * @return The estimated Gross Price for transnetBW customers
     */
    public static BigDecimal getTransnetBWGrossPrice(BigDecimal spotPrice) {
        return BigDecimal.valueOf(TRANSNET_NET_FEE_PER_KWH + spotPrice.doubleValue() * GERMAN_TAX).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Pad integer with zeros
     * @param number the number to pad
     * @param minWidth the desired length of the padded number
     * @return the padded number
     */
    public static String padIntegerWithZeros(int number, int minWidth) {
        return String.format("%0" + minWidth + "d", number);
    }

    /**
     * Pad integer with spaces
     * @param number the number to pad
     * @param minWidth the desired length of the padded number
     * @return the padded number
     */
    public static String padIntegerWithSpaces(int number, int minWidth) {
        return String.format("%" + minWidth + "d", number);
    }


}
