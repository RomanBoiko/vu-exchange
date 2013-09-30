package vu.exchange;

import java.math.BigDecimal;
import java.util.Random;

public class Order {
	static enum BuySell { BUY, SELL }

	static final int ID_MAX_PREFIX_LENGTH = 5;
	private static final Integer ID_PREFIX_LIMIT = Math.power(10L, ID_MAX_PREFIX_LENGTH).intValue();

	static final int ID_SUFFIX_LENGTH = 13;
	private static final Long SUFFIX_MULTIPLIER = Math.power(10L, ID_SUFFIX_LENGTH);

	final Long id;
	Long clientId;
	Cash price;
	BigDecimal quantity;
	BuySell buySell;

	Order() {
		Long timestamp = System.currentTimeMillis();
		this.id = (new Random().nextInt(ID_PREFIX_LIMIT) * SUFFIX_MULTIPLIER) + timestamp;
	}

	Long timestamp() {
		return id % SUFFIX_MULTIPLIER;
	}

	Order setClientId(Long clientId) {
		this.clientId = clientId;
		return this;
	}

	Order setPrice(Cash price) {
		this.price = price;
		return this;
	}

	Order setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
		return this;
	}

	Order setBuySell(BuySell buySell) {
		this.buySell = buySell;
		return this;
	}
}
