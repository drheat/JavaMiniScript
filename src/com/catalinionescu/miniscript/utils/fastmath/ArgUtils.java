package com.catalinionescu.miniscript.utils.fastmath;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for transforming the list of arguments passed to
 * constructors of exceptions.
 *
 */
public class ArgUtils {
    /**
     * Class contains only static methods.
     */
    private ArgUtils() {}

    /**
     * Transform a multidimensional array into a one-dimensional list.
     *
     * @param array Array (possibly multidimensional).
     * @return a list of all the {@code Object} instances contained in
     * {@code array}.
     */
    public static Object[] flatten(Object[] array) {
        final List<Object> list = new ArrayList<>();
        if (array != null) {
            for (Object o : array) {
                if (o instanceof Object[]) {
                    for (Object oR : flatten((Object[]) o)) {
                        list.add(oR);
                    }
                } else {
                    list.add(o);
                }
            }
        }
        return list.toArray();
    }
}