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
	final Long clientId;
	final Long marketId;
	final Cash price;
	final BigDecimal quantity;
	final BuySell buySell;

	public Order(Long clientId, Long marketId, Cash price, BigDecimal quantity, BuySell buySell) {
		Long timestamp = System.currentTimeMillis();
		this.id = (new Random().nextInt(ID_PREFIX_LIMIT) * SUFFIX_MULTIPLIER) + timestamp;
		this.clientId = clientId;
		this.marketId = marketId;
		this.price = price;
		this.quantity = quantity;
		this.buySell = buySell;
	}

	Order() {
		this(0L, 0L, null, BigDecimal.valueOf(0), BuySell.BUY);
	}

	Long timestamp() {
		return id % SUFFIX_MULTIPLIER;
	}
}
