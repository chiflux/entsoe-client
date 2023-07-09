package entsoe;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

import static entsoe.Utils.padIntegerWithZeros;

public record EntsoeDate(ZonedDateTime utcDate) implements Comparable<EntsoeDate> {

    public EntsoeDate(ZonedDateTime utcDate) {
        this.utcDate = utcDate.with(ChronoField.NANO_OF_SECOND, 0);
    }

    public static EntsoeDate fromENTSOEDate(EntsoeDate entsoeDate, int position) {
        return new EntsoeDate(entsoeDate.utcDate.plusHours(position));
    }

    public static EntsoeDate fromENTSOEDateString(String entsoeFormat) {
        int year = Integer.parseInt(entsoeFormat.substring(0, 4));
        int month = Integer.parseInt(entsoeFormat.substring(4, 6));
        int day = Integer.parseInt(entsoeFormat.substring(6, 8));
        LocalDate localDate = LocalDate.of(year, month, day);
        LocalTime localTime;
        if (entsoeFormat.length() > 8) {
            int hour = Integer.parseInt(entsoeFormat.substring(8, 10));
            int minute = Integer.parseInt(entsoeFormat.substring(10, 12));
            localTime = LocalTime.of(hour, minute);
        } else {
            localTime = LocalTime.of(0, 0);
        }
        ZonedDateTime utc = ZonedDateTime.of(localDate, localTime, ZoneId.of("UTC"));
        return new EntsoeDate(utc);
    }

    public String getEntsoeDate() {
        StringBuilder res = new StringBuilder();
        int year = utcDate.get(ChronoField.YEAR);
        int month = utcDate.get(ChronoField.MONTH_OF_YEAR);
        int day = utcDate.get(ChronoField.DAY_OF_MONTH);
        res.append(year).append(padIntegerWithZeros(month, 2)).append(padIntegerWithZeros(day, 2));
        return res.toString();
    }

    public String getEntsoeDateTime() {
        StringBuilder res = new StringBuilder(getEntsoeDate());
        int hour = utcDate.get(ChronoField.HOUR_OF_DAY);
        int minute = utcDate.get(ChronoField.MINUTE_OF_HOUR);
        res.append(padIntegerWithZeros(hour, 2)).append(padIntegerWithZeros(minute, 2));
        return res.toString();
    }

    @Override
    public String toString() {
        return getEntsoeDateTime();
    }

    @Override
    public int compareTo(EntsoeDate o) {
        return this.utcDate.compareTo(o.utcDate);
    }

}
