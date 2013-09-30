package vu.exchange;

import java.math.BigDecimal;

public class Cash {
	static enum Currency { GBP }

	Currency currency = Currency.GBP;
	BigDecimal amount;

	Cash setCurrency(Currency currency) {
		this.currency = currency;
		return this;
	}

	Cash setAmount(BigDecimal amount) {
		this.amount = amount;
		return this;
	}
}
