package me.cameronb.adidas.task;

import me.cameronb.adidas.Cart;
import me.cameronb.adidas.util.Console;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.time.Instant;
import java.util.concurrent.Callable;

/**
 * Created by Cameron on 12/12/2017.
 */
public class AlertWebhookTask implements Callable<Boolean> {

    private final Cart cart;

    public AlertWebhookTask(Cart cart) {
        this.cart = cart;
    }

    private JSONObject createPayload() {

        String description = "PID: " + this.cart.getPid() + "\n";

        if(cart.getEmail() != null) {
            description += String.format("Account email: %s\nAccount password: %s\nBackup cookie code: `document.cookie=\"%s\"`",
                    cart.getEmail(),
                    cart.getPassword(),
                    cart.getCookie().getName() + "=" + cart.getCookie().getValue());
        }

        String thumbUrl = "http://demandware.edgesuite.net/sits_pod20-adidas/dw/image/v2/aaqx_prd/on/demandware.static/-/Sites-adidas-products/en_US/dw2f4adb27/zoom/" + this.cart.getPid() + "_01_standard.jpg?width=80&height=80";
        long now = System.currentTimeMillis() / 1000L;

        String url = "https://www.adidas.com/yeezy";

        if(cart.getUrl() != null) {
            url = cart.getUrl();
        }

        return new JSONObject().put("attachments", new JSONArray().put(
                new JSONObject()
                .put("title", String.format("Nyx Cart - %s", this.cart.getSize()))
                .put("title_link", url)
                .put("color", "#D63E3E")
                .put("fallback", String.format("Nyx Cart(%s) - %s:%s", this.cart.getSize(), this.cart.getEmail(), this.cart.getPassword()))
                .put("text", description)
                .put("thumb_url", thumbUrl)
                .put("footer", "Powered by Nyx Adidas Bot")
                .put("ts", now)
        ));
    }

    @Override
    public Boolean call() {
        try {
            JSONObject jsonBody = createPayload();

            CloseableHttpClient client = HttpClientBuilder.create().build();

            HttpPost request = new HttpPost("https://discordapp.com/api/webhooks/384017334605447168/U4f2cjSl7kO5l-ZbngpZTW0WY_wZVCiQYuYdmZytksTd_DwnqUS0SR8Tyv0GS__JaRvI/slack");

            StringEntity data = new StringEntity(jsonBody.toString());

            request.addHeader(HttpHeaders.CONTENT_TYPE, "application/json");
            request.setEntity(data);

            CloseableHttpResponse response = client.execute(request);
            HttpEntity responseEntity = response.getEntity();
            InputStream input = responseEntity.getContent();

            int statusCode = response.getStatusLine().getStatusCode();
            String responseData = IOUtils.toString(input, "UTF-8");

            EntityUtils.consume(responseEntity);
            EntityUtils.consume(data);
            input.close();
            response.close();
            client.close();

            if(statusCode / 200 < 2) {
                // success
                return true;
            }

            Console.logError("Failed to alert webhooks: " + responseData + ":" + statusCode, -1);
            return false;
        } catch(UnsupportedEncodingException ex) {
            ex.printStackTrace();
            Console.logError("Error compiling webhook data", -1);
            return false;
        } catch(IOException ex) {
            ex.printStackTrace();
            return false;
        }
    }

}
