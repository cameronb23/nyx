package me.cameronb.adidas.util;

import me.cameronb.adidas.Region;
import me.cameronb.adidas.proxy.Proxy;
import me.cameronb.adidas.serializable.Config;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.*;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.message.BasicHeader;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

/**
 * Created by Cameron on 12/12/2017.
 */
public class ClientUtil {

    public static Cookie createCookie(String cookieData, String url) {
        String[] cookieString = cookieData.split("=");
        BasicClientCookie cookie = new BasicClientCookie(cookieString[0], cookieString[1]);
        String domain;

        if(url.contains("www.")) {
            domain = url.split("www.")[1].split("/")[0];
        } else {
            domain = url.split("^https?://")[1].split("/")[0];
        }

        cookie.setDomain(domain);
        return cookie;
    }

    public static String getCookieString(CookieStore cookies) {
        if(cookies.getCookies().size() < 1) {
            return null;
        }

        StringBuilder cookieBuilder = new StringBuilder();

        cookies.getCookies().stream().forEachOrdered(c -> cookieBuilder.append(c.getName() + "=" + c.getValue() + "; "));

        // remove ending ;
        cookieBuilder.setLength(cookieBuilder.length() - 2);

        return cookieBuilder.toString();
    }

    public static CloseableHttpClient createClient(String url, Region region, BasicCookieStore cookieStore, Proxy proxy, boolean json) {
        HttpClientBuilder clientBuilder = HttpClientBuilder.create();

        RequestConfig.Builder configBuilder = RequestConfig.custom()
                .setSocketTimeout(30000)
                .setConnectionRequestTimeout(30000)
                .setConnectTimeout(30000)
                .setCookieSpec(CookieSpecs.STANDARD)
                .setMaxRedirects(100)
                .setSocketTimeout(30000);

        if(proxy != null) {
            configBuilder.setProxy(new HttpHost(proxy.getAddress(), proxy.getPort()));
        }

        // TODO: proxy authentication

        String host = url.contains("adidas") ? String.format("www.adidas.%s", region.getTld()) : "www.cartchefs.co.uk";
        String origin = "http://" + host;
        String referer = url.contains("adidas") ? String.format("http://www.adidas.%s/yeezy", region.getTld()) : "http://www.cartchefs.co.uk/splash";

        List<Header> defaultHeaders = Arrays.asList(
                new BasicHeader(HttpHeaders.ACCEPT, json ? "application/json" : "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"),
                new BasicHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate"),
                new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9"),
                new BasicHeader(HttpHeaders.HOST, host),
                new BasicHeader("Origin", origin),
                new BasicHeader(HttpHeaders.REFERER, referer),
                new BasicHeader(HttpHeaders.CONNECTION, "keep-alive"),
                new BasicHeader("DNT", "1"),
                new BasicHeader(HttpHeaders.CACHE_CONTROL, "max-age=0")
        );

        clientBuilder
                .setUserAgent(Config.INSTANCE.getUserAgent())
                .setDefaultRequestConfig(configBuilder.build())
                .setDefaultCookieStore(cookieStore)
                .setRedirectStrategy(new LaxRedirectStrategy());

        if(proxy != null && proxy.getUsername() != null) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(proxy.getAddress(), proxy.getPort()),
//                    AuthScope.ANY,
                    new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword())
            );

            clientBuilder.setDefaultCredentialsProvider(credsProvider);
        }

        clientBuilder.setDefaultHeaders(defaultHeaders);

        return clientBuilder.build();
    }

}
