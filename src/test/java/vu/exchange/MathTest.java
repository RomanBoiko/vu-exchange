package vu.exchange;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

public class MathTest {

	@Test
	public void shouldProducePowerOfLongBase() {
		assertThat(Math.power(10L, 3), is(1000L));
		assertThat(Math.power(2L, 3), is(8L));
		assertThat(Math.power(2L, 1), is(2L));
		assertThat(Math.power(2L, 0), is(1L));
	}

}
