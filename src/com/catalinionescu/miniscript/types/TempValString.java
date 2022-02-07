package com.catalinionescu.miniscript.types;

//We frequently need to generate a ValString out of a string for fleeting purposes,
// like looking up an identifier in a map (which we do ALL THE TIME).  So, here's
// a little recycling pool of reusable ValStrings, for this purpose only.
public class TempValString extends ValString {
	private TempValString next;

	private TempValString(String s) {
		super(s);
		this.next = null;
	}

	private static TempValString _tempPoolHead = null;
	private static Object lockObj = new Object();
	
	public static TempValString Get(String s) {
		synchronized(lockObj) {
			if (_tempPoolHead == null) {
				return new TempValString(s);
			} else {
				TempValString result = _tempPoolHead;
				_tempPoolHead = _tempPoolHead.next;
				result.value = s;
				return result;
			}
		}
	}
	
	public static void Release(TempValString temp) {
		synchronized(lockObj) {
			temp.next = _tempPoolHead;
			_tempPoolHead = temp;
		}
	}
}
