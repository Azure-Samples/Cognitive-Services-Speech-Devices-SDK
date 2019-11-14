package com.microsoft.cognitiveservices.speech.samples;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import com.microsoft.cognitiveservices.speech.PropertyId;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.conversation.ConversationTranscriber;
import com.microsoft.cognitiveservices.speech.conversation.ConversationTranscriptionEventArgs;
import com.microsoft.cognitiveservices.speech.conversation.Participant;
import com.microsoft.cognitiveservices.speech.conversation.User;

import java.awt.GridBagLayout;
import java.awt.SystemColor;

import javax.swing.JMenu;
import java.awt.GridBagConstraints;
import javax.swing.JTextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.border.BevelBorder;
import javax.swing.ScrollPaneConstants;
import java.awt.Font;

public class Cts extends JFrame {

	private static final long serialVersionUID = 1L;
	
	private JPanel contentPane;
	private JTextArea interResultTextArea;
	private ConversationTranscriber transcriber = null;
	private boolean meetingStarted = false;
	private final HashMap<Pair<String, BigInteger>, ConversationTranscriptionEventArgs> transcriptions = new HashMap<>();
	private JMenuItem startMenuItem, stopMenuItem;
	private static final String CTSKey = "<enter your subscription info here>";
	private static final String CTSRegion = "centralus";// Region may be "centralus" or "eastasia"
	private static final Boolean DefaultGeometry = false;// Set to false for "Circular6+1" microphone device
	private static String DeviceGeometry = "Circular6+1"; // "Circular6+1", "Linear4",
	private static String SelectedGeometry = "Raw"; // "Raw"
	private SpeechConfig speechConfig = null;
	private HashMap<String, String> signatureMap = new HashMap<>();
	public ArrayList<CtsResult> finalResultsList = new ArrayList<>();
	public ArrayList<String> finalTranscriptions = new ArrayList<>();
	private JTextArea finalResultTextArea;
	private final File jarLocation = new File(ClassLoader.getSystemClassLoader().getResource(".").getPath());
	private final int finalTextArea = 0;
	private final int intermediaTextArea = 1;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					Cts CtsFrame = new Cts();
					CtsFrame.setVisible(true);
					CtsFrame.setLocationRelativeTo(null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Cts() {
		// put the participants.properties in the same path as this application runnable
		// Jar file
		String participantsProp = jarLocation.getAbsolutePath() + File.separator + "participants.properties";
		// log.text file will save in the same path as this application runnable Jar file
		String logPath = jarLocation.getAbsolutePath() + File.separator + "log.txt";

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		FrameDisplay fd = new FrameDisplay();
		setBounds(100, 100, fd.getWidth(), fd.getLength());
		Font menuFont = fd.getMenuFont();
		Font textFont = fd.getTextFont();
		Font boldTextFont = fd.getBoldTextFont();
		

		JMenuBar menuBar = new JMenuBar();
		setJMenuBar(menuBar);
		JMenu sessionMenu = new JMenu("Session");
		sessionMenu.getAccessibleContext().setAccessibleDescription("Start or Stop Session");
		menuBar.add(sessionMenu);
		menuBar.setFont(menuFont);
		sessionMenu.setFont(menuFont);

		startMenuItem = new JMenuItem("Start");
		startMenuItem.setFont(menuFont);
		sessionMenu.add(startMenuItem);

		stopMenuItem = new JMenuItem("Stop");
		stopMenuItem.setEnabled(false);
		stopMenuItem.setFont(menuFont);
		sessionMenu.add(stopMenuItem);

		JMenu returnMenu = new JMenu("Return");
		returnMenu.getAccessibleContext().setAccessibleDescription("Return");
		returnMenu.setFont(menuFont);
		menuBar.add(returnMenu);

		JMenuItem returnMenuItem = new JMenuItem("Return");
		returnMenuItem.setFont(menuFont);
		returnMenu.add(returnMenuItem);

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		GridBagLayout gblContentPane = new GridBagLayout();
		gblContentPane.columnWidths = new int[] { fd.getWidth() };
		gblContentPane.rowHeights = new int[] { fd.getLength() * 3 / 5, fd.getLength() * 2 / 5 };

		gblContentPane.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
		gblContentPane.rowWeights = new double[] { 1.0, 1.0 };
		contentPane.setLayout(gblContentPane);

		finalResultTextArea = new JTextArea(40, 1);
		finalResultTextArea.setLineWrap(true);
		finalResultTextArea.setWrapStyleWord(true);
		finalResultTextArea.setEditable(false);
		finalResultTextArea.setFont(boldTextFont);
		finalResultTextArea.setForeground(Color.BLACK);
		finalResultTextArea.setBackground(new Color(211, 211, 211));

		JScrollPane finalscrollPane = new JScrollPane(finalResultTextArea);
		finalscrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		finalscrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		finalscrollPane.setViewportBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		finalscrollPane.setPreferredSize(new Dimension(0, 0));
		GridBagConstraints gbcScrollPane = new GridBagConstraints();
		gbcScrollPane.anchor = GridBagConstraints.NORTH;
		gbcScrollPane.gridy = 0;
		gbcScrollPane.gridwidth = 2;
		gbcScrollPane.fill = GridBagConstraints.BOTH;
		gbcScrollPane.gridx = 0;
		getContentPane().add(finalscrollPane, gbcScrollPane);

		interResultTextArea = new JTextArea(25, 1);
		interResultTextArea.setFont(textFont);
		interResultTextArea.setLineWrap(true);
		interResultTextArea.setWrapStyleWord(true);
		interResultTextArea.setEditable(false);
		interResultTextArea.setForeground(SystemColor.DARK_GRAY);
		interResultTextArea.setBackground(new Color(255, 255, 255));

		JScrollPane interScrollPane = new JScrollPane(interResultTextArea);
		interScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		interScrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		interScrollPane.setViewportBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		interScrollPane.setPreferredSize(new Dimension(0, 0));
		GridBagConstraints gbcInterScrollPane = new GridBagConstraints();
		gbcInterScrollPane.gridwidth = 2;
		gbcInterScrollPane.anchor = GridBagConstraints.NORTH;
		gbcInterScrollPane.fill = GridBagConstraints.BOTH;
		gbcInterScrollPane.gridy = 1;
		gbcInterScrollPane.gridx = 0;
		getContentPane().add(interScrollPane, gbcInterScrollPane);

		startMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				meetingStarted = true;
				clearTextBox(intermediaTextArea);
				clearTextBox(finalTextArea);
				finalResultsList.clear();
				transcriptions.clear();
				finalTranscriptions.clear();

				speechConfig = SpeechConfig.fromSubscription(CTSKey, CTSRegion);
				if (!DefaultGeometry) {
					speechConfig.setProperty("DeviceGeometry", DeviceGeometry);
					speechConfig.setProperty("SelectedGeometry", SelectedGeometry);
				}
				speechConfig.setProperty(PropertyId.Speech_LogFilename, logPath);

				try {
					transcriber = new ConversationTranscriber(speechConfig, AudioConfig.fromDefaultMicrophoneInput());

					transcriber.setConversationId("MeetingTest");
					System.out.println("Participants enrollment");

					for (String userId : signatureMap.keySet()) {
						User user = User.fromUserId(userId);
						transcriber.addParticipant(user);
						Participant participant = Participant.from(userId, "en-US", signatureMap.get(userId));
						transcriber.addParticipant(participant);
						System.out.println("add participant: " + userId);
					}
					startRecognizeMeeting(transcriber);
					startMenuItem.setEnabled(false);
					stopMenuItem.setEnabled(true);
					meetingStarted = true;
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
					displayException(ex);
				}
			}
		});

		stopMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				stopClicked();
				meetingStarted = false;
			}
		});

		returnMenuItem.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (meetingStarted) {
					stopClicked();
					meetingStarted = false;
				}
				FunctionsList.MainFrame.setVisible(true);
				dispose();
			}
		});

		FileInputStream participantIs = null;
		String participantList = new String();
		Properties prop = new Properties();
		try {

			participantIs = new FileInputStream(participantsProp);
			prop.load(participantIs);
			participantList = prop.getProperty("PARTICIPANTSLIST");
		} catch (Exception io) {
			io.printStackTrace();
		} finally {
			if (participantIs != null) {
				try {
					participantIs.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		if (participantList.length() == 0) {
			System.out.println("Cannot find participants' voice signatures");
			setRecognizedText("Please save the participants' voice signatures in file named participants.properties",
					intermediaTextArea);
		} else {
			while (participantList.length() != 0) {
				String aName = participantList.substring(participantList.indexOf('<') + 1,
						participantList.indexOf('@'));
				String aSign = participantList.substring(participantList.indexOf('@') + 1,
						participantList.indexOf('>'));
				signatureMap.put(aName, aSign);
				System.out.println(aName);
				System.out.println(aSign);
				participantList = participantList.substring(participantList.indexOf('>') + 1);
			}
		}

	}

	private void startRecognizeMeeting(ConversationTranscriber t) {
		try {
			t.sessionStarted.addEventListener((o, e) -> System.out.println("Session started event. Start recognition"));

			t.recognizing.addEventListener((o, e) -> recognizingEventHandler(e));

			t.recognized.addEventListener((o, e) -> {
				final String text = e.getResult().getText();
				final String speakerId = e.getResult().getUserId().equals("Unidentified") ? "Guest"
						: e.getResult().getUserId();
				final BigInteger offset = e.getResult().getOffset();

				final String result = speakerId + " : " + text;
				System.out.println("TransResult final result received: " + result + "; Tick: " + offset);

				if (!text.isEmpty() && !speakerId.equals("$ref$")) {
					final CtsResult finalResult = new CtsResult(offset, speakerId + " : " + text, speakerId);
					finalResultsList.add(finalResult);
					Collections.sort(finalResultsList);
					System.out.println("Display final result : " + Integer.toString(finalResultsList.size()));
					finalTranscriptions.clear();

					for (int i = 0; i < finalResultsList.size(); i++) {
						finalTranscriptions.add(finalResultsList.get(i).getResult());
					}
					appendTextLine(FunctionsList.TextJoin("\n\n", finalTranscriptions), true, finalTextArea);

				}
			});

			t.canceled.addEventListener((o, e) -> {
				System.out.println("CANCELED: Reason=" + e.getReason() + ", ErrorCode=" + e.getErrorCode()
						+ ", ErrorDetails=" + e.getErrorDetails());
			});

			t.sessionStopped.addEventListener((o, e) -> System.out.println("Session stopped event. Stop recognition"));

			final Future<Void> task = t.startTranscribingAsync();
			setOnTaskCompletedListener(task, result -> {
				long currentTime = Calendar.getInstance().getTimeInMillis();
				System.out.println("Recognition started. " + currentTime);

			});
		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			displayException(ex);
		}
	}

	private void recognizingEventHandler(ConversationTranscriptionEventArgs e) {
		final String text = e.getResult().getText();
		final String speakerId = e.getResult().getUserId().equals("Unidentified") ? "..." : e.getResult().getUserId();
		final BigInteger offset = e.getResult().getOffset();

		System.out.println(
				"TransResult " + "Intermediate" + " result received: " + speakerId + ": " + text + "; Tick: " + offset);

		Pair<String, BigInteger> key = new Pair<>(speakerId, offset);

		if (text.isEmpty() || speakerId.equals("$ref$")) {
			transcriptions.remove(key);
		} else {
			if (transcriptions.containsKey(key)) {
				if (transcriptions.get(key).getResult().getReason() == ResultReason.RecognizingSpeech) {
					System.out.println("Two utterances occurred at the same time. Offset: " + offset + "; text: " + text);
				}
			}
			transcriptions.put(key, e);
		}
		setRecognizingText();
	}

	private void setRecognizingText() {
		ArrayList<Pair<BigInteger, String>> outputEvent = new ArrayList<>();

		for (ConversationTranscriptionEventArgs event : transcriptions.values()) {
			final String speakerId = event.getResult().getUserId().equals("Unidentified") ? "..."
					: event.getResult().getUserId();
			final BigInteger offset = event.getResult().getOffset();
			outputEvent.add(new Pair<>(offset, speakerId + " : " + event.getResult().getText()));
		}

		Collections.sort(outputEvent,
				(bigIntegerStringPair, t1) -> bigIntegerStringPair.getKey().compareTo(t1.getKey()));

		ArrayList<String> outputMessage = new ArrayList<>();
		for (Pair<BigInteger, String> event : outputEvent) {
			outputMessage.add(event.getValue());
		}
		appendTextLine(FunctionsList.TextJoin("\n", outputMessage), true, intermediaTextArea);

	}

	private void stopClicked() {
		try {
			final Future<Void> task = transcriber.stopTranscribingAsync();
			setOnTaskCompletedListener(task, result -> {
				System.out.println("Recognition stopped.");
				meetingStarted = false;
				stopMenuItem.setEnabled(false);
				startMenuItem.setEnabled(true);
			});

		} catch (Exception ex) {
			System.out.println(ex.getMessage());
			displayException(ex);
		}
	}

	private void clearTextBox(int textAreaId) {
		appendTextLine("", true, textAreaId);
	}

	private void setRecognizedText(String s, int textAreaId) {
		appendTextLine(s, true, textAreaId);
	}

	private void appendTextLine(final String s, final Boolean erase, int textAreaId) {
		switch (textAreaId) {
		case 0: {
			if (erase) {
				finalResultTextArea.setText(s);
			} else {
				String txt = finalResultTextArea.getText().toString();
				finalResultTextArea.setText(String.format("%s\n%s", txt, s));
			}
			finalResultTextArea.setCaretPosition(finalResultTextArea.getDocument().getLength());
			break;
		}
		case 1: {
			if (erase) {
				interResultTextArea.setText(s);
			} else {
				String txt = interResultTextArea.getText().toString();
				interResultTextArea.setText(String.format("%s\n%s", txt, s));
			}
			interResultTextArea.setCaretPosition(interResultTextArea.getDocument().getLength());
			break;
		}
		default:
		}
	}

	private <T> void setOnTaskCompletedListener(Future<T> task, OnTaskCompletedListener<T> listener) {
		s_executorService.submit(() -> {
			T result = task.get();
			listener.onCompleted(result);
			return null;
		});
	}

	private interface OnTaskCompletedListener<T> {
		void onCompleted(T taskResult);
	}

	protected static ExecutorService s_executorService;
	static {
		s_executorService = Executors.newCachedThreadPool();
	}

	private void displayException(Exception ex) {
		interResultTextArea.setText(String.format("%s\n%s", ex.getMessage(), ex.getStackTrace().toString()));
	}

}
