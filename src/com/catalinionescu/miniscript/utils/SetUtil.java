package com.catalinionescu.miniscript.utils;

import java.util.Set;

public class SetUtil {
    public static <T> T ElementAt(Set<T> set, int n) {
        if (set == null || n < 0 || n >= set.size()) {
        	return null;
        }
        
        int count = 0;
        for (T element : set) {
            if (n == count) {
                return element;
            }
            count++;
        }
        
        return null;
    }
}
