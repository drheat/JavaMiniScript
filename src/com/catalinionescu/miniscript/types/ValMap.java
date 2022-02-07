package com.catalinionescu.miniscript.types;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.catalinionescu.miniscript.Context;
import com.catalinionescu.miniscript.Machine;
import com.catalinionescu.miniscript.exceptions.IndexException;
import com.catalinionescu.miniscript.exceptions.KeyException;
import com.catalinionescu.miniscript.exceptions.TypeException;
import com.catalinionescu.miniscript.exceptions.UndefinedIdentifierException;
import com.catalinionescu.miniscript.utils.SetUtil;

/// <summary>
/// ValMap represents a MiniScript map, which under the hood is just a Dictionary
/// of Value, Value pairs.
/// </summary>
public class ValMap extends Value {
	public Map<Value, Value> map;

	// Assignment override function: return true to cancel (override)
	// the assignment, or false to allow it to happen as normal.
	public interface AssignOverrideFunc {
		boolean run(Value key, Value value);
	}
	
	public AssignOverrideFunc assignOverride;

	public ValMap() {
		// TODO: make sure the Value comparison for the key set finds duplicates properly 
		//this.map = new HashMap<Value, Value>(RValueEqualityComparer.instance);
		this.map = new HashMap<Value, Value>();
	}
	
	@Override
	public boolean BoolValue() {
		// A map is considered true if it is nonempty.
		return map != null && map.size() > 0;
	}

	/// <summary>
	/// Convenience method to check whether the map contains a given string key.
	/// </summary>
	/// <param name="identifier">string key to check for</param>
	/// <returns>true if the map contains that key; false otherwise</returns>
	public boolean ContainsKey(String identifier) {
		TempValString idVal = TempValString.Get(identifier);
		boolean result = map.containsKey(idVal);
		TempValString.Release(idVal);
		return result;
	}
	
	/// <summary>
	/// Convenience method to check whether this map contains a given key
	/// (of arbitrary type).
	/// </summary>
	/// <param name="key">key to check for</param>
	/// <returns>true if the map contains that key; false otherwise</returns>
	public boolean ContainsKey(Value key) {
		if (key == null) key = ValNull.instance;
		return map.containsKey(key);
	}
	
	/// <summary>
	/// Get the number of entries in this map.
	/// </summary>
	public int Count() {
		return map.size();
	}
	
	/// <summary>
	/// Return the KeyCollection for this map.
	/// </summary>
	public Set<Value> Keys() {
		return map.keySet();
	}
	
	
	/// <summary>
	/// Accessor to get/set on element of this map by a string key, walking
	/// the __isa chain as needed.  (Note that if you want to avoid that, then
	/// simply look up your value in .map directly.)
	/// </summary>
	/// <param name="identifier">string key to get/set</param>
	/// <returns>value associated with that key</returns>	
	public Value get(String identifier) {
		TempValString idVal = TempValString.Get(identifier);
		Value result = Lookup(idVal);
		TempValString.Release(idVal);
		return result;
	}
	
	public void set(String identifier, Value value) {
		map.put(new ValString(identifier), value);
	}
	
	/// <summary>
	/// Look up the given identifier as quickly as possible, without
	/// walking the __isa chain or doing anything fancy.  (This is used
	/// when looking up local variables.)
	/// </summary>
	/// <param name="identifier">identifier to look up</param>
	/// <returns>true if found, false if not</returns>
	public Pair<Boolean, Value> TryGetValue(String identifier) {
		// , out Value value
		TempValString idVal = TempValString.Get(identifier);
		Pair<Boolean, Value> result = new Pair<>(map.containsKey(idVal), map.get(idVal));
		TempValString.Release(idVal);
		return result;
	}
	
	/// <summary>
	/// Look up a value in this dictionary, walking the __isa chain to find
	/// it in a parent object if necessary.
	/// </summary>
	/// <param name="key">key to search for</param>
	/// <returns>value associated with that key, or null if not found</returns>
	public Value Lookup(Value key) {
		if (key == null) key = ValNull.instance;
		ValMap obj = this;
		while (obj != null) {
			if (obj.map.containsKey(key)) {
				return obj.map.get(key);
			}

			if (obj.map.containsKey(ValString.magicIsA)) {
				obj = (ValMap) obj.map.get(ValString.magicIsA);
			} else {
				break;
			}
		}
		return null;
	}
	
	/// <summary>
	/// Look up a value in this dictionary, walking the __isa chain to find
	/// it in a parent object if necessary; return both the value found and
	/// (via the output parameter) the map it was found in.
	/// </summary>
	/// <param name="key">key to search for</param>
	/// <returns>value associated with that key, or null if not found</returns>
	public Pair<Value, ValMap> LookupPair(Value key) {
		// , out ValMap valueFoundIn
		if (key == null) key = ValNull.instance;
		ValMap obj = this;
		while (obj != null) {
			if (obj.map.containsKey(key)) {
				return new Pair<Value, ValMap>(obj.map.get(key), obj);
			}
			
			if (obj.map.containsKey(ValString.magicIsA)) {
				obj = (ValMap) obj.map.get(ValString.magicIsA);
			} else {
				break;
			}
		}
		
		return new Pair<Value, ValMap>(null, null);
	}
	
	@Override
	public Value FullEval(Context context) throws KeyException, TypeException, IndexException, UndefinedIdentifierException {
		// Evaluate each of our elements, and if any of those is
		// a variable or temp, then resolve those now.
		for (Value k : map.keySet()) {	// TODO: something more efficient here.
			Value key = k;		// stupid C#!
			Value value = map.get(key);
			if (key instanceof ValTemp || key instanceof ValVar) {
				map.remove(key);
				key = key.Val(context);
				map.put(key, value);
			}
			if (value instanceof ValTemp || value instanceof ValVar) {
				map.put(key, value.Val(context));
			}
		}
		return this;
	}

	public ValMap EvalCopy(Context context) throws KeyException, TypeException, IndexException, UndefinedIdentifierException {
		// Create a copy of this map, evaluating its members as we go.
		// This is used when a map literal appears in the source, to
		// ensure that each time that code executes, we get a new, distinct
		// mutable object, rather than the same object multiple times.
		ValMap result = new ValMap();
		for (Value k : map.keySet()) {
			Value key = k;		// stupid C#!
			Value value = map.get(key);
			if (key instanceof ValTemp || key instanceof ValVar) key = key.Val(context);
			if (value instanceof ValTemp || value instanceof ValVar) value = value.Val(context);
			result.map.put(key, value);
		}
		return result;
	}

	@Override
	public String CodeForm(Machine vm, int recursionLimit) {
		if (recursionLimit == 0) return "{...}";
		if (recursionLimit > 0 && recursionLimit < 3 && vm != null) {
			String shortName = vm.FindShortName(this);
			if (shortName != null) return shortName;
		}
		String[] strs = new String[map.size()];
		int i = 0;
		for (Entry<Value, Value> kv : map.entrySet()) {
			int nextRecurLimit = recursionLimit - 1;
			if (kv.getKey() == ValString.magicIsA) nextRecurLimit = 1;
			strs[i++] = String.format("%s: %s", kv.getKey().CodeForm(vm, nextRecurLimit), kv.getValue() == null ? "null" : kv.getValue().CodeForm(vm, nextRecurLimit));
		}
		return "{" + String.join(", ", strs) + "}";
	}

	@Override
	public String toString(Machine vm) {
		return CodeForm(vm, 3);
	}

	@Override
	public boolean IsA(Value type, Machine vm) {
		// If the given type is the magic 'map' type, then we're definitely
		// one of those.  Otherwise, we have to walk the __isa chain.
		if (type == vm.mapType) return true;
		Value p = map.get(ValString.magicIsA);
		while (p != null) {
			if (p == type) return true;
			if (!(p instanceof ValMap)) return false;
			p = ((ValMap) p).map.get(ValString.magicIsA);
		}
		return false;
	}

	@Override
	public int Hash(int recursionDepth) {
		//return map.GetHashCode();
		int result = map.size();
		if (recursionDepth < 0) return result;  // (important to recurse an odd number of times, due to bit flipping)
		for (Entry<Value, Value> kv : map.entrySet()) {
			result ^= kv.getKey().Hash(recursionDepth-1);
			if (kv.getValue() != null) result ^= kv.getValue().Hash(recursionDepth-1);
		}
		return result;
	}

	@Override
	public double Equality(Value rhs, int recursionDepth) {
		if (!(rhs instanceof ValMap)) return 0;
		Map<Value, Value> rhm = ((ValMap)rhs).map;
		if (rhm == map) return 1;  // (same map)
		int count = map.size();
		if (count != rhm.size()) return 0;
		if (recursionDepth < 1) return 0.5;		// in too deep
		double result = 1;
		for (Entry<Value, Value> kv : map.entrySet()) {
			if (!rhm.containsKey(kv.getKey())) return 0;
			Value rhvalue = rhm.get(kv.getKey());
			if (kv.getValue() == null) {
				if (rhvalue != null) return 0;
				continue;
			}
			result *= kv.getValue().Equality(rhvalue, recursionDepth-1);
			if (result <= 0) break;
		}
		return result;
	}

	@Override
	public boolean CanSetElem() {
		return true;
	}

	/// <summary>
	/// Set the value associated with the given key (index).  This is where
	/// we take the opportunity to look for an assignment override function,
	/// and if found, give that a chance to handle it instead.
	/// </summary>
	@Override
	public void SetElem(Value index, Value value) {
		if (index == null) index = ValNull.instance;
		if (assignOverride == null || !assignOverride.run(index, value)) {
			map.put(index, value);
		}
	}

	/// <summary>
	/// Get the indicated key/value pair as another map containing "key" and "value".
	/// (This is used when iterating over a map with "for".)
	/// </summary>
	/// <param name="index">0-based index of key/value pair to get.</param>
	/// <returns>new map containing "key" and "value" with the requested key/value pair</returns>
	public ValMap GetKeyValuePair(int index) throws IndexException {
		Set<Value> keys = map.keySet();
		if (index < 0 || index >= keys.size()) {
			throw new IndexException("index " + index + " out of range for map");
		}
		Value key = SetUtil.ElementAt(keys, index);
		ValMap result = new ValMap();
		result.map.put(keyStr, (key instanceof ValNull ? null : key));
		result.map.put(valStr, map.get(key));
		return result;
	}
	
	static ValString keyStr = new ValString("key");
	static ValString valStr = new ValString("value");
}
