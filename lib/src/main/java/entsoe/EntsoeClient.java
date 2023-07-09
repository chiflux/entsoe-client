package entsoe;

import com.google.common.util.concurrent.RateLimiter;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Access the ENTSO-E Transparency Platform.
 * To be able to use the library, an API TOKEN is required. See README.md
 * Please note that some constants make only sense for users in Germany.
 */
public class EntsoeClient implements EntsoeDefines {

    private static final Logger LOGGER = Logger.getLogger(EntsoeClient.class.getName());

    private static final String apiUrl = "https://web-api.tp.entsoe.eu/api"; // The URL of the ENTSOE API
    private static final String documentType = "A44"; // The day ahead prices document
    private static final String in_Domain = "10Y1001A1001A82H"; // DE_LU (Germany)
    private static final String out_Domain = in_Domain; // must be the same as in_Domain

    private final RateLimiter rateLimiter = RateLimiter.create(.2); // one permit per 5 seconds

    private final String entsoeSecurityToken; // required to get access

    public EntsoeClient(String entsoeSecurityToken) {
        if (entsoeSecurityToken==null) {
            throw new IllegalArgumentException("entsoeSecurityToken must not be null");
        }
        this.entsoeSecurityToken = entsoeSecurityToken;
    }

    public EntsoeClient() {
        String property = System.getProperty(ENTSOE_SECURITY_TOKEN);
        if (property==null) {
            property = System.getenv(ENTSOE_SECURITY_TOKEN);
        }
        if (property==null) {
            throw new IllegalStateException("ENTSOE_SECURITY_TOKEN must not be null");
        }
        this.entsoeSecurityToken = property;
    }

    /**
     * Returns a TimeSeries that is truncated at the specified cutOffDate.
     * See {@link #getTimeSeries(EntsoeDate, EntsoeResolution)}
     * @param entsoeDate The date to request the day ahead spot price data for
     * @param entsoeResolution The resolution of the data. For spot price plans in Germany {@link EntsoeResolution#PT60M} must be used.
     * @param cutOffDate The cutoff date. Data before this date will be removed.
     * @return A map that correlates timeslots and day ahead spot prices
     */
    public TreeMap<EntsoeDate, BigDecimal> getTimeSeries(EntsoeDate entsoeDate, EntsoeResolution entsoeResolution, ZonedDateTime cutOffDate) {
        TreeMap<EntsoeDate, BigDecimal> timeSeries = getTimeSeries(entsoeDate, entsoeResolution);
        if (cutOffDate != null) {
            TreeSet<EntsoeDate> entsoeDates = new TreeSet<>(timeSeries.keySet());
            for (EntsoeDate date : entsoeDates) {
                if (date.utcDate().isBefore(cutOffDate.minus(1, ChronoUnit.HOURS))) {
                    timeSeries.remove(date);
                }
            }
        }
        return timeSeries;
    }

    /**
     * Returns a TimeSeries that is truncated at the specified cutOffDate.
     * @param entsoeDate The date to request the day ahead spot price data for
     * @param entsoeResolution The resolution of the data. For spot price plans in Germany {@link EntsoeResolution#PT60M} must be used.
     * @return A map that correlates timeslots and day ahead spot prices
     */
    public TreeMap<EntsoeDate, BigDecimal> getTimeSeries(EntsoeDate entsoeDate, EntsoeResolution entsoeResolution) {
        TreeMap<EntsoeDate, BigDecimal> res = new TreeMap<>();
        String spotpriceDataRaw = getSpotpriceDataRaw(entsoeDate);
        if (spotpriceDataRaw == null) {
            return res;
        }
        DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
        try {
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            Document xmlDocument = builder.parse(new ByteArrayInputStream(spotpriceDataRaw.getBytes(StandardCharsets.UTF_8)));
            XPath xPath = XPathFactory.newInstance().newXPath();
            String expression = "/Publication_MarketDocument/TimeSeries/Period/resolution";
            NodeList nodeList = (NodeList) xPath.compile(expression).evaluate(xmlDocument, XPathConstants.NODESET);
            int length = nodeList.getLength();
            List<Node> nodes = new ArrayList<>();
            for (int i = 0; i < length; i++) {
                Node resoultionNode = nodeList.item(i);
                String resolution = resoultionNode.getTextContent();
                if (resolution.equals(entsoeResolution.name())) {
                    nodes.add(resoultionNode.getParentNode()); // Period
                }
            }
            for (Node node : nodes) {
                String start = null;
                NodeList childNodes = node.getChildNodes();
                for (int i = 0; i < childNodes.getLength(); i++) {
                    Node item = childNodes.item(i);
                    String nodeName = item.getNodeName();
                    if (nodeName.equals("timeInterval")) {
                        NodeList childNodes1 = item.getChildNodes();
                        for (int j = 0; j < childNodes1.getLength(); j++) {
                            Node item1 = childNodes1.item(j);
                            if (item1.getNodeName().equals("start")) {
                                start = item1.getTextContent();
                                LOGGER.fine("Found start of time interval: " + start);
                            }
                        }
                    } else if (nodeName.equals("Point") && start != null) {
                        NodeList childNodes1 = item.getChildNodes();
                        Integer position = null;
                        BigDecimal price = null;
                        for (int j = 0; j < childNodes1.getLength(); j++) {
                            Node item1 = childNodes1.item(j);
                            if (item1.getNodeName().equals("position")) {
                                String textContent = item1.getTextContent();
                                position = Integer.parseInt(textContent);
                            } else if (item1.getNodeName().equals("price.amount")) {
                                String textContent = item1.getTextContent();
                                price = new BigDecimal(textContent).divide(BigDecimal.TEN, RoundingMode.HALF_UP);
                            }
                        }
                        if (position != null && price!=null) {
                            ZonedDateTime zonedDateTime = ZonedDateTime.parse(start);
                            EntsoeDate entsoeStartDate = new EntsoeDate(zonedDateTime);
                            EntsoeDate priceDate = EntsoeDate.fromENTSOEDate(entsoeStartDate, position - 1, entsoeResolution);
                            LOGGER.finer("price mapping " + priceDate + "=" + price);
                            res.put(priceDate, price.setScale(2, RoundingMode.HALF_UP));
                        }
                    }
                }
            }
            return res;
        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException e) {
            throw new IllegalStateException(e);
        }
    }

    private final AtomicReference<String> XML_CACHE = new AtomicReference<>();

    /**
     * Returns the raw spot price data as XML String.
     * @param entsoeDateIn The date to request the day ahead spot price data for
     * @return the raw XML response of the ENTSO-E API
     */
    public String getSpotpriceDataRaw(EntsoeDate entsoeDateIn) {
        ZonedDateTime zonedDateTime = entsoeDateIn.utcDate().
                with(ChronoField.MINUTE_OF_HOUR, 0).
                with(ChronoField.HOUR_OF_DAY, 0);
        EntsoeDate entsoeDate = new EntsoeDate(zonedDateTime);
        boolean useCache = ZonedDateTime.now().withHour(12).withMinute(0).isAfter(ZonedDateTime.now());
        useCache = useCache | ZonedDateTime.now().withHour(15).withMinute(0).isBefore(ZonedDateTime.now());
        String cachedXMLValue = XML_CACHE.get();
        if (cachedXMLValue != null && useCache) {
            return cachedXMLValue;
        }
        rateLimiter.acquire();
        String requestURL = getRequestURL(new EntsoeDate(entsoeDate.utcDate()));
        LOGGER.log(Level.FINE, "GET " + requestURL);

        Request request = new Request.Builder().url(requestURL).build();
        OkHttpClient client = new OkHttpClient();
        try (Response response = client.newCall(request).execute()) {
            ResponseBody body = response.body();
            if (body != null) {
                String xmlPayload = body.string();
                XML_CACHE.set(xmlPayload);
                return xmlPayload;
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return null;
    }

    /**
     * Assemble the request URL for a specific date
     * @param entsoeDate The date to request the day ahead spot price data for
     * @return the URL to call
     */
    public String getRequestURL(EntsoeDate entsoeDate) {
        LOGGER.fine("ENTSOE securityToken=" + entsoeSecurityToken);
        ZonedDateTime utcDate = entsoeDate.utcDate();
        ZonedDateTime startDate = utcDate.with(ChronoField.HOUR_OF_DAY, 0).
                with(ChronoField.MINUTE_OF_HOUR, 0);
        ZonedDateTime endDate = utcDate.with(ChronoField.HOUR_OF_DAY, 23).
                with(ChronoField.MINUTE_OF_HOUR, 0);
        EntsoeDate entsoeDateStart = new EntsoeDate(startDate);
        EntsoeDate entsoeDateEnd = new EntsoeDate(endDate);
        return apiUrl + "?"
                + "securityToken=" + entsoeSecurityToken + "&"
                + "documentType=" + documentType + "&"
                + "in_Domain=" + in_Domain + "&"
                + "out_Domain=" + out_Domain + "&"
                + "periodStart=" + entsoeDateStart + "&"
                + "periodEnd=" + entsoeDateEnd;
    }


}
