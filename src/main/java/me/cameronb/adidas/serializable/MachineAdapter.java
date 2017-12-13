package me.cameronb.adidas.serializable;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

/**
 * Created by Cameron on 12/13/2017.
 */
public class MachineAdapter extends TypeAdapter<Machine> {

    @Override
    public void write(JsonWriter writer, Machine obj) throws IOException {
        writer.beginObject();

        writer.name("fingerprint").value(obj.getFingerprint());
        writer.name("name").value(obj.getDisplayName());
        writer.name("platform").value(obj.getPlatform());

        writer.endObject();
    }

    @Override
    public Machine read(JsonReader in) throws IOException {
        in.beginObject();
        Machine d = new Machine();

        while (in.hasNext()) {
            String key = in.nextName();

            switch(key) {
                case "fingerprint":
                    d.setFingerprint(in.nextString());
                    break;
                case "name":
                    d.setDisplayName(in.nextString());
                    break;
                case "platform":
                    d.setPlatform(in.nextString());
                    break;
                default:
                    in.skipValue();
                    break;
            }
        }

        in.endObject();
        return d;
    }

}
