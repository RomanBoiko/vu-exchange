package vu.exchange;

import static java.lang.String.format;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import vu.exchange.RequestResponseRepo.RequestDTO;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

class RequestHandler {
	static final String INPUT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss:SSSz";

	private final Logger log = Logger.getLogger(this.getClass());

	private final File incomingEventsFile;
	private final DateFormat dateFormat = new SimpleDateFormat(INPUT_DATE_FORMAT);
	private final DisruptorBridge eventProcessor;
	private final RequestResponseRepo requestResponseRepo;

	private RequestHandler(RequestResponseRepo requestResponseRepo, File incomingEventsFile, DisruptorBridge eventProcessor) {
		this.incomingEventsFile = incomingEventsFile;
		this.eventProcessor = eventProcessor;
		this.requestResponseRepo = requestResponseRepo;
	}

	String getResponseMessage(String requestMessage) throws Exception {
		Date arrivalTime = new Date(System.currentTimeMillis());
		log.debug("Request received: " + requestMessage);
		Request apiRequest = Request.fromJson(requestMessage);
		apiRequest.withArrivalTimestamp(arrivalTime);
		persistIncomingEvent(arrivalTime, requestMessage);
		log.debug("Request persisted");
		RequestDTO requestDto = requestResponseRepo.request(apiRequest);
		eventProcessor.process(requestDto);
		log.debug("Request forwarded for further processing");
		requestDto.waitForResponse();
		return requestResponseRepo.getResponse(requestDto).toJson();
	}

	private void persistIncomingEvent(Date arrivalTime, String message) throws Exception {
		Files.append(format("%s=%s\n", dateFormat.format(arrivalTime), message), incomingEventsFile, Charsets.UTF_8);
	}

	static class RequestHandlerFactory {
		private final RequestResponseRepo requestResponseRepo;
		private final File incomingEventsFile;
		private final DisruptorBridge eventProcessor;

		RequestHandlerFactory(RequestResponseRepo repo, File eventsFile, DisruptorBridge eventProcessor) {
			this.requestResponseRepo = repo;
			this.incomingEventsFile = eventsFile;
			this.eventProcessor = eventProcessor;
			if(!incomingEventsFile.getParentFile().exists()) {
				incomingEventsFile.getParentFile().mkdirs();
			}
		}

		RequestHandler createHandler() {
			return new RequestHandler(requestResponseRepo, incomingEventsFile, eventProcessor);
		}
	}
}