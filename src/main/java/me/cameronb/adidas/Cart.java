package me.cameronb.adidas;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.http.cookie.Cookie;

/**
 * Created by Cameron on 12/12/2017.
 */
@Data @AllArgsConstructor
public class Cart {

    private String pid;
    private double size;
    private String email;
    private String password;
    private Cookie cookie;
    private String url;

}
