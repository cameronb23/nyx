package me.cameronb.adidas.proxy;

import lombok.Getter;
import me.cameronb.adidas.util.Console;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by Cameron on 12/12/2017.
 */
public class ProxyLoader {

    @Getter
    private final List<Proxy> proxiesLoaded = new ArrayList<>();

    public ProxyLoader(File f) throws IOException {

        if(!f.exists()) return;

        BufferedReader reader = new BufferedReader(new FileReader(f));

        String line;
        int lineIndex = 0;

        while((line = reader.readLine()) != null) {
            lineIndex++;
            String[] data = line.split(":");
            int len = data.length;
            if(len == 2) {
                proxiesLoaded.add(new Proxy(data[0], Integer.parseInt(data[1]), null, null));
            } else if(len == 4) {
                proxiesLoaded.add(new Proxy(data[0], Integer.parseInt(data[1]), data[2], data[3]));
            } else {
                Console.logError(String.format("could not parse proxy %s", line), -1);
            }
        }

    }

    public Proxy getProxy() {
        return this.proxiesLoaded.get(ThreadLocalRandom.current().nextInt(0, proxiesLoaded.size()));
    }
}
