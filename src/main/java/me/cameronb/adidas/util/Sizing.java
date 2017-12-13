package me.cameronb.adidas.util;

/**
 * Created by Cameron on 12/12/2017.
 */
public class Sizing {

    private static final int BASE_SIZE = 600;
    private static final double BASE_SIZE_LITERAL = 7.5;

    public static int getSizeCode(double size) {
        double diff = size - BASE_SIZE_LITERAL;
        double sizeModifier = diff * 20;
        Double rawSize = BASE_SIZE + sizeModifier;
        return rawSize.intValue();
    }
}
