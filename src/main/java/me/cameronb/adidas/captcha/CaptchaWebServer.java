package me.cameronb.adidas.captcha;

import fi.iki.elonen.NanoHTTPD;
import me.cameronb.adidas.util.Console;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Cameron on 12/12/2017.
 */
public class CaptchaWebServer extends NanoHTTPD {

    private Object captchaLock = new Object();
    private AtomicBoolean onHold = new AtomicBoolean(false);
    private List<Captcha> captchas = new LinkedList<>();

    public String getCaptcha() {
//        if(onHold.get()) {
//            return null;
//        }
        synchronized (captchaLock) {
            onHold.set(true);
            if(captchas.size() < 1) {
                return null;
            }

            Captcha cap = captchas.get(0);

            if(cap.getTime().plusSeconds(110).isBefore(Instant.now())) {
                captchas.remove(cap);
                return null;
            }

            captchas.remove(cap);
//            onHold.set(false);
            return cap.getToken();
        }
    }

    public CaptchaWebServer() throws IOException {
        super(2145);
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        Console.logBasic("@|green Captcha server running at http://captcha.adidas.com:2145 |@");
    }

    @Override
    public Response serve(IHTTPSession session) {
        String msg;
        Map<String, String> parms = session.getParms();
        if(session.getMethod().equals(Method.POST)) {

            Map<String, String> files = new HashMap<>();

            try {
                session.parseBody(files);
            } catch (IOException ioe) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "SERVER INTERNAL ERROR: IOException: " + ioe.getMessage());
            } catch (ResponseException re) {
                return newFixedLengthResponse(re.getStatus(), MIME_PLAINTEXT, re.getMessage());
            }

            String token = session.getQueryParameterString().substring(21);
            captchas.add(new Captcha(token, Instant.now(), Captcha.CaptchaSource.LOCAL));
            Console.logSuccess("Received token.", -1);
            Response r = newFixedLengthResponse(Response.Status.REDIRECT, MIME_PLAINTEXT, "REDIRECTING");
            r.addHeader("Location", "http://captcha.adidas.com:2145");
            return r;
        } else {
            msg = "<html><head> <title>Nyx Captcha Harvester</title> <script src=\"https://www.google.com/recaptcha/api.js\" async defer></script></head><body> <form action=\"/solve\" method=\"POST\"> <div class=\"g-recaptcha\" data-sitekey=\"6Le-jDsUAAAAAHomSeUw7A9MhQpbP2sSnDzBgeMl\"></div> <br/> <input type=\"submit\" value=\"Submit\"> </form></body></html>";
        }

        return newFixedLengthResponse(msg);
    }

}
