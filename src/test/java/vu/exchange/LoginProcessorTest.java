package vu.exchange;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;

import vu.exchange.AddCreditResult.AddCreditStatus;
import vu.exchange.LoginProcessor.UserSession;
import vu.exchange.LoginResult.LoginStatus;
import vu.exchange.Order.Currency;
import vu.exchange.UserRegistrationResult.UserRegistrationStatus;
import vu.exchange.WithdrawResult.WithdrawStatus;

public class LoginProcessorTest {
	private static final String EMAIL = "email";
	private static final String PASSWORD = "pass";
	private static final String SYSTEM_USER_NAME = "sysuser";
	private static final String SYSTEM_USER_PASSWORD = "sysuserpass";

	private LoginProcessor loginProcessor;

	@Before
	public void setUp() {
		loginProcessor = new LoginProcessor().withSystemUserName(SYSTEM_USER_NAME).withSystemUserPassword(SYSTEM_USER_PASSWORD);
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
	public void shouldAddCreditToAccount() {
		AddCreditResult addCreditResult = loginProcessor.addCredit(new AddCreditRequest().withEmail(EMAIL).withAmount(1.2));
		assertThat(addCreditResult.status, is(AddCreditStatus.SUCCESS));
		assertThat(addCreditResult.amount, is(BigDecimal.valueOf(1.2)));
	}

	@Test
	public void shouldNotWithdrawFromAccountWithNotEnoughMoneyOnIt() {
		WithdrawResult withdrawResult = loginProcessor.withdraw(new WithdrawRequest().withEmail(EMAIL).withAmount(1.2));
		assertThat(withdrawResult.status, is(WithdrawStatus.FAILURE_ACCOUNT_CREDIT_LOW));
		assertThat(withdrawResult.amount, is(BigDecimal.valueOf(1.2)));
	}

	@Test
	public void shouldWithdrawFromAccount() {
		loginProcessor.addCredit(new AddCreditRequest().withEmail(EMAIL).withAmount(1.2));
		WithdrawResult withdrawResult = loginProcessor.withdraw(new WithdrawRequest().withEmail(EMAIL).withAmount(1.2));
		assertThat(withdrawResult.status, is(WithdrawStatus.SUCCESS));
		assertThat(withdrawResult.amount, is(BigDecimal.valueOf(1.2)));
	}
	
	@Test
	public void shouldReturnCorrectInitialAccountState() {
		AccountState accountState = loginProcessor.accountState(new AccountStateRequest().withEmail(EMAIL));
		assertThat(accountState.exposure, is(BigDecimal.ZERO));
		assertThat(accountState.credit, is(BigDecimal.ZERO));
		assertThat(accountState.currency, is(Currency.GBP));
	}
	
	@Test
	public void shouldReturnCorrectAccountStateAfterAddingCredit() {
		loginProcessor.addCredit(new AddCreditRequest().withEmail(EMAIL).withAmount(1.2));
		AccountState accountState = loginProcessor.accountState(new AccountStateRequest().withEmail(EMAIL));
		assertThat(accountState.credit, is(BigDecimal.valueOf(1.2)));
	}

	@Test
	public void shouldReturnCorrectAccountStateAfterWithdrawal() {
		loginProcessor.addCredit(new AddCreditRequest().withEmail(EMAIL).withAmount(1.2));
		loginProcessor.withdraw(new WithdrawRequest().withEmail(EMAIL).withAmount(1.1));
		AccountState accountState = loginProcessor.accountState(new AccountStateRequest().withEmail(EMAIL));
		assertThat(accountState.credit, is(BigDecimal.valueOf(0.1)));
	}

	@Test
	public void shouldReturnUniqueSessionIds() {
		LoginResult loginResult1 = loginProcessor.login(new Login().withEmail(EMAIL).withPassword(PASSWORD));
		loginProcessor.registerUser(new UserRegistrationRequest().withEmail(EMAIL + 1).withPassword(PASSWORD + 1));
		LoginResult loginResult2 = loginProcessor.login(new Login().withEmail(EMAIL + 1).withPassword(PASSWORD + 1));
		assertThat(loginResult1.sessionId, is(not(loginResult2.sessionId)));
	}

	@Test
	public void shouldReportValidSessionOfUser() {
		LoginResult loginResult = loginProcessor.login(new Login().withEmail(EMAIL).withPassword(PASSWORD));
		AuthenticatedRequest authenticatedRequest = new Order().withSessionId(loginResult.sessionId);
		UserSession session = loginProcessor.sessionDetails(authenticatedRequest);
		assertThat(session.isSessionValid, is(true));
		assertThat(session.hasSystemAdminPermissions, is(false));
	}

	@Test
	public void shouldReportInvalidSessionOfUser() {
		AuthenticatedRequest authenticatedRequest = new Order().withSessionId("unexistingSessionId");
		UserSession session = loginProcessor.sessionDetails(authenticatedRequest);
		assertThat(session.isSessionValid, is(false));
		assertThat(session.hasSystemAdminPermissions, is(false));
	}

	@Test
	public void shouldReportSessionOfSystemAdmin() {
		LoginResult loginResult = loginProcessor.login(new Login().withEmail(SYSTEM_USER_NAME).withPassword(SYSTEM_USER_PASSWORD));
		AuthenticatedRequest authenticatedRequest = new Order().withSessionId(loginResult.sessionId);
		UserSession session = loginProcessor.sessionDetails(authenticatedRequest);
		assertThat(session.isSessionValid, is(true));
		assertThat(session.hasSystemAdminPermissions, is(true));
	}
}
