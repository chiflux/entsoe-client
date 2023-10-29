# entsoe-client

This library provides a simplified API access via Java to Day-ahead Prices of
the ENTSO-E Transparency Platform RESTful API.

The Day-ahead Prices (aka spot price) data contains the price information that is 
relevant for the so-called dynamic power plans available in Germany.

To be able to use the API an API token must be requested from https://transparency.entsoe.eu.
The API token can be set either as constructor parameter or as
Java Property or ENV variable with this name: ENTSOE_SECURITY_TOKEN

## Requirements

- Java JDK 17 or newer
- Gradle 8.2 or newer
- API Token

## Building

Linux / macOS

    ./gradlew build

Windows

    .\gradlew.bat build

## Usage

### Maven 

    <dependency>
        <groupId>de.chiflux</groupId>
        <artifactId>entsoe-client</artifactId>
        <version>1.0.8</version>
    </dependency>

## Examples

### Get spot prices for the next day

        EntsoeClient entsoeClient = new EntsoeClient(TOKEN);
        ZonedDateTime tomorrow = ZonedDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).plusDays(1);
        EntsoeDate entsoeDate = new EntsoeDate(tomorrow);
        TreeMap<EntsoeDate, BigDecimal> timeSeries = entsoeClient.getTimeSeries(entsoeDate, EntsoeResolution.PT60M);
        if (timeSeries.isEmpty()) {
            System.out.println("spot prices for " + entsoeDate.utcDate() + " are no available yet. Try again sometime after 2pm.");
        } else {
            System.out.println("spot prices for " + entsoeDate.utcDate() + ": " + timeSeries);
        }

For further use cases see unit tests. 