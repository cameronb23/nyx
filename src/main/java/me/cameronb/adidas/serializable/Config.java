package me.cameronb.adidas.serializable;

import lombok.Getter;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.io.File;

/**
 * Created by Cameron on 12/13/2017.
 */
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Config {

    public static JAXBContext CONTEXT;
    public static Config INSTANCE;

    // define configuration properties and default values

    @Getter
    private String email = "enter license email here";

    @Getter
    private String license = "enter license key here";

    @Getter
    private String discordHook = "";

    @Getter
    @XmlElement(type = Long.class)
    private long requestDelay = 60000;

    @Getter
    private String chromeDriverPath = "~/Downloads/chromedriver";

    @Getter
    private String firefoxDriverPath = "C:/Program Files/Mozilla Firefox/firefox.exe";

    @Getter
    private String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36";

    @Getter @XmlElement(name = "testMode")
    private boolean isTestMode = false;

    // so it doesn't get initialized
    public Config() {}

    public static Config loadConfig(File file) {
        try {
            return (Config) CONTEXT.createUnmarshaller().unmarshal(file);
        } catch (JAXBException ex) {
            throw new IllegalArgumentException("Could not load configuration from " + file + ".", ex);
        }
    }

}
