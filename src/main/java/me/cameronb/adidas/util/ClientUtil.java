package me.cameronb.adidas.util;

import me.cameronb.adidas.Region;
import me.cameronb.adidas.proxy.Proxy;
import me.cameronb.adidas.serializable.Config;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CookieStore;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.*;
import org.apache.http.message.BasicHeader;

import java.util.Arrays;
import java.util.List;

/**
 * Created by Cameron on 12/12/2017.
 */
public class ClientUtil {

    public static CloseableHttpClient createClient(Region region, BasicCookieStore cookieStore, Proxy proxy, boolean json) {
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

        List<Header> defaultHeaders = Arrays.asList(
                new BasicHeader(HttpHeaders.ACCEPT, json ? "application/json" : "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8"),
                new BasicHeader(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate"),
                new BasicHeader(HttpHeaders.ACCEPT_LANGUAGE, "en-US,en;q=0.9"),
                new BasicHeader(HttpHeaders.HOST, String.format("www.adidas.%s", region.getTld())),
                new BasicHeader("Origin", String.format("http://www.adidas.%s", region.getTld())),
                new BasicHeader(HttpHeaders.REFERER, String.format("http://www.adidas.%s/yeezy", region.getTld())),
                new BasicHeader(HttpHeaders.CONNECTION, "keep-alive"),
                new BasicHeader("DNT", "1"),
                new BasicHeader(HttpHeaders.CACHE_CONTROL, "max-age=0")
        );

        clientBuilder
                .setUserAgent(Config.INSTANCE.getUserAgent())
                .setDefaultHeaders(defaultHeaders)
                .setDefaultRequestConfig(configBuilder.build())
                .setDefaultCookieStore(cookieStore)
                .setRedirectStrategy(new LaxRedirectStrategy());

        if(proxy != null && proxy.getUsername() != null) {
            CredentialsProvider credsProvider = new BasicCredentialsProvider();
            credsProvider.setCredentials(
                    new AuthScope(proxy.getAddress(), proxy.getPort()),
                    new UsernamePasswordCredentials(proxy.getUsername(), proxy.getPassword())
            );
            clientBuilder.setDefaultCredentialsProvider(credsProvider);
        }

        return clientBuilder.build();
    }

}
