package me.cameronb.adidas.serializable;

import com.google.gson.annotations.JsonAdapter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.cameronb.adidas.Region;

/**
 * Created by Cameron on 12/13/2017.
 * {"pid":"CG6422","region":"us","size":"4","account":true,"proxy":true,"splash":true,"testMode":true}
 */
@JsonAdapter(TaskDataAdapter.class)
@Data @NoArgsConstructor @AllArgsConstructor
public class TaskData {
    private String pid;
    private Region region;
    private double size;
}
