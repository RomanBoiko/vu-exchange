package vu.exchange;

import java.util.Map;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.map.ObjectMapper;

public class JsonConverter <T> {

	private final Map<String, Class<? extends T>> typeToClass;

	public JsonConverter(Map<String, Class<? extends T>> typeToClass) {
		this.typeToClass = typeToClass;
	}

	T fromJson(String json) throws Exception {
		JsonFactory jsonFactory = new JsonFactory();
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(
				jsonFactory.createJsonParser(json),
				typeToClass.get(
						mapper.readTree(jsonFactory.createJsonParser(json)).get("type").getTextValue()));
	}

	public String toJson(T object) throws Exception {
		return new ObjectMapper().writeValueAsString(object);
	}
}
