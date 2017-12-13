package me.cameronb.adidas.task;

import me.cameronb.adidas.AdidasAccount;
import me.cameronb.adidas.Application;
import me.cameronb.adidas.Cart;
import me.cameronb.adidas.Region;
import me.cameronb.adidas.proxy.Proxy;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
    private final Region region;
    private final String pid;
    private final double size;
    private final int sizeCode;
    private final CloseableHttpClient httpClient;
    private final BasicCookieStore cookieStore;
    private final Proxy proxy;

    private AtomicBoolean running = new AtomicBoolean(true);

    public CartTask(int id, Region region, String pid, double size, BasicCookieStore cookieStore, Proxy proxy) {
        this.id = id;
        this.region = region;
        this.pid = pid;
        this.size = size;
        this.sizeCode = Sizing.getSizeCode(size);
        this.cookieStore = cookieStore;
        this.proxy = proxy;

        this.httpClient = ClientUtil.createClient(this.region, this.cookieStore, this.proxy, true);
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
                    basketUrl = String.format("http://www.adidas.%s%s", this.region.getTld(), basketUrlDecoded);

                    Future<Boolean> loadedFuture = Application.getExecutor().submit(new LoadAccountTask(this.region, basketUrl, this.cookieStore, null));

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
                    this.pid,
                    this.size,
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
                c.setUrl(Accounts.generateAutoLoginUrl(this.region, account));
            }

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

    private boolean timeout() {
        try {
            sleep(5000);
        } catch(InterruptedException ex) {
            Console.logError("Error timing out the task.", this.id);
        }

        return this.addToCart();
    }

    private boolean addToCart() {
        if(!this.running.get()) {
            return this.timeout();
        }

        HttpPost request = new HttpPost(String.format(
                "http://www.adidas.%s/on/demandware.store/Sites-adidas-%s-Site/%s/Cart-MiniAddProduct",
                this.region.getTld(),
                this.region.getDemandwareSite(),
                this.region.getLocale()
        ));

        try {
            List<NameValuePair> params = Arrays.asList(
                    new BasicNameValuePair("pid", String.format("%s_%d", this.pid, this.sizeCode)),
                    new BasicNameValuePair("Quantity", "1"),
                    new BasicNameValuePair("request", "ajax"),
                    new BasicNameValuePair("responseformat", "json")
            );

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
                Console.logError("Failed ATC: Access Denied", this.id);
                return this.timeout();
            }

            if(statusCode != 200) {
                Console.logError(String.format("Failed ATC: %d", statusCode), this.id);
                return this.timeout();
            }

            try {
                JSONObject root = new JSONObject(new JSONTokener(responseData));

                String status = root.getString("result");

                if(status.equalsIgnoreCase("success")) {
                    // cart success
                    String product = "UNKNOWN";

                    try {
                        JSONArray basket = root.getJSONArray("basket");
                        product = basket.getJSONObject(0).getString("product_id");
                    } catch(JSONException ex) {}

                    this.running.set(false);
                    this.httpClient.close();
                    Console.logSuccess(String.format("Carted %s successfully!", this.pid + " in size " + this.size), this.id);
                    return true;
                }

                Console.logError(String.format("Error carting: %s", status), this.id);
                return this.timeout();
            } catch(JSONException ex) {
                System.out.println(responseData);
                ex.printStackTrace();
                Console.logError("Error parsing response JSON.", this.id);
                return this.timeout();
            }
        } catch(IOException ex) {
            ex.printStackTrace();
            Console.logError(String.format("Failed to send ATC request: %s", ex.getLocalizedMessage()), this.id);
            return this.timeout();
        }
    }

}
