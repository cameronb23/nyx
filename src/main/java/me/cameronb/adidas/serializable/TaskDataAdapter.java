package me.cameronb.adidas.serializable;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.cameronb.adidas.Region;

import java.io.IOException;

/**
 * Created by Cameron on 12/13/2017.
 */
public class TaskDataAdapter extends TypeAdapter<TaskData> {

    @Override
    public void write(JsonWriter writer, TaskData obj) throws IOException {
        writer.beginObject();

        writer.name("pid").value(obj.getPid());
        writer.name("region").value(obj.getRegion().name());
        writer.name("size").value(obj.getSize());

        writer.endObject();
    }

    @Override
    public TaskData read(JsonReader in) throws IOException {
        in.beginObject();
        TaskData d = new TaskData();

        while (in.hasNext()) {
            String key = in.nextName();

            switch(key) {
                case "pid":
                    d.setPid(in.nextString());
                    break;
                case "region":
                    d.setRegion(Region.getRegion(in.nextString()));
                    break;
                case "size":
                    d.setSize(in.nextDouble());
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
