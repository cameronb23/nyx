package me.cameronb.adidas;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import lombok.Getter;
import me.cameronb.adidas.captcha.CaptchaWebServer;
import me.cameronb.adidas.proxy.ProxyLoader;
import me.cameronb.adidas.serializable.Config;
import me.cameronb.adidas.serializable.TaskData;
import me.cameronb.adidas.task.RequestTask;
import me.cameronb.adidas.util.Console;
import org.beryx.textio.TextIO;
import org.beryx.textio.TextIoFactory;
import org.fusesource.jansi.AnsiConsole;
import org.json.JSONException;
import org.openqa.selenium.chrome.ChromeDriverService;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static org.fusesource.jansi.Ansi.ansi;

/**
 * Created by Cameron on 11/25/2017.
 */
public class Application {

    public static final String TASKS_FILE_PATH = System.getProperty("user.dir") + "/tasks.json";
    public static final String PROXIES_FILE_PATH = System.getProperty("user.dir") + "/proxies.txt";
    public static final String CONFIG_FILE_PATH = System.getProperty("user.dir") + "/config.xml";


    @Getter
    private static ProxyLoader proxyLoader;

    @Getter
    private static CaptchaWebServer webServer;

    @Getter
    private static ChromeDriverService driverService;

    @Getter
    private static ExecutorService executor = Executors.newCachedThreadPool();

    private static LinkedList<RequestTask> runningTasks = new LinkedList<>();

    public static void main(String[] args) {
        // configure jansi
        AnsiConsole.systemInstall();

        Console.logBasic("@|yellow Loading configuration... |@");
        try {
            Config.CONTEXT = JAXBContext.newInstance(Config.class);
        } catch(JAXBException ex) {
            Console.logBasic("@|red Error loading configuration! |@");
            System.exit(1);
        }

        File configFile = new File(CONFIG_FILE_PATH);
        if(configFile.exists()) {
            Config.INSTANCE = Config.loadConfig(configFile);
        } else {
            Config.INSTANCE = new Config();
        }

        System.setProperty("webdriver.chrome.driver", Config.INSTANCE.getChromeDriverPath());

        Console.logBasic("@|green Configuration loaded |@");

        try {
            boolean validated = Licensing.validateLicense(Config.INSTANCE.getLicense());
            if(!validated) {
                Console.logBasic("@|red Could not validate license key |@");
                System.exit(0);
            }
        } catch(IOException ex) {
            Console.logBasic("@|red Error reading license key. |@");
            System.exit(0);
        }

        Console.logBasic("@|yellow Loading proxies... |@");

        try {
            proxyLoader = new ProxyLoader(new File(PROXIES_FILE_PATH));
            Console.logBasic(String.format("@|green Loaded %d proxies. |@", proxyLoader.getProxiesLoaded().size()));
        } catch(IOException ex) {
            Console.logBasic("@|red Error loading proxies. |@");
            System.exit(0);
        }

        startPrompt();
    }

    public static void startServices() {
        Console.logBasic("@|yellow Starting api server |@");

        driverService = new ChromeDriverService.Builder()
                .usingDriverExecutable(new File(Config.INSTANCE.getChromeDriverPath()))
                .usingAnyFreePort()
                .build();

        try {
            driverService.start();
        } catch (IOException e) {
            e.printStackTrace();
            Console.logBasic("@|red Unable to start api server. |@");
            System.exit(1);
        }

        try {
            webServer = new CaptchaWebServer();
        } catch (IOException e) {
            Console.logBasic("@|red Failed to initialize Captcha webserver. Please close all applications using port 2145 and try again. |@");
            System.exit(0);
        }

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            driverService.stop();
            runningTasks.forEach(t -> {
                try {
                    t.shutdown();
                } catch(IOException ex) {
                    System.out.println("Failed to shutdown task");
                }
            });
        }));
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
                startServices();
                Console.logBasic("@|yellow Loading tasks... |@");
                List<TaskData> tasks = loadTasks();
                Console.logSuccess(String.format("@|green %s tasks loaded |@", tasks.size()), -1);

                for(int i = 0; i < tasks.size(); i++) {
                    TaskData taskData = tasks.get(i);

                    RequestTask task = new RequestTask(
                            (i+1),
                            taskData,
                            proxyLoader.getProxy()
                    );

                    runningTasks.add(task);
                    task.start();
                }
                break;
            default:
                System.exit(0);
                break;
        }
    }

    private static List<TaskData> loadTasks() {
        File tasksFile = new File(TASKS_FILE_PATH);

        try {
            if(!tasksFile.exists()) {
                List<TaskData> data = new ArrayList<>();
                saveTasks(data);
                return data;
            }

            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader(tasksFile));

            Type listType = new TypeToken<ArrayList<TaskData>>(){}.getType();
            List<TaskData> tasks = gson.fromJson(reader, listType);

            reader.close();

            return tasks;
        } catch(IOException | JSONException ex) {
            Console.logError("Error loading tasks. May be corrupt", -1);
            System.exit(1);
            return null;
        }
    }

    private static void saveTasks(List<TaskData> taskSet) throws IOException {
        try(Writer writer = new FileWriter(new File(TASKS_FILE_PATH))) {
            Gson gson = new Gson();
            gson.toJson(taskSet, writer);
            writer.close();
        }
    }

    private static void taskSetup(TextIO textIO) {
        int taskCount = textIO.newIntInputReader()
                .withMinVal(1)
                .read(ansi().render("@|magenta Amount of tasks to create(PER SIZE) |@").toString());

        String regionString = textIO.newStringInputReader()
                .withNumberedPossibleValues(Region.availableRegions())
                .read(ansi().render("@|magenta Region to run tasks on |@").toString());

        Region region = Region.getRegion(regionString);

//        if(region == null) {
//            Console.logBasic("@|red Invalid region. |@");
//            System.exit(1);
//        }

        String pid = textIO.newStringInputReader()
                .read(ansi().render("@|magenta Adidas product ID |@").toString());

        String sizeSelection = textIO.newStringInputReader()
                // .withDefaultValue("4,4.5,5,5.5,6,6.5,7,7.5,8,8.5,9,9.5,10,10.5,11,11.5,12,12.5,13,14")
                .withPattern("^[0-9.,]+$")
                .read(ansi().render("@|magenta Input sizes separated by commas or enter for 4-14 |@").toString());

        if(sizeSelection.length() < 1) {
            sizeSelection = "4,4.5,5,5.5,6,6.5,7,7.5,8,8.5,9,9.5,10,10.5,11,11.5,12,12.5,13,14";
        }

        String[] sizes = sizeSelection.split(",");

        List<TaskData> tasks = Arrays.stream(sizes).map(s -> {
            List<TaskData> sizeTasks = new ArrayList<>();
            for(int x = 0; x < taskCount; x++) {
                sizeTasks.add(new TaskData(pid, region, Double.valueOf(s)));
            }

            return sizeTasks;
        }).flatMap(Collection::stream).collect(Collectors.toList());

        try {
            saveTasks(tasks);
            Console.logBasic("@|green Saved task data to file |@");
            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
            Console.logBasic("@|red Error saving task data to file |@");
            System.exit(1);
        }
    }

}
