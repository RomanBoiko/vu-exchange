package vu.exchange;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.Test;
import org.junit.Ignore;

public class PerformanceTest {

	private static final Long TEST_VALUE = 12345678910111213L;

	@Test
	@Ignore
	public void hashOnLongShouldBeMorePerformantThanOnStringAndOnUuid() {
		final Long longUnderTest = Long.valueOf(TEST_VALUE);
		final BigDecimal bigDecimalUnderTest = BigDecimal.valueOf(TEST_VALUE);
		final String stringUnderTest = TEST_VALUE.toString();
		final UUID uuidUnderTest = UUID.randomUUID();
		Long timeForLong = timeForMillionExecutions(new Callback() {public void execute() { longUnderTest.hashCode(); }});
		Long timeForBigDecimal = timeForMillionExecutions(new Callback() {public void execute() { bigDecimalUnderTest.hashCode(); }});
		Long timeForString = timeForMillionExecutions(new Callback() {public void execute() { stringUnderTest.hashCode(); }});
		Long timeForUuid = timeForMillionExecutions(new Callback() {public void execute() { uuidUnderTest.hashCode(); }});

		assertThat(timeForString, lessThanOrEqualTo(timeForUuid));
		assertThat(timeForUuid, lessThan(timeForLong));
		assertThat(timeForLong, lessThan(timeForBigDecimal));
	}

	private long timeForMillionExecutions(Callback callback) {
		long startTime = System.currentTimeMillis();
		for (int i = 0; i < 1000000; i++) {
			callback.execute();
		}
		return System.currentTimeMillis() - startTime;
	}

	private static interface Callback {
		void execute();
	}
}
