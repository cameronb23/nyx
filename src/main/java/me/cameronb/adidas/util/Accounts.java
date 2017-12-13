package me.cameronb.adidas.util;

import com.github.javafaker.Faker;
import com.github.javafaker.Name;
import me.cameronb.adidas.AdidasAccount;
import me.cameronb.adidas.Region;
import org.apache.http.client.utils.URIBuilder;
import org.passay.CharacterRule;
import org.passay.EnglishCharacterData;
import org.passay.PasswordGenerator;

import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

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
