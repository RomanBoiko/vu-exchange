package vu.exchange;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.lmax.disruptor.EventFactory;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.YieldingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

public class DisruptorBridge {
	private Disruptor<ValueEvent> disruptor;
	private ExecutorService exec;
	private final EventHandler<ValueEvent> handler;
	private final ProducerType producerType;
	private final WaitStrategy waitStrategy;
	private DisruptorBridge(EventHandler<ValueEvent> eventHandler, ProducerType producerType, WaitStrategy waitStrategy) {
		this.handler = eventHandler;
		this.producerType = producerType;
		this.waitStrategy = waitStrategy;
	}

	static DisruptorBridge singleProducerMultipleConsumers(EventHandler<ValueEvent> eventHandler) {
		//to test and fine appropriate waiting strategy
		return new DisruptorBridge(eventHandler, ProducerType.SINGLE, new YieldingWaitStrategy());
	}

	static DisruptorBridge multipleProducersSingleConsumer(EventHandler<ValueEvent> eventHandler) {
		//to test and fine appropriate waiting strategy
		return new DisruptorBridge(eventHandler, ProducerType.MULTI, new YieldingWaitStrategy());
	}

	@SuppressWarnings("unchecked")
	DisruptorBridge start() {
		exec = Executors.newSingleThreadExecutor();
		int ringBufferPreallocatedEventsNumber = 1024;
		disruptor = new Disruptor<ValueEvent>(ValueEvent.EVENT_FACTORY, ringBufferPreallocatedEventsNumber, exec, producerType, waitStrategy);
		disruptor.handleEventsWith(handler);
		disruptor.start();
		return this;
	}
	
	public void process(Object event) {
		disruptor.publishEvent(new ValueEventTranslator(), event);
	}

	void stop() {
		disruptor.shutdown();
		exec.shutdown();
	}

	static class ValueEventTranslator implements EventTranslatorOneArg<ValueEvent, Object> {
		@Override
		public void translateTo(ValueEvent valueEvent, long sequence, Object event) {
			valueEvent.setValue(event);
		}
	}
}


class ValueEvent {
	private Object event;

	public Object getValue() {
		return event;
	}

	public ValueEvent setValue(Object event) {
		this.event = event;
		return this;
	}

	public static final EventFactory<ValueEvent> EVENT_FACTORY = new EventFactory<ValueEvent>() {
		public ValueEvent newInstance() {
			return new ValueEvent();
		}
	};
}
