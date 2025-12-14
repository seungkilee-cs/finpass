package com.finpass.issuer.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public final class CanonicalJson {

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

	private CanonicalJson() {
	}

	public static String stringify(Object value) {
		try {
			return MAPPER.writeValueAsString(value);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to canonicalize JSON", e);
		}
	}
}
