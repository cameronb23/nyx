package me.cameronb.adidas;

import lombok.Getter;
import me.cameronb.adidas.captcha.CaptchaWebServer;
import me.cameronb.adidas.proxy.ProxyLoader;
import me.cameronb.adidas.task.RequestTask;
import me.cameronb.adidas.util.Console;
import org.apache.commons.io.IOUtils;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.fusesource.jansi.AnsiConsole;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Created by Cameron on 11/25/2017.
 */
public class Application {

    @Getter
    private static ProxyLoader proxyLoader;

    @Getter
    private static CaptchaWebServer webServer;

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
            webServer = new CaptchaWebServer();
        } catch (IOException e) {
            Console.logBasic("@|red Failed to initialize Captcha webserver. Please close all applications using port 2145 and try again. |@");
            System.exit(0);
        }

        startPrompt();
    }

    private static void startPrompt() {
        TextIO textIO = TextIoFactory.getTextIO();

        Console.logBasic("@|cyan Task modes: |@");
        Console.logBasic("@|cyan 1. Task Setup\n2. Splash |@");

        int taskMode = textIO.newIntInputReader()
                .read(ansi().render("@|magenta Select App mode |@").toString());

        switch (taskMode) {
            case 1:
                taskSetup(textIO);
                break;
            case 2:
                Console.logBasic("@|yellow Loading tasks... |@");
                JSONArray tasks = loadTasks();
                Console.logSuccess(String.format("@|green %s tasks loaded |@", tasks.length()), -1);

                for(int i = 0; i < tasks.length(); i++) {
                    JSONObject taskData = tasks.getJSONObject(i);

                    new RequestTask(
                            (i+1),
                            Region.getRegion(taskData.getString("region")),
                            taskData.getString("pid"),
                            taskData.getDouble("size"),
                            proxyLoader.getProxy()
                    ).start();
                }
                break;
            default:
                System.exit(0);
                break;
        }
    }

    private static JSONArray loadTasks() {
        File tasksFile = new File(System.getProperty("user.dir") + "/tasks.json");

        try {
            if(!tasksFile.exists()) {
                saveTasks(new JSONArray());
                return new JSONArray();
            }

            InputStream inputStream = new FileInputStream(tasksFile);
            String data = IOUtils.toString(inputStream, "UTF-8");
            JSONArray tasks = new JSONArray(data);

            return tasks;
        } catch(IOException | JSONException ex) {
            Console.logError("Error loading tasks. May be corrupt", -1);
            System.exit(1);
            return null;
        }
    }

    private static void saveTasks(JSONArray taskSet) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(new File(System.getProperty("user.dir") + "/tasks.json")));
        writer.write(taskSet.toString());
        writer.close();
    }

    private static void taskSetup(TextIO textIO) {
        int taskCount = textIO.newIntInputReader()
                .read(ansi().render("@|magenta Amount of tasks to create |@").toString());
        String regionString = textIO.newStringInputReader()
                .read(ansi().render("@|magenta Region to run tasks on(us/uk) |@").toString());

        Region region = Region.getRegion(regionString);

        if(region == null) {
            Console.logError("Invalid region.", -1);
            System.exit(1);
        }

        String pid = textIO.newStringInputReader()
                .read(ansi().render("@|magenta Adidas product ID |@").toString());

        String sizeSelection = textIO.newStringInputReader()
                .withDefaultValue("4,4.5,5,5.5,6,6.5,7,7.5,8,8.5,9,9.5,10,10.5,11,11.5,12,12.5,13,14")
                .read(ansi().render("@|magenta Input sizes separated by commas or enter for 4-14").toString());

        JSONArray root = new JSONArray();



        System.exit(0);
    }

}
