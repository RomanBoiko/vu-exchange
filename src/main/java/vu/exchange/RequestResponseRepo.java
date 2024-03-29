package vu.exchange;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RequestResponseRepo {
	private ConcurrentMap<UUID, RequestResponsePair> repo = new ConcurrentHashMap<UUID, RequestResponsePair>();

	RequestDTO request(Request request) {
		RequestResponsePair reqRespPair = new RequestResponsePair(request);
		repo.put(reqRespPair.key, reqRespPair);
		return new RequestDTO(reqRespPair);
	}
	
	void respond(ResponseDTO responseDto) {
		RequestResponsePair reqRespPair = repo.get(responseDto.key);
		reqRespPair.response = responseDto.response;
		synchronized (reqRespPair.lock) {
			reqRespPair.lock.notify();
		}
	}

	Response getResponse(RequestDTO requestDTO) {
		return repo.remove(requestDTO.key).response;
	}

	static class RequestResponsePair {
		private final UUID key = UUID.randomUUID();
		private final Object lock = new Object();
		private final Request request;
		Response response = null;
		RequestResponsePair(Request request) {
			this.request = request;
		}
	}
	
	static class RequestDTO {
		private final UUID key;
		private final Object lock;
		private boolean alreadyLocked = false;
		final Request request;
		RequestDTO(RequestResponsePair requestResponsePair) {
			this.key = requestResponsePair.key;
			this.request = requestResponsePair.request;
			this.lock = requestResponsePair.lock;
		}

		void waitForResponse() throws Exception {
			synchronized (lock) {
				if(alreadyLocked) {
					throw new IllegalStateException("Request already locked [can be locked only once]");
				}
				alreadyLocked = true;
				lock.wait();
			}
		}
	}

	static class ResponseDTO {
		final UUID key;
		final Response response;
		ResponseDTO(RequestDTO request, Response response) {
			this.key = request.key;
			this.response = response;
		}
	}
}
