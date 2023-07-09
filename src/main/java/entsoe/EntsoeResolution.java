package entsoe;

/**
 * ENTSO-E resolutions for day ahead spot price data.
 */
public enum EntsoeResolution {

    /**
     * The 15-minute resolution. Returns data set in 15-minute resolution.
     * Note: This is usually not useful for dynamic power plans.
     */
    PT15M,

    /**
     * The 60-minute resolution. Returns data set in 60-minute resolution.
     */
    PT60M

}
