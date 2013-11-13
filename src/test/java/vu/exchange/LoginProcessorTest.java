package vu.exchange;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import vu.exchange.LoginResult.LoginStatus;
import vu.exchange.UserRegistrationResult.UserRegistrationStatus;

public class LoginProcessorTest {
	private static final String EMAIL = "email";
	private static final String PASSWORD = "pass";

	private LoginProcessor loginProcessor;

	@Before
	public void setUp() {
		loginProcessor = new LoginProcessor();
		loginProcessor.registerUser(new UserRegistrationRequest().withEmail(EMAIL).withPassword(PASSWORD));
	}

	@Test
	public void shouldRegisterNewUser() {
		UserRegistrationResult registrationResult = loginProcessor.registerUser(new UserRegistrationRequest().withEmail(EMAIL + 1).withPassword(PASSWORD));
		assertThat(registrationResult.registrationStatus, is(UserRegistrationStatus.REGISTERED));
		assertThat(registrationResult.email, is(EMAIL + 1));
	}

	@Test
	public void shouldUpdatePasswordForExistingUser() {
		UserRegistrationResult registrationResult = loginProcessor.registerUser(new UserRegistrationRequest().withEmail(EMAIL).withPassword(PASSWORD + 1));
		assertThat(registrationResult.registrationStatus, is(UserRegistrationStatus.PASSWORD_UPDATED));
		assertThat(registrationResult.email, is(EMAIL));
	}

	@Test
	public void shouldLeaveExistingUserUnchanged() {
		UserRegistrationResult registrationResult = loginProcessor.registerUser(new UserRegistrationRequest().withEmail(EMAIL).withPassword(PASSWORD));
		assertThat(registrationResult.registrationStatus, is(UserRegistrationStatus.UNCHANGED));
		assertThat(registrationResult.email, is(EMAIL));
	}

	@Test
	public void shouldLoginExistingUser() {
		LoginResult loginResult = loginProcessor.login(new Login().withEmail(EMAIL).withPassword(PASSWORD));
		assertThat(loginResult.status, is(LoginStatus.OK));
		assertThat(loginResult.sessionId, notNullValue());
	}

	@Test
	public void shouldReturnWrongPassword() {
		LoginResult loginResult = loginProcessor.login(new Login().withEmail(EMAIL).withPassword(PASSWORD + 1));
		assertThat(loginResult.status, is(LoginStatus.WRONG_PASSWORD));
		assertThat(loginResult.sessionId, nullValue());
	}

	@Test
	public void shouldReturnAlreadyLoggedInResult() {
		loginProcessor.login(new Login().withEmail(EMAIL).withPassword(PASSWORD));
		LoginResult secondLoginResult = loginProcessor.login(new Login().withEmail(EMAIL).withPassword(PASSWORD));
		assertThat(secondLoginResult.status, is(LoginStatus.ALREADY_LOGGED_IN));
		assertThat(secondLoginResult.sessionId, notNullValue());
	}

	@Test
	public void shouldReturnUserNotExist() {
		LoginResult loginResult = loginProcessor.login(new Login().withEmail(EMAIL + 1).withPassword(PASSWORD));
		assertThat(loginResult.status, is(LoginStatus.NO_SUCH_USER));
		assertThat(loginResult.sessionId, nullValue());
	}

	@Test
	public void shouldReturnUniqueSessionIds() {
		LoginResult loginResult1 = loginProcessor.login(new Login().withEmail(EMAIL).withPassword(PASSWORD));
		loginProcessor.registerUser(new UserRegistrationRequest().withEmail(EMAIL + 1).withPassword(PASSWORD + 1));
		LoginResult loginResult2 = loginProcessor.login(new Login().withEmail(EMAIL + 1).withPassword(PASSWORD + 1));
		assertThat(loginResult1.sessionId, is(not(loginResult2.sessionId)));
	}
}
