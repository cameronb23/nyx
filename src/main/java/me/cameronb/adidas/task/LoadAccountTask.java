package me.cameronb.adidas.task;

import me.cameronb.adidas.AdidasAccount;
import me.cameronb.adidas.Region;
import me.cameronb.adidas.proxy.Proxy;
import me.cameronb.adidas.util.Accounts;
import me.cameronb.adidas.util.ClientUtil;
import me.cameronb.adidas.util.Console;
import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by Cameron on 12/12/2017.
 */
public class LoadAccountTask implements Callable<Boolean> {

    private Region region;
    private String basketUrl;
    private BasicCookieStore cookieStore;
    private Proxy proxy;

    public LoadAccountTask(Region region, String basketUrl, BasicCookieStore cookieStore, Proxy proxy) {
        this.region = region;
        this.basketUrl = basketUrl;
        this.cookieStore = cookieStore;
        this.proxy = proxy;
    }

    @Override
    public Boolean call() {
        CloseableHttpClient client = ClientUtil.createClient(this.region, this.cookieStore, this.proxy, false);
        try {
            CloseableHttpResponse response = client.execute(new HttpGet(this.basketUrl));

            if(response.getStatusLine().getStatusCode() == 200) {
                response.close();
                return true;
            }

            Console.logError("Error loading account: " + response.getStatusLine().getStatusCode(), -1);
            response.close();
            return false;
        } catch (IOException ex) {
            Console.logError("Unable to fetch basket", -1);
            return false;
        }
    }
}