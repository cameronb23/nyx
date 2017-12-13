package me.cameronb.adidas;

import me.cameronb.adidas.serializable.Config;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Created by Cameron on 12/13/2017.
 */
public class Licensing {

    private static final String PRODUCT_ID = "5a3167b1734d1d293237075d";

    public static boolean validateLicense(String key) throws IOException {

        CloseableHttpClient client = HttpClients.createDefault();

        HttpPost request = new HttpPost("https://lightspeed-api.cameronb.me/activation/validate");

        StringEntity entity = new StringEntity(String.format("{\"productId\":\"%s\",\"licenseKey\":\"%s\"}", PRODUCT_ID, Config.INSTANCE.getLicense()));
        request.setEntity(entity);

        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        CloseableHttpResponse response = client.execute(request);

        int code = response.getStatusLine().getStatusCode();

        boolean validated = false;

        if(code == 200) {
            validated = true;
        }

        EntityUtils.consume(entity);
        response.close();
        client.close();
        return validated;
    }

}
