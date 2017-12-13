package me.cameronb.adidas.proxy;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Created by Cameron on 12/12/2017.
 */
@Data @AllArgsConstructor
public class Proxy {

    private String address;
    private int port;
    private String username;
    private String password;
}
