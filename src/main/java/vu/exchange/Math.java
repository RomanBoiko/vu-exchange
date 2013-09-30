package vu.exchange;

public class Math {
	static Long power(Long base, Integer power) {
		if (power == 0L) {
			return 1L;
		}
		Long result = base;
		for (int p = 2; p <= power; p++) {
			result = result * base;
		}
		return result;
	}
}
