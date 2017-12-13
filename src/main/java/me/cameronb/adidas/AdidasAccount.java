package me.cameronb.adidas;

import lombok.Data;

import java.util.Date;

/**
 * Created by Cameron on 12/12/2017.
 */
@Data
public class AdidasAccount {

    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private Date birthday;

}
