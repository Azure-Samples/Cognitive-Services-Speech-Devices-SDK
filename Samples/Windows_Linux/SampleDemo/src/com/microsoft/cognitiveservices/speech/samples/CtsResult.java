package com.microsoft.cognitiveservices.speech.samples;

import java.math.BigInteger;

public class CtsResult implements Comparable<CtsResult> {
	BigInteger offset;
	String result;
	String speakerId;

	public CtsResult(BigInteger a, String b, String c) {
		offset = a;
		result = b;
		speakerId = c;
	}

	public BigInteger getOffset() {
		return offset;
	}

	public String getResult() {
		return result;
	}

	public String getSpeakerId() {
		return speakerId;
	}

	@Override
	public int compareTo(CtsResult o) {
		return offset.compareTo(o.getOffset());
	}

}
