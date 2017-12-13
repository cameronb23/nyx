package me.cameronb.adidas.util;

import org.fusesource.jansi.Ansi;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Date;
import java.util.Locale;

import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

/**
 * Created by Cameron on 12/11/2017.
 */
public class Console {

    private static final DateTimeFormatter formatter = DateTimeFormatter
            .ofPattern("H:m:s.SSS")
            .withLocale(Locale.US)
            .withZone(ZoneId.systemDefault());

    public static void logSuccess(String message, int taskId) {
        String msg = ansi().render("@|green " + message + "|@").toString();
        log(msg, taskId);
    }

    public static void logError(String message, int taskId) {
        String msg = ansi().render("@|red " + message + "|@").toString();
        log(msg, taskId);
    }

    public static void log(String message, int taskId) {
        Instant time = Instant.now();
        Ansi msg = ansi()
                .fg(MAGENTA).a("[" + formatter.format(time) + "]");

        if(taskId != -1) {
            msg.fg(CYAN).a("(" + taskId + ")");
        }

        msg.reset().render(" " + message).reset();
        System.out.println(msg);
    }

    public static void logBasic(String message) {
        System.out.println(ansi().render(message));
    }

}
