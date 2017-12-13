package me.cameronb.adidas.serializable;

import com.google.gson.annotations.JsonAdapter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by Cameron on 12/13/2017.
 */
@JsonAdapter(MachineAdapter.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Machine {
    private String fingerprint;
    private String displayName;
    private String platform;
}
