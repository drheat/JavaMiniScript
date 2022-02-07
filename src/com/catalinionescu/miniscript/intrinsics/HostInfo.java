package com.catalinionescu.miniscript.intrinsics;

/// <summary>
/// Information about the app hosting MiniScript.  Set this in your main program.
/// This is provided to the user via the `version` intrinsic.
/// </summary>
public class HostInfo {
	public static String name;		// name of the host program
	public static String info;		// URL or other short info about the host
	public static double version;	// host program version number
}
