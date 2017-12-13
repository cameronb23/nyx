package me.cameronb.adidas;

import lombok.Getter;
import me.cameronb.adidas.captcha.CaptchaWebServer;
import me.cameronb.adidas.proxy.ProxyLoader;
import me.cameronb.adidas.util.Console;
import org.fusesource.jansi.AnsiConsole;

import java.io.File;
import java.io.IOException;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Cameron on 11/25/2017.
 */
public class Application {

    @Getter
    private static ProxyLoader proxyLoader;

    @Getter
    private static ExecutorService executor = Executors.newCachedThreadPool();

    public static void main(String[] args) {
        // configure jansi
        AnsiConsole.systemInstall();

        Console.logBasic("@|yellow Loading proxies... |@");

        try {
            proxyLoader = new ProxyLoader(new File(System.getProperty("user.dir") + "/proxies.txt"));
            Console.logBasic(String.format("@|green Loaded %d proxies. |@", proxyLoader.getProxiesLoaded().size()));
        } catch(IOException ex) {
            Console.logBasic("@|red Error loading proxies. |@");
            System.exit(0);
        }

//        startPrompt();

        try {
            new CaptchaWebServer();
        } catch (IOException e) {
            Console.logBasic("@|red Failed to initialize Captcha webserver. Please close all applications using port 2145 and try again. |@");
            System.exit(0);
        }

//        new CartTask(1, Region.GB, "AC7749", 7.5, new BasicCookieStore(), proxyLoader.getProxy()).start();

//        int x = 0;
//
//        while(x < 50) {
//            x++;
//            new RequestTask(x, "AC7749", 7.5, proxyLoader.getProxy()).start();
//        }
    }

    private static void startPrompt() {
        Scanner inputScanner = new Scanner(System.in);

        Console.logBasic("@|magenta Select Task Mode |@");
        Console.logBasic("@|cyan 1. Task Setup\n2. Splash |@");

        int input = inputScanner.nextInt();

        System.out.println(input);

        inputScanner.close();
    }

}
