package entsoe;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.TreeMap;

class TestEntsoeClientTest {

    private static String TOKEN;
    private final ZonedDateTime NEW_YEAR_2023 = ZonedDateTime.of(2023,1,1,0,0,0,0, ZoneId.of("UTC"));
    private final ZonedDateTime NEW_YEAR_2023_9am = ZonedDateTime.of(2023,1,1,9,0,0,0, ZoneId.of("UTC"));

    @BeforeAll
    static void init() {
        TOKEN = System.getProperty(Defines.ENTSOE_SECURITY_TOKEN);
        if (TOKEN==null) {
            TOKEN = System.getenv(Defines.ENTSOE_SECURITY_TOKEN);
        }
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
        if (System.getProperty(Defines.ENTSOE_SECURITY_TOKEN)==null && System.getenv(Defines.ENTSOE_SECURITY_TOKEN)==null) {
            try {
                new EntsoeClient();
                throw new RuntimeException("expected exception");
            } catch (IllegalStateException | IllegalArgumentException e) {
                System.out.println("Exception is expected (2)");
            }
        }
    }

    @Test
    void testGet() {
        if (TOKEN==null) {
            return; // skipTest
        }
        System.out.println("Testing GET request");
        EntsoeClient entsoeClient = new EntsoeClient(TOKEN);
        EntsoeDate entsoeDate = new EntsoeDate(NEW_YEAR_2023);
        String requestURL = entsoeClient.getRequestURL(entsoeDate);
        Assertions.assertEquals("https://web-api.tp.entsoe.eu/api?securityToken=b54136fa-b5e0-4f25-8b0b-6fc6ff3a4cce&documentType=A44&in_Domain=10Y1001A1001A82H&out_Domain=10Y1001A1001A82H&periodStart=202301010000&periodEnd=202301012300", requestURL);
        String spotpriceDataRaw = entsoeClient.getSpotpriceDataRaw(entsoeDate);
        Assertions.assertNotNull(spotpriceDataRaw);
        Assertions.assertEquals(13704, spotpriceDataRaw.length());
        Assertions.assertTrue(spotpriceDataRaw.contains("PT60"));
        TreeMap<EntsoeDate, BigDecimal> timeSeries = entsoeClient.getTimeSeries(entsoeDate);
        Assertions.assertNotNull(timeSeries);
        Assertions.assertEquals(24, timeSeries.size());
        TreeMap<EntsoeDate, BigDecimal> timeSeries2 = entsoeClient.getTimeSeries(entsoeDate, NEW_YEAR_2023_9am);
        Assertions.assertNotNull(timeSeries2);
        Assertions.assertEquals(24-9, timeSeries2.size());
    }

}