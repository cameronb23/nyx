package me.cameronb.adidas.task;

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

        this.client = ClientUtil.createClient(this.options.getRegion(), this.cookieStore, this.proxy, false);
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

            boolean passed = refreshPage();

            if(passed) {
                Console.logSuccess("Passed splash!", this.id);
                this.cartTask = new CartTask(this.id, this.options, this.cookieStore, this.proxy, this.clientId);
                this.cartTask.start();

                // RELEASE RESOURCE.
                this.client.close();
                return true;
            }

            sleep(Config.INSTANCE.getRequestDelay());
            return this.loop();
        } catch (IOException | InterruptedException ex) {
            ex.printStackTrace();
            return this.loop();
        }
    }

    private boolean refreshPage() throws IOException {

        String url;

        if(Config.INSTANCE.isTestMode()) {
            url = "http://www.cartchefs.co.uk/splash_test";
        } else {
            url = String.format("http://www.adidas.com/%s/apps/yeezy/", this.options.getRegion().getMicroSite());
        }

        CloseableHttpResponse response = this.client.execute(new HttpGet(url));
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

            Console.logSuccess("ClientID: " + clientId, this.id);
            Console.logSuccess("Sitekey: " + sitekey, this.id);

            this.clientId = clientId;

            return true;
        }

        Console.log("Still on splash...", this.id);
        return false;
    }

}
