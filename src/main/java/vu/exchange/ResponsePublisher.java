package vu.exchange;

import org.apache.log4j.Logger;

import vu.exchange.RequestResponseRepo.ResponseDTO;

import com.lmax.disruptor.EventHandler;

public class ResponsePublisher implements EventHandler<ValueEvent>{
	private final Logger log = Logger.getLogger(this.getClass());
	private final RequestResponseRepo requestResponseRepo;
	public ResponsePublisher(RequestResponseRepo requestResponseRepo){
		this.requestResponseRepo = requestResponseRepo;
	}

	@Override
	public void onEvent(ValueEvent event, long sequence, boolean endOfBatch) throws Exception {
		ResponseDTO responseDto = (ResponseDTO)event.getValue();
		log.debug(String.format("Pushing response to client: %s", responseDto));
		requestResponseRepo.respond(responseDto);
	}
}
