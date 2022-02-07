package com.catalinionescu.miniscript.utils;

import java.util.Comparator;
import java.util.List;

public class ListUtil<T> {
	ListUtil() {
		
	}
	
	public int FindIndex(List<T> list, Comparator<T> comparator, T value) {
		return FindIndex(list, 0, comparator, value);
	}
	
	public int FindIndex(List<T> list, int startIndex, Comparator<T> comparator, T value) {
		for (int i = startIndex; i < list.size(); i++) {
			if (comparator.compare(list.get(i), value) == 0) {
				return i;
			}
		}
		
		return -1;
	}
	
	private static ListUtil<?> instance = null;
	
	public static ListUtil<?> getInstance() {
		if (instance == null) {
			instance = new ListUtil<>();
		}
		
		return instance;
	}
}
