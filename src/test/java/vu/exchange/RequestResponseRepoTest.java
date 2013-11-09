package vu.exchange;

import static org.junit.Assert.assertSame;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import vu.exchange.RequestResponseRepo.RequestDTO;
import vu.exchange.RequestResponseRepo.ResponseDTO;

public class RequestResponseRepoTest {

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void shouldBeAbleToLockRequestOnlyOnce() throws Exception {
		exception.expect(IllegalStateException.class);
		exception.expectMessage("Request already locked [can be locked only once]");
		RequestResponseRepo repo = new RequestResponseRepo();
		final Request request = new Login();
		final RequestDTO requestDto = repo.request(request);
		Thread thread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					requestDto.waitForResponse();
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		try{
			thread.start();
			Thread.sleep(10);
			requestDto.waitForResponse();
		} finally {
			Response response = new LoginResult();
			repo.respond(new ResponseDTO(requestDto, response));
			thread.join();
		}
	}
	
	@Test
	public void shouldBeAbleToProceedExecutionAfterRespondMethodCalledAndMakeResponseAvailableStraightAway() throws Exception {
		final RequestResponseRepo repo = new RequestResponseRepo();
		final Request request = new Login();
		final Response response = new LoginResult();
		final RequestDTO requestDto = repo.request(request);
		final Thread thread = new Thread(new Runnable() {
			public void run() {
				try {
					Thread.sleep(10);
					repo.respond(new ResponseDTO(requestDto, response));
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}
		});
		thread.start();
		requestDto.waitForResponse();
		Object actualResponse = repo.getResponse(requestDto);
		thread.join();
		assertSame(response, actualResponse);
	}
}
