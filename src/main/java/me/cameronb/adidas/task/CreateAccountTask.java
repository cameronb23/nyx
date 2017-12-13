package me.cameronb.adidas.task;

import me.cameronb.adidas.AdidasAccount;
import me.cameronb.adidas.Region;
import me.cameronb.adidas.proxy.Proxy;
import me.cameronb.adidas.util.Accounts;
import me.cameronb.adidas.util.ClientUtil;
import me.cameronb.adidas.util.Console;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Cameron on 12/12/2017.
 */
public class CreateAccountTask implements Callable<AdidasAccount> {

    private BasicCookieStore cookieStore;
    private Proxy proxy;

    public CreateAccountTask(BasicCookieStore cookieStore, Proxy proxy) {
        this.cookieStore = cookieStore;
        this.proxy = proxy;
    }

    private String doStep(String body, Map<String, Object> payload, CloseableHttpClient httpClient) throws Exception {
        Document doc = Jsoup.parse(body);

        Element form = doc.select("form").first();

        if(form == null) {
            throw new Exception("Unable to parse HTML");
        }

        String nextUrl = form.attr("action");

        Elements inputs = form.select("input,select,button[value]");

        Map<String, Object> newParams = inputs.stream().collect(Collectors.toMap(input -> input.attr("name"), input -> input.val()));

        newParams.putAll(payload);

        List<NameValuePair> params = newParams.entrySet().stream()
                .map(entry -> new BasicNameValuePair(entry.getKey(), String.valueOf(entry.getValue())))
                .collect(Collectors.toList());

        HttpPost request = new HttpPost(nextUrl);

        UrlEncodedFormEntity paramData;

        try {
            paramData = new UrlEncodedFormEntity(params);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return doStep(body, payload, httpClient);
        }

        request.setEntity(paramData);

        try {
            CloseableHttpResponse response = httpClient.execute(request);
            HttpEntity responseEntity = response.getEntity();
            InputStream responseStream = responseEntity.getContent();

            String data = IOUtils.toString(responseStream, "UTF-8");

            if(response.getStatusLine().getStatusCode() != 200) {
                System.out.println(data);
            }

            EntityUtils.consume(paramData);
            EntityUtils.consume(responseEntity);
            responseStream.close();
            response.close();
            return data;
        } catch (IOException ex) {
            ex.printStackTrace();
            return doStep(body, payload, httpClient);
        }
    }

    @Override
    public AdidasAccount call() {
        CloseableHttpClient client = ClientUtil.createClient("https://www.adidas.com", Region.US, this.cookieStore, this.proxy, false);
        AdidasAccount account = Accounts.generateAccount();

        try {
            CloseableHttpResponse response = client.execute(new HttpGet("https://www.adidas.com/on/demandware.store/Sites-adidas-US-Site/en_US/MiAccount-Register?redirect=https%3a%2f%2fcfg%2eadidas%2ecom%2fconfigurator%2fmiadidas%2faccount%2faccount%2ehtml"));
            HttpEntity responseEntity = response.getEntity();
            InputStream responseStream = responseEntity.getContent();
            String responseData = IOUtils.toString(responseStream, "UTF-8");

            EntityUtils.consume(responseEntity);
            responseStream.close();
            response.close();

            Calendar calendar = Calendar.getInstance();
            calendar.setTime(account.getBirthday());

            int day = calendar.get(Calendar.DAY_OF_MONTH);
            int month = calendar.get(Calendar.MONTH);
            int year = calendar.get(Calendar.YEAR);

            Map<String, Object> stepOnePayload = Collections.unmodifiableMap(
                    Stream.of(
                            new AbstractMap.SimpleEntry<>("dwfrm_mipersonalinfo_firstname", account.getFirstName()),
                            new AbstractMap.SimpleEntry<>("dwfrm_mipersonalinfo_lastname", account.getLastName()),
                            new AbstractMap.SimpleEntry<>("dwfrm_mipersonalinfo_customer_birthday_dayofmonth", day),
                            new AbstractMap.SimpleEntry<>("dwfrm_mipersonalinfo_customer_birthday_month", month),
                            new AbstractMap.SimpleEntry<>("dwfrm_mipersonalinfo_customer_birthday_year", year)
                    ).collect(
                            Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())
                    )
            );

            Map<String, Object> stepTwoPayload = Collections.unmodifiableMap(
                    Stream.of(
                            new AbstractMap.SimpleEntry<>("dwfrm_milogininfo_email", account.getEmail()),
                            new AbstractMap.SimpleEntry<>("dwfrm_milogininfo_password", account.getPassword()),
                            new AbstractMap.SimpleEntry<>("dwfrm_milogininfo_newpasswordconfirm", account.getPassword())
                    ).collect(
                            Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())
                    )
            );

            Map<String, Object> stepThreePayload = Collections.unmodifiableMap(
                    Stream.of(new AbstractMap.SimpleEntry<>("dwfrm_micommunicinfo_agreeterms", true)).collect(
                            Collectors.toMap((e) -> e.getKey(), (e) -> e.getValue())
                    )
            );

            try {
                String stepOneRes = doStep(responseData, stepOnePayload, client);

                String stepTwoRes = doStep(stepOneRes, stepTwoPayload, client);

                String stepThreeRes = doStep(stepTwoRes, stepThreePayload, client);
            } catch(Exception ex) {
                return null;
            }

            return account;
        } catch (IOException e) {
            Console.logBasic("@|red Unable to create account: " + e.getLocalizedMessage() + " |@");
            return null;
        }
    }
}