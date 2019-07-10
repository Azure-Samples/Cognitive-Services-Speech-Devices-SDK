package com.microsoft.cognitiveservices.speech.samples;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;

class FrameDisplay {
	private Dimension screenSize;
	private int frameLength;
	private int frameWidth;

	public FrameDisplay() {
		screenSize = Toolkit.getDefaultToolkit().getScreenSize();
	}

	public int getLength() {
		frameLength = (int) Math.round(screenSize.getHeight() * 0.75);
		return frameLength;
	}

	public int getWidth() {
		frameWidth = (int) Math.round(screenSize.getWidth() * 0.3);
		return frameWidth;
	}

	public Font getButtonFont() {
		return new Font("Tahoma", Font.BOLD, frameWidth / 30);
	}

	public Font getLabelFont() {
		return new Font("Tahoma", Font.BOLD, frameWidth / 35);
	}

	public Font getTextFont() {
		return new Font("Monospaced", Font.PLAIN, frameWidth / 35);
	}

	public Font getBoldTextFont() {
		return new Font("Monospaced", Font.BOLD, frameWidth / 35);
	}

	public Font getMenuFont() {
		return new Font("Tahoma", Font.BOLD, frameWidth / 35);
	}
}