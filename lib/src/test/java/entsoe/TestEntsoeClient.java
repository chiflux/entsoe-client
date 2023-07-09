package entsoe;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

class TestEntsoeClient {

    private static String TOKEN;
    private final ZonedDateTime NEW_YEAR_1900 = ZonedDateTime.of(1900,1,1,0,0,0,0, EntsoeDate.UTC_ZONE);
    private final ZonedDateTime NEW_YEAR_2023 = ZonedDateTime.of(2023,1,1,0,0,0,0, EntsoeDate.UTC_ZONE);
    private final ZonedDateTime NEW_YEAR_2023_9am = ZonedDateTime.of(2023,1,1,9,0,0,0, EntsoeDate.UTC_ZONE);

    @BeforeAll
    static void init() {
        TOKEN = System.getProperty(EntsoeDefines.ENTSOE_SECURITY_TOKEN);
        if (TOKEN==null) {
            TOKEN = System.getenv(EntsoeDefines.ENTSOE_SECURITY_TOKEN);
        }
    }

    @Test
    void testEntsoeDate() {
        ZonedDateTime zd1 = ZonedDateTime.of(2023,1,1,0,0,0,0, EntsoeDate.UTC_ZONE);
        EntsoeDate entsoeDate1 = EntsoeDate.fromENTSOEDateString("202301010000");
        Assertions.assertEquals(new EntsoeDate(zd1), entsoeDate1);
        ZonedDateTime zd2 = ZonedDateTime.of(2023,1,1,8,15,0,0, EntsoeDate.UTC_ZONE);
        EntsoeDate entsoeDate2 = EntsoeDate.fromENTSOEDateString("202301010815");
        Assertions.assertEquals(new EntsoeDate(zd2), entsoeDate2);
        List<EntsoeDate> list = Arrays.asList(entsoeDate1, entsoeDate2);
        Assertions.assertTrue(list.contains(new EntsoeDate(zd1)));
        Assertions.assertTrue(list.contains(new EntsoeDate(zd2)));
        Map<EntsoeDate, Integer> map = new HashMap<>();
        map.put(entsoeDate1, 1);
        map.put(entsoeDate2, 2);
        Assertions.assertTrue(map.containsKey(new EntsoeDate(zd1)));
        Assertions.assertTrue(map.containsKey(new EntsoeDate(zd2)));
    }

    @Test
    void testContructors() {
        System.out.println("Testing constructors");
        try {
            new EntsoeClient(null);
            throw new RuntimeException("expected exception");
        } catch (IllegalStateException | IllegalArgumentException e) {
            System.out.println("Exception is expected (1)");
        }
        if (System.getProperty(EntsoeDefines.ENTSOE_SECURITY_TOKEN)==null && System.getenv(EntsoeDefines.ENTSOE_SECURITY_TOKEN)==null) {
            try {
                new EntsoeClient();
                throw new RuntimeException("expected exception");
            } catch (IllegalStateException | IllegalArgumentException e) {
                System.out.println("Exception is expected (2)");
            }
        }
    }

    @Test
    void testInvalidDate() {
        if (TOKEN==null) {
            return; // skipTest
        }
        System.out.println("Testing wrong Date");
        EntsoeClient entsoeClient = new EntsoeClient(TOKEN);
        EntsoeDate entsoeDate = new EntsoeDate(NEW_YEAR_1900);
        String requestURL = entsoeClient.getRequestURL(entsoeDate);
        Assertions.assertEquals("https://web-api.tp.entsoe.eu/api?securityToken=b54136fa-b5e0-4f25-8b0b-6fc6ff3a4cce&documentType=A44&in_Domain=10Y1001A1001A82H&out_Domain=10Y1001A1001A82H&periodStart=190001010000&periodEnd=190001012300", requestURL);
        String spotpriceDataRaw = entsoeClient.getSpotpriceDataRaw(entsoeDate);
        System.out.println(spotpriceDataRaw);
        Assertions.assertNotNull(spotpriceDataRaw);
        Assertions.assertTrue(spotpriceDataRaw.contains("<code>999</code>"));
    }

    @Test
    void testGetPricesForNextDay() {
        if (TOKEN==null) {
            return; // skipTest
        }
        System.out.println("getSpotPrices for next day");
        EntsoeClient entsoeClient = new EntsoeClient(TOKEN);
        ZonedDateTime tomorrow = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).plusDays(1);
        EntsoeDate entsoeDate = new EntsoeDate(tomorrow);
        TreeMap<EntsoeDate, BigDecimal> timeSeries = entsoeClient.getTimeSeries(entsoeDate, EntsoeResolution.PT60M);
        if (timeSeries.isEmpty()) {
            System.out.println("spot prices for " + entsoeDate.utcDate() + " are no available yet. Try again sometime after 2pm.");
        } else {
            System.out.println("spot prices for " + entsoeDate.utcDate() + ": " + timeSeries);
        }
    }

    @Test
    void testGetRaw() {
        if (TOKEN==null) {
            return; // skipTest
        }
        System.out.println("Testing raw GET request");
        EntsoeClient entsoeClient = new EntsoeClient(TOKEN);
        EntsoeDate entsoeDate = new EntsoeDate(NEW_YEAR_2023);
        String requestURL = entsoeClient.getRequestURL(entsoeDate);
        Assertions.assertEquals("https://web-api.tp.entsoe.eu/api?securityToken=b54136fa-b5e0-4f25-8b0b-6fc6ff3a4cce&documentType=A44&in_Domain=10Y1001A1001A82H&out_Domain=10Y1001A1001A82H&periodStart=202301010000&periodEnd=202301012300", requestURL);
        String spotpriceDataRaw = entsoeClient.getSpotpriceDataRaw(entsoeDate);
        System.out.println(spotpriceDataRaw);
        Assertions.assertNotNull(spotpriceDataRaw);
        Assertions.assertEquals(13704, spotpriceDataRaw.length());
        Assertions.assertFalse(spotpriceDataRaw.contains("<code>999</code>"));
    }

    @Test
    void testGetPT60M() {
        if (TOKEN==null) {
            return; // skipTest
        }
        EntsoeResolution resolution = EntsoeResolution.PT60M;
        System.out.println("Testing GET request with " + resolution.name());
        EntsoeClient entsoeClient = new EntsoeClient(TOKEN);
        EntsoeDate entsoeDate = new EntsoeDate(NEW_YEAR_2023);
        String requestURL = entsoeClient.getRequestURL(entsoeDate);
        Assertions.assertEquals("https://web-api.tp.entsoe.eu/api?securityToken=b54136fa-b5e0-4f25-8b0b-6fc6ff3a4cce&documentType=A44&in_Domain=10Y1001A1001A82H&out_Domain=10Y1001A1001A82H&periodStart=202301010000&periodEnd=202301012300", requestURL);
        TreeMap<EntsoeDate, BigDecimal> timeSeries = entsoeClient.getTimeSeries(entsoeDate, resolution);
        Assertions.assertNotNull(timeSeries);
        Assertions.assertEquals(24, timeSeries.size());
        TreeMap<EntsoeDate, BigDecimal> timeSeries2 = entsoeClient.getTimeSeries(entsoeDate, resolution, NEW_YEAR_2023_9am);
        Assertions.assertNotNull(timeSeries2);
        Assertions.assertEquals(24-9, timeSeries2.size());
    }

    @Test
    void testGetPT15M() {
        if (TOKEN==null) {
            return; // skipTest
        }
        EntsoeResolution resolution = EntsoeResolution.PT15M;
        System.out.println("Testing GET request with " + resolution.name());
        EntsoeClient entsoeClient = new EntsoeClient(TOKEN);
        EntsoeDate entsoeDate = new EntsoeDate(NEW_YEAR_2023);
        String requestURL = entsoeClient.getRequestURL(entsoeDate);
        Assertions.assertEquals("https://web-api.tp.entsoe.eu/api?securityToken=b54136fa-b5e0-4f25-8b0b-6fc6ff3a4cce&documentType=A44&in_Domain=10Y1001A1001A82H&out_Domain=10Y1001A1001A82H&periodStart=202301010000&periodEnd=202301012300", requestURL);
        TreeMap<EntsoeDate, BigDecimal> timeSeries = entsoeClient.getTimeSeries(entsoeDate, resolution);
        Assertions.assertNotNull(timeSeries);
        Assertions.assertEquals(96, timeSeries.size());
        TreeMap<EntsoeDate, BigDecimal> timeSeries2 = entsoeClient.getTimeSeries(entsoeDate, resolution, NEW_YEAR_2023_9am);
        Assertions.assertNotNull(timeSeries2);
        System.out.println(timeSeries2);
        Assertions.assertEquals(96-9*4, timeSeries2.size());
        System.out.println(timeSeries2.keySet());
        EntsoeDate entsoeDate1 = new EntsoeDate(ZonedDateTime.of(2023, 1, 1, 8, 15
                , 0, 0, EntsoeDate.UTC_ZONE));

        Assertions.assertTrue(timeSeries2.containsKey(entsoeDate1));
        Assertions.assertTrue(timeSeries2.containsKey(EntsoeDate.fromENTSOEDateString("202301012245")));
    }

    @Test
    void testGrossPrice() {
        System.out.println("Testing gross price calculation");
        EntsoeClient entsoeClient = new EntsoeClient(TOKEN);
        EntsoeDate entsoeDate = new EntsoeDate(NEW_YEAR_2023);
        TreeMap<EntsoeDate, BigDecimal> timeSeries = entsoeClient.getTimeSeries(entsoeDate, EntsoeResolution.PT60M);
        BigDecimal bigDecimal = timeSeries.get(EntsoeDate.fromENTSOEDateString("202301010800"));
        Assertions.assertEquals(BigDecimal.valueOf(-0.11), bigDecimal);
    }

    @Test
    void testRequestLimiter() throws InterruptedException {
        ApiRateLimiter apiRateLimiter = new ApiRateLimiter(
                2, Duration.of(5, ChronoUnit.SECONDS),
                Duration.of(500, ChronoUnit.MILLIS));
        Instant start = Instant.now();
        AtomicLong counter = new AtomicLong(0);
        while(start.plus(Duration.of(20, ChronoUnit.SECONDS)).isAfter(Instant.now())) {
            System.out.println("wait for permit");
            apiRateLimiter.acquireWait();
            System.out.println("permit received " + counter.incrementAndGet());
            Thread.sleep(100);
        }
        Assertions.assertTrue(counter.get()<12);
    }

}