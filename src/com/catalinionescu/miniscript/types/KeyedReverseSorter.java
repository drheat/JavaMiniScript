package com.catalinionescu.miniscript.types;

import java.util.Comparator;

import com.catalinionescu.miniscript.intrinsics.KeyedValue;

public class KeyedReverseSorter  implements Comparator<KeyedValue> {
	public static KeyedReverseSorter instance = new KeyedReverseSorter();
	
	@Override
	public int compare(KeyedValue o1, KeyedValue o2) {
		if (o1 == null && o2 == null) return 0;
		
		if (o1 != null && o2 != null) {
			Value s1 = o1.sortKey;
			Value s2 = o2.sortKey;
			
			return Value.Compare(s2, s1);
		}
		
		return o1 == null ? 1 : -1;
	}
}
