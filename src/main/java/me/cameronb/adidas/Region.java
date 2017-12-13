package me.cameronb.adidas;

import lombok.Getter;

/**
 * Created by Cameron on 12/12/2017.
 */
public enum Region {

    US("com", "US", "us", "en_US"),
    GB("co.uk", "GB", "uk", "en_GB");

    @Getter private String tld;
    @Getter private String demandwareSite;
    @Getter private String microSite;
    @Getter private String locale;

    Region(String tld, String demandware, String microSite, String locale) {
        this.tld = tld;
        this.demandwareSite = demandware;
        this.microSite = microSite;
        this.locale = locale;
    }

    public static Region getRegion(String selector) {
        for(Region r : values()) {
            if(r.name().equalsIgnoreCase(selector) || r.demandwareSite.equalsIgnoreCase(selector) || r.microSite.equalsIgnoreCase(selector)) {
                return r;
            }
        }
        return null;
    }

}
