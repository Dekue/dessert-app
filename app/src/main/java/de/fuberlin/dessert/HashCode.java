package de.fuberlin.dessert;

import java.lang.reflect.Array;

/**
 * Methods implementing hashCode() based on "Effective Java" by Joshua Bloch.
 */
public final class HashCode {

	public static final int SEED = 17;
	private static final int PRIME = 31;

	// used: int, long
	public static int hash(int seed , int f) {
		return PRIME * seed + f;
	}

	/* UNUSED: boolean, byte(, char, short), long, float, double
	public static int hash(int seed, boolean f) {
		return PRIME * seed + (f ? 1 : 0);
	}

	public static int hash(int seed, byte f) {
		return PRIME * seed + (int)f;
	}

	public static int hash(int seed , long f) {
		return PRIME * seed + (int)(f ^ (f >>> 32));
	}

	public static int hash(int seed , float f) {
		return hash(seed, Float.floatToIntBits(f));
	}

	public static int hash(int seed , double f) {
		return hash(seed, Double.doubleToLongBits(f));
	}
	*/

	/**
	 * f is an array or a possibly-null object field;
	 * if f is an array the array elements consist of primitives or possibly-null objects
	 */
	public static int hash(int seed , Object f) {
		int result = seed;
		if (f == null) {
			result = hash(result, 0);
		}
		else if (!isArray(f)) {
			result = hash(result, f.hashCode());
		}
		else {
			// check if array element is not referencing f itself and call hash recursively
			for (int i = 0; i < Array.getLength(f); ++i) {
				Object item = Array.get(f, i);
				if(!(item == f)) {
					result = hash(result, item);
				}
			}
		}
		return result;
	}

	private static boolean isArray(Object f) {
		return f.getClass().isArray();
	}
}
