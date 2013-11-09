package vu.exchange;

import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import vu.exchange.RequestResponseRepo.RequestDTO;
import vu.exchange.RequestResponseRepo.RequestResponsePair;
import vu.exchange.RequestResponseRepo.ResponseDTO;

@RunWith(MockitoJUnitRunner.class)
public class ResponsePublisherTest {

	@Mock RequestResponseRepo repo;

	@Test
	public void shouldPublishResponseEventToRepo() throws Exception {
		ResponseDTO response = new ResponseDTO(new RequestDTO(new RequestResponsePair(null)), null);
		ValueEvent responseEvent = new ValueEvent().setValue(response);
		new ResponsePublisher(repo).onEvent(responseEvent, 1L, true);
		verify(repo).respond(response);
	}

}
