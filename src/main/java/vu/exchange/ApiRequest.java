package vu.exchange;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Random;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

public abstract class ApiRequest {
	static final int ID_MAX_PREFIX_LENGTH = 5;
	static final Integer ID_PREFIX_LIMIT = Math.power(10L, ID_MAX_PREFIX_LENGTH).intValue();
	
	static final int ID_SUFFIX_LENGTH = 13;
	static final Long SUFFIX_MULTIPLIER = Math.power(10L, ID_SUFFIX_LENGTH);


	static long requestId(Date timestamp) {
		return (new Random().nextInt(ID_PREFIX_LIMIT) * SUFFIX_MULTIPLIER) + timestamp.getTime();
	}

	static ApiRequest fromJson(String json) throws Exception {
		JsonFactory f = new JsonFactory();
		JsonParser parser = f.createJsonParser(json);
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(parser, Order.class);
	}

	public Long id;
	public String sessionId;
	public Long client;

	ApiRequest withArrivalTimestamp(Date timestamp) {
		this.id = requestId(timestamp);
		return this;
	}

	Long timestamp() {
		return id % SUFFIX_MULTIPLIER;
	}
}

class Order extends ApiRequest {
	static enum Position { BUY, SELL }
	static enum Currency { GBP }

	public Long market;
	public BigDecimal price;
	public Currency currency;
	public BigDecimal quantity;
	public Position position;
}
