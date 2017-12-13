package me.cameronb.adidas.captcha;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Instant;

/**
 * Created by Cameron on 12/12/2017.
 */
@Data @AllArgsConstructor
public class Captcha {

   private String token;
   private Instant time;
   private CaptchaSource source;

   public enum CaptchaSource {
       LOCAL,
       MOBILE,
       WEB
   }

}
