package me.cameronb.adidas;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import me.cameronb.adidas.serializable.Config;
import me.cameronb.adidas.serializable.Machine;
import me.cameronb.adidas.util.Console;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;

/**
 * Created by Cameron on 12/13/2017.
 */
public class Licensing {

    private static final String PRODUCT_ID = "5a3167b1734d1d293237075d";

    public static boolean validateLicense(String key) throws IOException {

        Machine machine = getMachineDetails(key);

        if(machine == null) {
            return false;
        }

        CloseableHttpClient client = HttpClients.createDefault();

        Gson gson = new Gson();

        HttpPost request = new HttpPost("https://lightspeed-api.cameronb.me/activation/validate");

        String dataString = String.format("{\"email\":\"%s\",\"productId\":\"%s\",\"licenseKey\":\"%s\",\"machine\": %s}",
                Config.INSTANCE.getEmail(),
                PRODUCT_ID,
                Config.INSTANCE.getLicense(),
                gson.toJson(machine));

        StringEntity entity = new StringEntity(dataString);

        request.setEntity(entity);

        request.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        CloseableHttpResponse response = client.execute(request);

        int code = response.getStatusLine().getStatusCode();

        boolean validated = false;

        if(code == 200) {
            validated = true;
        } else {
            HttpEntity ent = response.getEntity();
            InputStream stream = ent.getContent();
            String data = IOUtils.toString(stream);
            System.out.println(data);
            stream.close();
            EntityUtils.consume(ent);
        }

        EntityUtils.consume(entity);
        response.close();
        client.close();
        return validated;
    }

    private static Machine getMachineDetails(String licenseKey) {
        Machine machine = null;
        try {
            machine = getNetworkDetails(licenseKey);
        } catch(IOException ex) {
            Console.logBasic("@|red Error retrieving machine information |@");
            return null;
        }

        String os = System.getProperty("os.name");

        machine.setPlatform(os);
        return machine;
    }

    private static Machine getNetworkDetails(String licenseKey) throws IOException {
        String host;
        String macAddress;
        String hashed;

        InetAddress local = InetAddress.getLocalHost();
        host = local.getHostName();

        NetworkInterface network = NetworkInterface.getByInetAddress(local);

        byte[] macBytes = network.getHardwareAddress();

        StringBuilder sb = new StringBuilder();

        for(int i = 0; i < macBytes.length; i++) {
            sb.append(String.format("%02X%s", macBytes[i], (i < macBytes.length - 1) ? "-" : ""));
        }

        macAddress = sb.toString();

        String beforeHash = macAddress + licenseKey;

        hashed = Hashing.sha256().hashString(beforeHash, StandardCharsets.UTF_8).toString();

        Machine m = new Machine();
        m.setDisplayName(host);
        m.setFingerprint(hashed);

        return m;
    }

}
