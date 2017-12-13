package me.cameronb.adidas.task;

import me.cameronb.adidas.Region;
import me.cameronb.adidas.proxy.Proxy;
import me.cameronb.adidas.util.ClientUtil;
import me.cameronb.adidas.util.Console;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Cameron on 12/11/2017.
 */
public class RequestTask extends Thread {

    private final int id;
    private final Region region;
    private final String pid;
    private final double size;
    private CloseableHttpClient client;
    private BasicCookieStore cookieStore;
    private CartTask cartTask;
    private Proxy proxy;

    private AtomicBoolean running = new AtomicBoolean(true);

    public RequestTask(int id, Region region, String pid, double size, Proxy proxy) {
        this.id = id;
        this.region = region;
        this.pid = pid;
        this.size = size;
        this.cookieStore = new BasicCookieStore();
        this.proxy = proxy;

        this.client = ClientUtil.createClient(this.region, this.cookieStore, this.proxy, false);
    }

    @Override
    public void run() {
/*        try {
            sleep(300 * this.id);
        } catch(InterruptedException ex) {}*/

        this.loop();
    }

    private boolean loop() {
        try {
            if(!this.running.get()) {
                sleep(5000);
                return this.loop();
            }

            boolean passed = refreshPage();

            if(passed) {
                Console.logSuccess("Passed splash!", this.id);
                this.cartTask = new CartTask(this.id, this.region, this.pid, this.size, this.cookieStore, this.proxy);
                this.cartTask.start();

                // RELEASE RESOURCE.
                this.client.close();
                return true;
            }

            sleep(5000);
            return this.loop();
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            return this.loop();
        }
    }

    private boolean refreshPage() throws IOException {
//        CloseableHttpResponse response = this.client.execute(new HttpGet(String.format("http://www.adidas.com/%s/apps/yeezy/", this.region.getMicroSite())));
        CloseableHttpResponse response = this.client.execute(new HttpGet("http://www.cartchefs.co.uk/splash_test"));
        HttpEntity responseEntity = response.getEntity();
        InputStream dataStream = responseEntity.getContent();

        String parsed = IOUtils.toString(dataStream, "UTF-8");

        // release resources
        EntityUtils.consume(responseEntity);
        dataStream.close();
        response.close();

        parsed = parsed.trim();

        if(parsed.contains("<!-- ERR_CACHE_ACCESS_DENIED -->")) {
            // proxy connection failed
            return false;
        }

        if(parsed.contains("captchaform")) {
            // passed splash

            String clientId = null;
            String sitekey = null;

            // find client id
            if(parsed.contains("?clientId=")) {
                int startIndex = parsed.indexOf("?clientId=") + 10;
                String splitString = parsed.substring(startIndex);
                clientId = splitString.split("\"")[0];
            }

//            if(parsed.contains("CAPTCHA_KEY")) {
//                int startIndex = parsed.indexOf("CAPTCHA_KEY") + 11;
//                String splitString = parsed.substring(startIndex);
//                int captchaStartIndex = splitString.indexOf("'") + 1;
//                String captchaSplit = splitString.substring(captchaStartIndex);
//                sitekey = captchaSplit.split("'")[0];
//            }

            System.out.println("Client ID: " + clientId);
            System.out.println("Sitekey: " + sitekey);

            return true;
        }

        Console.log("Still on splash...", this.id);
        return false;
    }

}
