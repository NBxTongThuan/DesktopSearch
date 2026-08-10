package org.thuantn.desktopsearch.common;

import java.util.Objects;
import java.util.function.Consumer;

public class CommonUtil {

    public static boolean objectIsNullOrEmpty(Object object)
    {
        return Objects.isNull(object);
    }

    public static void log(Object t)
    {
        Objects.requireNonNull(t, "");
        System.out.println(t.toString());
    }



}
