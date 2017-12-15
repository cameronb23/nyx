package me.cameronb.adidas.task;

import com.google.common.net.HttpHeaders;
import me.cameronb.adidas.Region;
import me.cameronb.adidas.proxy.Proxy;
import me.cameronb.adidas.serializable.Config;
import me.cameronb.adidas.serializable.TaskData;
import me.cameronb.adidas.util.ClientUtil;
import me.cameronb.adidas.util.Console;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Cameron on 12/11/2017.
 */
public class RequestTask extends Thread {

    private final int id;
    private final String url;
    private final TaskData options;

    private CloseableHttpClient client;
    private BasicCookieStore cookieStore;
    private CartTask cartTask = null;
    private Proxy proxy;

    private AtomicBoolean running = new AtomicBoolean(true);
    private String clientId = null;

    public RequestTask(int id, TaskData data, Proxy proxy) {
        this.id = id;
        this.options = data;
        this.cookieStore = new BasicCookieStore();
        this.proxy = proxy;

        if(Config.INSTANCE.isTestMode()) {
            this.url = "http://www.cartchefs.co.uk/splash";
        } else {
            this.url = String.format("http://www.adidas.com/%s/apps/yeezy/", this.options.getRegion().getMicroSite());
        }

        this.client = ClientUtil.createClient(this.url, this.options.getRegion(), this.cookieStore, this.proxy, false);
    }

    public void shutdown() throws IOException {
        this.running.set(false);
        this.client.close();
        if (this.cartTask != null && this.cartTask.getRunning().get()) {
            this.cartTask.shutdown();
        }
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

            sleep(Config.INSTANCE.getRequestDelay());

            boolean passed = refreshPage();

            if(passed) {
                Console.logSuccess("Passed splash!", this.id);
//                this.cartTask = new CartTask(this.id, this.options, this.cookieStore, this.proxy, this.clientId);
//                this.cartTask.start();

                new FallbackBrowser(this.url, this.cookieStore, this.proxy).start();

                // RELEASE RESOURCE.
                this.client.close();
                return true;
            }

            return this.loop();
        } catch (IOException | InterruptedException ex) {
            Console.logError("Error loading splash page: " + ex.getMessage(), this.id);
            return this.loop();
        }
    }

    private boolean refreshPage() throws IOException {
        HttpGet request = new HttpGet(this.url);

        String cookieHeader = ClientUtil.getCookieString(this.cookieStore);

        if(cookieHeader != null) {
            request.setHeader(HttpHeaders.COOKIE, cookieHeader);
        }

        CloseableHttpResponse response = this.client.execute(request);

        HttpEntity responseEntity = response.getEntity();
        InputStream dataStream = responseEntity.getContent();

        String parsed = IOUtils.toString(dataStream, "UTF-8");

        // detect JS cookie code
        String check = "document.cookie=\"";
        int checkIndex = parsed.indexOf(check);
        while(checkIndex > 0) {
            String cookie = parsed.substring((checkIndex + check.length())).split("\"")[0];
            this.cookieStore.addCookie(ClientUtil.createCookie(cookie, this.url));
            checkIndex = parsed.indexOf(check, checkIndex + 1);
        }

        // release resources
        EntityUtils.consume(responseEntity);
        dataStream.close();
        response.close();

        parsed = parsed.trim();

        if(parsed.contains("<!-- ERR_CACHE_ACCESS_DENIED -->")) {
            // proxy connection failed
            Console.logError("Proxy denied our access...", this.id);
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

            Console.logSuccess("ClientID: " + clientId, this.id);
            Console.logSuccess("Sitekey: " + sitekey, this.id);

            this.clientId = clientId;

            return true;
        }

        Console.log("Still on splash...", this.id);
        return false;
    }

}
