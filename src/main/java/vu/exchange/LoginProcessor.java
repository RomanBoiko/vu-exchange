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
import com.google.common.collect.ImmutableMap;

class LoginProcessor {
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

	private final Map<String, UserDetails> email2Details = new HashMap<String, UserDetails>(
			new ImmutableMap.Builder<String, UserDetails>()
				.put("user1@smarkets.com", new UserDetails().withPassword("pass1"))
				.build());
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
		UserDetails existingUserDetails = email2Details.get(login.email);
		if(null == existingUserDetails) {
			return new LoginResult().withStatus(NO_SUCH_USER);
		} else if (existingUserDetails.password.equals(login.password)) {
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
}