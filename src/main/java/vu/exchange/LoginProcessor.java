package vu.exchange;

import static vu.exchange.LoginResult.LoginStatus.ALREADY_LOGGED_IN;
import static vu.exchange.LoginResult.LoginStatus.NO_SUCH_USER;
import static vu.exchange.LoginResult.LoginStatus.OK;
import static vu.exchange.LoginResult.LoginStatus.WRONG_PASSWORD;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import vu.exchange.UserRegistrationResult.UserRegistrationStatus;
import vu.exchange.WithdrawResult.WithdrawStatus;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

class LoginProcessor {
	private String systemUserName;
	private String systemUserPassword;

	private static class UserDetails {
		public String password;
		private BigDecimal credit = BigDecimal.ZERO;
		public BigDecimal credit() {return credit; }

		UserDetails withPassword(String password) {this.password = password; return this; }
		public void addCredit(BigDecimal additionalCredit) {
			this.credit = this.credit.add(additionalCredit);
		}
		public boolean withdraw(BigDecimal withdrawalAmount) {
			if(credit.compareTo(withdrawalAmount) < 0) {
				return false;
			}
			this.credit = this.credit.subtract(withdrawalAmount);
			return true;
		}
	}

	private final Map<String, UserDetails> email2Details = new HashMap<String, UserDetails>();
	private BiMap<String, String> sessionToEmail = HashBiMap.create();

	UserRegistrationResult registerUser(UserRegistrationRequest userRegistrationRequest) {
		UserDetails existingUserDetails = email2Details.get(userRegistrationRequest.email);
		if(null == existingUserDetails) {
			email2Details.put(userRegistrationRequest.email, new UserDetails().withPassword(userRegistrationRequest.password));
			return registrationResult(userRegistrationRequest, UserRegistrationStatus.REGISTERED);
		} else if(! existingUserDetails.password.equals(userRegistrationRequest.password)){
			existingUserDetails.withPassword(userRegistrationRequest.password);
			return registrationResult(userRegistrationRequest, UserRegistrationStatus.PASSWORD_UPDATED);
		} else {
			return registrationResult(userRegistrationRequest, UserRegistrationStatus.UNCHANGED);
		}
	}

	private UserRegistrationResult registrationResult(UserRegistrationRequest userRegistrationRequest, UserRegistrationStatus status) {
		return new UserRegistrationResult().withStatus(status).withEmail(userRegistrationRequest.email);
	}

	AccountState accountState(AccountStateRequest request) {
		UserDetails details = email2Details.get(request.email);
		return new AccountState().withCredit(details.credit());
	}

	LoginResult login(Login login) {
		if(systemUserName.equals(login.email)) {
			return loginSystemUser(login);
		}
		UserDetails existingUserDetails = email2Details.get(login.email);
		if(null == existingUserDetails) {
			return new LoginResult().withStatus(NO_SUCH_USER);
		} else if (existingUserDetails.password.equals(login.password)) {
			return onCorrectCredentials(login);
		} else {
			return new LoginResult().withStatus(WRONG_PASSWORD);
		}
	}

	private LoginResult loginSystemUser(Login login) {
		if(systemUserPassword.equals(login.password)) {
			return onCorrectCredentials(login);
		} else {
			return new LoginResult().withStatus(WRONG_PASSWORD);
		}
	}

	private LoginResult onCorrectCredentials(Login login) {
		String existingSessionId = sessionToEmail.inverse().get(login.email);
		if(existingSessionId == null) {
			return loggedInSuccessfuly(login);
		} else {
			return new LoginResult().withStatus(ALREADY_LOGGED_IN).withSessionId(existingSessionId);
		}
	}

	private LoginResult loggedInSuccessfuly(Login login) {
		String sessionId = UUID.randomUUID().toString();
		sessionToEmail.put(sessionId, login.email);
		return new LoginResult().withStatus(OK).withSessionId(sessionId);
	}

	public AddCreditResult addCredit(AddCreditRequest request) {
		this.email2Details.get(request.email).addCredit(request.amount);
		return new AddCreditResult().withAmount(request.amount);
	}

	public WithdrawResult withdraw(WithdrawRequest request) {
		if(this.email2Details.get(request.email).withdraw(request.amount)) {
			return new WithdrawResult().withAmount(request.amount).withWithdrawStatus(WithdrawStatus.SUCCESS);
		}
		return new WithdrawResult().withAmount(request.amount).withWithdrawStatus(WithdrawStatus.FAILURE_ACCOUNT_CREDIT_LOW);
	}

	LoginProcessor withSystemUserName(String systemUserName) {
		this.systemUserName = systemUserName;
		return this;
	}

	LoginProcessor withSystemUserPassword(String systemUserPassword) {
		this.systemUserPassword = systemUserPassword;
		return this;
	}

	public UserSession sessionDetails(AuthenticatedRequest authenticatedRequest) {
		String userEmail = sessionToEmail.get(authenticatedRequest.sessionId);
		if(userEmail == null) {
			return UserSession.nonValidSession();
		} else if (userEmail.equals(systemUserName)) {
			return UserSession.systemUserSession();
		} else {
			return UserSession.validUserSession();
		}
	}

	static class UserSession {
		final Boolean isSessionValid;
		final Boolean hasSystemAdminPermissions;
		private UserSession(Boolean isSessionValid, Boolean hasSystemAdminPermissions) {
			this.isSessionValid = isSessionValid;
			this.hasSystemAdminPermissions = hasSystemAdminPermissions;
		}
		static UserSession nonValidSession() {
			return new UserSession(false, false);
		}
		static UserSession systemUserSession() {
			return new UserSession(true, true);
		}
		static UserSession validUserSession() {
			return new UserSession(true, false);
		}
	} 
}