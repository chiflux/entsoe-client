package entsoe;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;

import static entsoe.Utils.padIntegerWithZeros;

/**
 * Helper record to handle the special date format used in the ENTSO-E API.
 * The record is comparable/sortable. It supports equals() and can thus be used without restriction as a key attribute in all Java Collection classes.
 * @param utcDate the utc date
 */
public record EntsoeDate(ZonedDateTime utcDate) implements Comparable<EntsoeDate> {

    /**
     * The default ZoneId
     */
    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    /**
     * Creates a new object based on the provided utc time.
     * @param utcDate the utc time
     */
    public EntsoeDate(ZonedDateTime utcDate) {
        this.utcDate = utcDate.withSecond(0).withNano(0);
    }

    /**
     * Constrict an new object based on an existing object and a relative offset (position).
     * @param entsoeDate The base date
     * @param position the relative position to the date compared
     * @param entsoeResolution the resolution to use. {@link EntsoeResolution#PT60M} supports only full hours, {@link EntsoeResolution#PT15M} supports 15-minute intervals
     * @return the new date
     */
    public static EntsoeDate fromENTSOEDate(EntsoeDate entsoeDate, int position, EntsoeResolution entsoeResolution) {
        int plusHours = position;
        int plusMinutes = 0;
        if (entsoeResolution==EntsoeResolution.PT15M) {
            plusHours = position / 4;
            plusMinutes = (position % 4) * 15;
        }
        return new EntsoeDate(entsoeDate.utcDate.plusHours(plusHours).plusMinutes(plusMinutes));
    }

    /**
     * Create a new object of the String representation of an ENTSO-E date.
     * @param entsoeFormat A date like 202301010123 what means Jan 1st, 2023 at 1:23am
     * @return new object
     */
    public static EntsoeDate fromENTSOEDateString(String entsoeFormat) {
        int year = Integer.parseInt(entsoeFormat.substring(0, 4));
        int month = Integer.parseInt(entsoeFormat.substring(4, 6));
        int day = Integer.parseInt(entsoeFormat.substring(6, 8));
        int hour;
        int minute;
        if (entsoeFormat.length() > 8) {
            hour = Integer.parseInt(entsoeFormat.substring(8, 10));
            minute = Integer.parseInt(entsoeFormat.substring(10, 12));
        } else {
            hour = 0;
            minute = 0;
        }
        ZonedDateTime utc = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, UTC_ZONE);
        return new EntsoeDate(utc);
    }

    /**
     * Date-only ENTSO-E String representation of the utc date object
     * @return Date-only ENTSO-E String representation of the utc date object
     */
    public String getEntsoeDate() {
        StringBuilder res = new StringBuilder();
        int year = utcDate.get(ChronoField.YEAR);
        int month = utcDate.get(ChronoField.MONTH_OF_YEAR);
        int day = utcDate.get(ChronoField.DAY_OF_MONTH);
        res.append(year).append(padIntegerWithZeros(month, 2)).append(padIntegerWithZeros(day, 2));
        return res.toString();
    }

    /**
     * Date-time ENTSO-E String representation of the utc date object
     * @return Date-time ENTSO-E String representation of the utc date object
     */
    public String getEntsoeDateTime() {
        StringBuilder res = new StringBuilder(getEntsoeDate());
        int hour = utcDate.get(ChronoField.HOUR_OF_DAY);
        int minute = utcDate.get(ChronoField.MINUTE_OF_HOUR);
        res.append(padIntegerWithZeros(hour, 2)).append(padIntegerWithZeros(minute, 2));
        return res.toString();
    }

    public String toPrettyLocalString() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return utcDate.withZoneSameInstant(ZoneId.systemDefault()).format(dateTimeFormatter);
    }

    public String toPrettyUTCString() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
        return utcDate.format(dateTimeFormatter);
    }

    @Override
    public String toString() {
        return getEntsoeDateTime();
    }

    @Override
    public int compareTo(EntsoeDate o) {
        return this.utcDate.withFixedOffsetZone().compareTo(o.utcDate.withFixedOffsetZone());
    }

}
