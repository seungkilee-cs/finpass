package com.finpass.verifier.service;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ChallengeStore {

	private static final class Entry {
		private final Instant expiresAt;
		private boolean used;

		private Entry(Instant expiresAt) {
			this.expiresAt = expiresAt;
			this.used = false;
		}
	}

	private final Map<String, Entry> challenges = new ConcurrentHashMap<>();
	private final long ttlSeconds;

	public ChallengeStore(@Value("${challenge.ttlSeconds:300}") long ttlSeconds) {
		this.ttlSeconds = ttlSeconds;
	}

	public long ttlSeconds() {
		return ttlSeconds;
	}

	public String mint() {
		String c = UUID.randomUUID().toString();
		Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
		challenges.put(c, new Entry(expiresAt));
		return c;
	}

	public void consumeOrThrow(String challenge) {
		Entry entry = challenges.get(challenge);
		if (entry == null) {
			throw new IllegalArgumentException("Unknown challenge");
		}
		if (entry.used) {
			throw new IllegalArgumentException("Challenge already used");
		}
		if (Instant.now().isAfter(entry.expiresAt)) {
			challenges.remove(challenge);
			throw new IllegalArgumentException("Challenge expired");
		}
		entry.used = true;
	}
}
