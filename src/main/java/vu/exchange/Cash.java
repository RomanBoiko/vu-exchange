package vu.exchange;

import java.math.BigDecimal;

public class Cash {
	static enum Currency { GBP }

	final Currency currency;
	final BigDecimal amount;

	public Cash(Currency currency, BigDecimal amount) {
		this.currency = currency;
		this.amount = amount;
	}
}
