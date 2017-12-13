package me.cameronb.adidas.util;

import com.github.javafaker.Faker;
import com.github.javafaker.Name;
import me.cameronb.adidas.AdidasAccount;
import me.cameronb.adidas.Region;
import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.LaxRedirectStrategy;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.passay.*;
import sun.nio.ch.IOUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Created by Cameron on 12/12/2017.
 */
public class Accounts {

    private static final Faker generator;
    private static final PasswordGenerator passwordGen;
    private static final List<CharacterRule> passwordRules;

    static {
        generator = new Faker();
        passwordGen = new PasswordGenerator();

        passwordRules = Arrays.asList(
                new CharacterRule(EnglishCharacterData.UpperCase, 1),
                new CharacterRule(EnglishCharacterData.LowerCase, 1),
                new CharacterRule(EnglishCharacterData.Digit, 1)
        );
    }

    public static Name getName() {
        return generator.name();
    }

    public static Date getBirthday() {
        return generator.date().birthday(16, 75);
    }

    public static String getPassword() {
        return passwordGen.generatePassword(10, passwordRules);
    }

    public static AdidasAccount generateAccount() {
        Name name = getName();

        AdidasAccount acc = new AdidasAccount();
        acc.setFirstName(name.firstName());
        acc.setLastName(name.lastName());
        acc.setBirthday(getBirthday());

        String id = ("" + Math.random()).substring(2, 7);

        String email = String.format("%s.%s%s@cameronb.me", acc.getFirstName(), acc.getLastName(), id);
        String password = passwordGen.generatePassword(10, passwordRules);

        acc.setEmail(email);
        acc.setPassword(password);

        return acc;
    }

    public static String generateAutoLoginUrl(Region region, AdidasAccount account) {
        try {
            URIBuilder uriBuilder = new URIBuilder(String.format("https://cp.adidas.%s/idp/startSSO.ping", region.getTld()));
            uriBuilder
                    .addParameter("username", account.getEmail())
                    .addParameter("password", account.getPassword())
                    .addParameter("signinSubmit", "Sign in")
                    .addParameter("IdpAdapterId", "adidasIdP10")
                    .addParameter("SpSessionAuthnAdapterId", String.format("https://cp.adidas.%s/web/", region.getTld()))
                    .addParameter("PartnerSpId", "sp:demandware")
                    .addParameter("validator_id", "adieComDW" + region.getDemandwareSite().toLowerCase())
                    .addParameter("TargetResource", String.format("https://www.adidas.%s/on/demandware.store/Sites-adidas-%s-Site/%s/MyAccount-ResumeLogin?target=account", region.getTld(), region.getDemandwareSite(), region.getLocale()))
                    .addParameter("target", "account")
                    .addParameter("InErrorResource", String.format("https://www.adidas.%s/on/demandware.store/Sites-adidas-%s-Site/%s/null", region.getTld(), region.getDemandwareSite(), region.getLocale()))
                    .addParameter("loginUrl", String.format("https://cp.adidas.%s/web/eCom/%s/loadsignin", region.getTld(), region.getLocale()))
                    .addParameter("cd", String.format("eCom|%s|cp.adidas.%s|null", region.getLocale(), region.getTld()))
                    .addParameter("remembermeParam", "")
                    .addParameter("app", "eCom")
                    .addParameter("locale", region.getMicroSite())
                    .addParameter("domain", "cp.adidas." + region.getTld())
                    .addParameter("email", "")
                    .addParameter("pfRedirectBaseURL_test", "https://cp.adidas." + region.getTld())
                    .addParameter("pfStartSSOURL_test", String.format("https://cp.adidas.%s/idp/startSSO.ping", region.getTld()))
                    .addParameter("resumeURL_test", "")
                    .addParameter("FromFinishRegistraion", "")
                    .addParameter("CSRFToken", "");

            return uriBuilder.build().toString();
        } catch(URISyntaxException ex) {
            ex.printStackTrace();
            return null;
        }
    }
}
