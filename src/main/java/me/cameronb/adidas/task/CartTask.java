package me.cameronb.adidas.task;

import com.google.common.net.HttpHeaders;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import me.cameronb.adidas.AdidasAccount;
import me.cameronb.adidas.Application;
import me.cameronb.adidas.Cart;
import me.cameronb.adidas.proxy.Proxy;
import me.cameronb.adidas.serializable.Config;
import me.cameronb.adidas.serializable.TaskData;
import me.cameronb.adidas.util.Accounts;
import me.cameronb.adidas.util.ClientUtil;
import me.cameronb.adidas.util.Console;
import me.cameronb.adidas.util.Sizing;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Cameron on 12/12/2017.
 */
public class CartTask extends Thread {

    private final int id;
    private final String url;
    private final TaskData options;
    private final int sizeCode;
    private final String clientId;
    private final CloseableHttpClient httpClient;
    private final BasicCookieStore cookieStore;
    private final Proxy proxy;

    @Getter
    private AtomicBoolean running = new AtomicBoolean(true);
    private int failures = 0;

    public CartTask(int id, TaskData data, BasicCookieStore cookieStore, Proxy proxy, String clientId) {
        this.id = id;
        this.clientId = clientId;
        this.options = data;
        this.sizeCode = Sizing.getSizeCode(this.options.getSize());
        this.cookieStore = cookieStore;
        this.proxy = proxy;

        this.url = String.format(
                "http://www.adidas.%s/on/demandware.store/Sites-adidas-%s-Site/%s/Cart-MiniAddProduct%s",
                this.options.getRegion().getTld(),
                this.options.getRegion().getDemandwareSite(),
                this.options.getRegion().getLocale(),
                this.clientId == null ? "" : "?clientId=" + this.clientId // if clientId is required
        );

        this.httpClient = ClientUtil.createClient(this.url, this.options.getRegion(), this.cookieStore, this.proxy, true);
    }

    public void shutdown() throws IOException {
        this.running.set(false);
        this.httpClient.close();
    }

    @Override
    public void run() {
        boolean success = this.addToCart();

        if (success) {
            Future<AdidasAccount> accountFuture = Application.getExecutor().submit(new CreateAccountTask(this.cookieStore, this.proxy));
            AdidasAccount account = null;
            String basketUrl = null;

            try {
                AdidasAccount accountCreated  = accountFuture.get();

                Optional<Cookie> basketUrlOptional = this.cookieStore.getCookies().stream().filter(c -> c.getName().equalsIgnoreCase("restoreBasketUrl")).findFirst();

                if(basketUrlOptional.isPresent()) {

                    Cookie basketCookie = basketUrlOptional.get();
                    String basketUrlEncoded = basketCookie.getValue();

                    String basketUrlDecoded = URLDecoder.decode(basketUrlEncoded, "UTF-8");
                    basketUrl = String.format("http://www.adidas.%s%s", this.options.getRegion().getTld(), basketUrlDecoded);

                    Future<Boolean> loadedFuture = Application.getExecutor().submit(new LoadAccountTask(this.options.getRegion(), basketUrl, this.cookieStore, null));

                    boolean loaded = loadedFuture.get();

                    if(loaded) {
                        account = accountCreated;
                        Console.logSuccess(String.format("Loaded account - %s:%s", account.getEmail(), account.getPassword()), this.id);
                    } else {
                        Console.logError("Error loading account.", this.id);
                    }
                }
            } catch(InterruptedException | ExecutionException ex) {
                ex.printStackTrace();
                Console.logError("Unable to create account - Thread interrupted", this.id);
            } catch(UnsupportedEncodingException ex) {
                ex.printStackTrace();
                Console.logError("Unable to load account - Couldn't parse cart data", this.id);
            }

            Optional<Cookie> sessionCookie = this.cookieStore.getCookies().stream().filter(c -> c.getName().startsWith("dwanony")).findFirst();

            Cart c = new Cart(
                    this.options.getPid(),
                    this.options.getSize(),
                    null,
                    null,
                    null,
                    null
            );

            if(sessionCookie.isPresent()) {
                c.setCookie(sessionCookie.get());
            }

            if(basketUrl != null) {
                c.setUrl(basketUrl);
            }

            if(account != null) {
                c.setEmail(account.getEmail());
                c.setPassword(account.getPassword());
                c.setUrl(Accounts.generateAutoLoginUrl(this.options.getRegion(), account));
            }

            if(Config.INSTANCE.getDiscordHook().length() > 0) {
                Future<Boolean> alertFuture = Application.getExecutor().submit(new AlertWebhookTask(c));

                try {
                    boolean alerted = alertFuture.get();

                    if(alerted) {
                        Console.logSuccess("Alerted webhooks successfully", this.id);
                    } else {
                        Console.logSuccess("Failed to alert webhooks", this.id);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private boolean timeout() {
        try {
            sleep(5000);
        } catch(InterruptedException ex) {
            Console.logError("Error timing out the task.", this.id);
        }

        return this.addToCart();
    }

    private String waitForCaptcha() {
        try {
            String cap = Application.getWebServer().getCaptcha();

            if(cap != null) {
                return cap;
            }

            sleep(1500);
        } catch(InterruptedException ex) {
            Console.logError("Error waiting for captcha.", this.id);
        }

        return this.waitForCaptcha();
    }

    private boolean addToCart() {
        if(!this.running.get()) {
            return this.timeout();
        }

        if(this.failures >= 3) {
            this.running.set(false);
            new FallbackBrowser(this.url, this.cookieStore, this.proxy).start();
            return false;
        }

        String recapResponse = null;

        if(this.clientId != null) {
            // wait for captcha
            Console.log("@|yellow Waiting for captcha... |@", this.id);
            recapResponse = this.waitForCaptcha();
        }

        HttpPost request = new HttpPost(this.url);

        String cookieString = ClientUtil.getCookieString(this.cookieStore);

        if(cookieString != null) {
            request.setHeader(HttpHeaders.COOKIE, cookieString);
        }

        try {
            List<NameValuePair> params = new ArrayList<>(Arrays.asList(
                    new BasicNameValuePair("pid", String.format("%s_%d", this.options.getPid(), this.sizeCode)),
                    new BasicNameValuePair("Quantity", "1"),
                    new BasicNameValuePair("request", "ajax"),
                    new BasicNameValuePair("responseformat", "json")
            ));

            if(recapResponse != null) {
                params.add(new BasicNameValuePair("g-recaptcha-response", recapResponse));
            }

            request.setEntity(new UrlEncodedFormEntity(params));
        } catch(UnsupportedEncodingException ex) {
            Console.logError("Failed to add ATC data to request.", this.id);
            this.running.set(false);
            return false;
        }

        try {
            for(Header h : request.getAllHeaders()) {
                System.out.println(h.getName() + ":" + h.getValue());
            }
            CloseableHttpResponse response = this.httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();
            InputStream dataStream = responseEntity.getContent();

            String responseData = IOUtils.toString(dataStream, "UTF-8");

            int statusCode = response.getStatusLine().getStatusCode();

            dataStream.close();
            response.close();
            EntityUtils.consume(responseEntity);

            if(statusCode == 401 || statusCode == 403) {
                this.failures++;
                Console.logError("Failed ATC: Access Denied", this.id);
                return this.timeout();
            }

            if(statusCode != 200) {
                Console.logError(String.format("Failed ATC: %d", statusCode), this.id);
                return this.timeout();
            }

            try {
                JsonParser parser = new JsonParser();
                JsonObject root = parser.parse(responseData).getAsJsonObject();

                String status = root.get("result").getAsString();

                if(status.equalsIgnoreCase("success")) {
                    this.running.set(false);
                    this.httpClient.close();
                    Console.logSuccess(String.format("Carted %s successfully!", this.options.getPid() + " in size " + this.options.getSize()), this.id);
                    return true;
                }

                Console.logError(String.format("Error carting: %s", status), this.id);
                return this.timeout();
            } catch(JsonSyntaxException ex) {
                System.out.println(responseData);
                Console.logError("Error parsing response JSON.", this.id);
                return this.timeout();
            }
        } catch(IOException ex) {
            Console.logError(String.format("Failed to send ATC request: %s", ex.getLocalizedMessage()), this.id);
            return this.timeout();
        }
    }

}
