package com.microsoft.cognitiveservices.speech.samples;

import java.io.Serializable;

public class Pair<Key, Value> implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private Key key;
	private Value value;

	public Key getKey() {
		return key;
	}

	public Value getValue() {
		return value;
	}

	public Pair(Key key, Value value) {
		this.key = key;
		this.value = value;
	}

	public String toString() {
		return key + "/" + value;
	}

	public int hashCode() {
		int hash = 7;
		hash = 31 * hash + (key != null ? key.hashCode() : 0);
		hash = 31 * hash + (value != null ? value.hashCode() : 0);
		return hash;
	}

	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o instanceof Pair) {
			Pair<?, ?> pair = (Pair<?, ?>) o;
			if (key != null ? !key.equals(pair.key) : pair.key != null)
				return false;
			if (value != null ? !value.equals(pair.value) : pair.value != null)
				return false;
			return true;
		}
		return false;
	}
}
