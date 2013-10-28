package vu.exchange;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.lmax.disruptor.EventHandler;

@RunWith(MockitoJUnitRunner.class)
public class ExchangeDisruptorTest {

	@Mock
	private EventHandler<ValueEvent> handler;

	@Test
	public void shouldProcessEventsConcurrently() throws Exception {
		DisruptorWrapper disruptor = DisruptorWrapper.multipleProducersSingleConsumer(handler);
		disruptor.start();
		for (long i = 0; i < 20; i++) {
			disruptor.process(i);
		}
		Thread.sleep(20);
		disruptor.stop();
		verify(handler, times(20)).onEvent(any(ValueEvent.class), any(Long.class), any(Boolean.class));
	}
}
