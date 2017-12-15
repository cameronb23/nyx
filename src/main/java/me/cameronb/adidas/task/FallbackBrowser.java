package me.cameronb.adidas.task;

//import com.machinepublishers.jbrowserdriver.JBrowserDriver;
//import com.machinepublishers.jbrowserdriver.ProxyConfig;
//import com.machinepublishers.jbrowserdriver.Settings;
//import com.machinepublishers.jbrowserdriver.UserAgent;
import me.cameronb.adidas.Application;
import me.cameronb.adidas.proxy.Proxy;
import me.cameronb.adidas.serializable.Config;
import org.apache.http.client.CookieStore;
import org.apache.http.impl.client.BasicCookieStore;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.rmi.Remote;
import java.util.logging.Level;

/**
 * Created by Cameron on 12/14/2017.
 */
public class FallbackBrowser extends Thread {

    private RemoteWebDriver chromeDriver;
//    private JBrowserDriver driver;

    private final String url;
    private final DesiredCapabilities caps;
    private final ChromeOptions opts;
//    private final Settings driverSettings;
    private final CookieStore cookies;

    public FallbackBrowser(String url, BasicCookieStore cookieStore, Proxy proxy) {
        this.url = url;
        this.cookies = cookieStore;


        this.opts = new ChromeOptions();

        this.opts.addArguments(String.format("--user-agent=%s", Config.INSTANCE.getUserAgent()));

        this.caps = DesiredCapabilities.chrome();

        if(proxy != null) {
            org.openqa.selenium.Proxy prox = new org.openqa.selenium.Proxy();
            prox.setHttpProxy(String.format("%s:%s", proxy.getAddress(), proxy.getPort()));

            if(proxy.getUsername() != null && proxy.getPassword() != null) {
                prox.setSocksUsername(proxy.getUsername());
                prox.setSocksPassword(proxy.getPassword());
            }

            caps.setCapability(CapabilityType.PROXY, prox);
        }

//        Settings.Builder builder = Settings.builder()
//                .userAgent(new UserAgent(null, null, null, null, null, Config.INSTANCE.getUserAgent()))
//                .loggerLevel(Level.SEVERE)
//                .logTrace(true)
//                .logWarnings(false)
//                .headless(false);
//
//        if(proxy != null) {
//            ProxyConfig proxyConfig;
//
//            if(proxy.getUsername() != null) {
//                proxyConfig = new ProxyConfig(ProxyConfig.Type.HTTP, proxy.getAddress(), proxy.getPort(), proxy.getUsername(), proxy.getPassword());
//            } else {
//                proxyConfig = new ProxyConfig(ProxyConfig.Type.HTTP, proxy.getAddress(), proxy.getPort());
//            }
//
//            builder.proxy(proxyConfig);
//        }
//
//        this.driverSettings = builder.build();
    }

    @Override
    public void run() {
        // start driver
//        this.driver = new JBrowserDriver(this.driverSettings);

        this.caps.setCapability(ChromeOptions.CAPABILITY, this.opts);

        this.chromeDriver = new RemoteWebDriver(Application.getDriverService().getUrl(), this.caps);

        this.chromeDriver.get(this.url);

        this.cookies.getCookies().stream().forEachOrdered(c -> {
            Cookie cookie = new Cookie.Builder(c.getName(), c.getValue())
                    .domain(c.getDomain())
                    .expiresOn(c.getExpiryDate())
                    .path(c.getPath())
                    .isSecure(c.isSecure())
                    .build();
            this.chromeDriver.manage().addCookie(cookie);
        });

        this.chromeDriver.get(this.url);
    }

}
