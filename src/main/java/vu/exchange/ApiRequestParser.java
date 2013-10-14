package vu.exchange;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Random;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

public class ApiRequestParser {
	Object fromJson(String json) {
		return new Object();
	}
}

class Order {
	static enum Position { BUY, SELL }
	static enum Currency { GBP }

	static final int ID_MAX_PREFIX_LENGTH = 5;
	private static final Integer ID_PREFIX_LIMIT = Math.power(10L, ID_MAX_PREFIX_LENGTH).intValue();

	static final int ID_SUFFIX_LENGTH = 13;
	private static final Long SUFFIX_MULTIPLIER = Math.power(10L, ID_SUFFIX_LENGTH);

	public Long id;
	public String sessionId;
	public Long client;
	public Long market;
	public BigDecimal price;
	public Currency currency;
	public BigDecimal quantity;
	public Position position;

	Order withArrivalTimestamp(Date timestamp) {
		this.id = (new Random().nextInt(ID_PREFIX_LIMIT) * SUFFIX_MULTIPLIER) + timestamp.getTime();
		return this;
	}

	Long timestamp() {
		return id % SUFFIX_MULTIPLIER;
	}

	static Order fromJson(String json) throws Exception {
		JsonFactory f = new JsonFactory();
		JsonParser parser = f.createJsonParser(json);
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(parser, Order.class);
	}
}
