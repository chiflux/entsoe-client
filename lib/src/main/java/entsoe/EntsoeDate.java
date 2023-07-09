package entsoe;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;

import static entsoe.Utils.padIntegerWithZeros;

public record EntsoeDate(ZonedDateTime utcDate) implements Comparable<EntsoeDate> {
    public static final ZoneId UTC_ZONE = ZoneId.of("UTC");

    public EntsoeDate(ZonedDateTime utcDate) {
        this.utcDate = utcDate.withSecond(0).withNano(0);
    }

    public static EntsoeDate fromENTSOEDate(EntsoeDate entsoeDate, int position, EntsoeResolution entsoeResolution) {
        int plusHours = position;
        int plusMinutes = 0;
        if (entsoeResolution==EntsoeResolution.PT15M) {
            plusHours = position / 4;
            plusMinutes = (position % 4) * 15;
        }
        return new EntsoeDate(entsoeDate.utcDate.plusHours(plusHours).plusMinutes(plusMinutes));
    }

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
        return this.utcDate.withFixedOffsetZone().compareTo(o.utcDate.withFixedOffsetZone());
    }

}
