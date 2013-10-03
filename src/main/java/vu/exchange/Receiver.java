package vu.exchange;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.map.ObjectMapper;

public class Receiver {
	static Order orderFromString(String json) {
		try {
			JsonFactory f = new JsonFactory();
			JsonParser parser = f.createJsonParser(json);
			ObjectMapper mapper = new ObjectMapper();
			return mapper.readValue(parser, Order.class);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
