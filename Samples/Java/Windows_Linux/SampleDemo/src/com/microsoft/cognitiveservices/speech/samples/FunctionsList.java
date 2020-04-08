package com.microsoft.cognitiveservices.speech.samples;

import static com.microsoft.cognitiveservices.speech.ResultReason.RecognizedKeyword;
import static com.microsoft.cognitiveservices.speech.ResultReason.RecognizingSpeech;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import com.microsoft.cognitiveservices.speech.CancellationDetails;
import com.microsoft.cognitiveservices.speech.KeywordRecognitionModel;
import com.microsoft.cognitiveservices.speech.PropertyId;
import com.microsoft.cognitiveservices.speech.ResultReason;
import com.microsoft.cognitiveservices.speech.SpeechConfig;
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult;
import com.microsoft.cognitiveservices.speech.SpeechRecognizer;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import com.microsoft.cognitiveservices.speech.intent.IntentRecognitionResult;
import com.microsoft.cognitiveservices.speech.intent.IntentRecognizer;
import com.microsoft.cognitiveservices.speech.intent.LanguageUnderstandingModel;
import com.microsoft.cognitiveservices.speech.translation.SpeechTranslationConfig;
import com.microsoft.cognitiveservices.speech.translation.TranslationRecognizer;

public class FunctionsList extends JFrame {

	private static final long serialVersionUID = 1L;
	static FunctionsList MainFrame;
	private GridBagLayout gblContentPane;
	private JLabel recoLanguageLabel;
	private JComboBox<String> recoLanguageComboBox;
	private JButton recoOnceButton;
	private JButton recoContButton;
	private JButton recoKwsButton;
	private JButton recoIntentButton;
	private JButton recoIntentkwsButton;
	private JButton ctsButton;
	private JButton translateButton;
	private JComboBox<String> tranLanguageComboBox;
	private JTextArea recoResultTextArea;
	private static String RecoLanguage;
	private final HashMap<String, String> intentIdMap = new HashMap<>();
	private static String LogPath = new String();
	private final static File JarLocation = new File(ClassLoader.getSystemClassLoader().getResource(".").getPath());

	// Subscription
	private static final String SpeechSubscriptionKey = "<enter your subscription info here>";
	private static final String SpeechRegion = "westus"; // You can change this if your speech region is different.
	private static final String LuisSubscriptionKey = "<enter your subscription info here>";
	private static final String LuisRegion = "westus2"; // you can change this, if you want to test the intent, and your
														// LUIS region is different.
	private static final String LuisAppId = "<enter your LUIS AppId>";

	private static String Keyword = "computer";
	private static String KeywordModel = new String();
	private static final Boolean DefaultGeometry = false;
	private static String DeviceGeometry = "Circular6+1"; // "Circular6+1", "Linear4",
	private static String SelectedGeometry = "Circular6+1"; // "Circular6+1", "Circular3+1", "Linear4", "Linear2"

	private static String SampleAudioInput = new String();

	private JPanel contentPane;
	private final String[] recolanguage = { "English (United States)", "German (Germany)",
			"Chinese (Mandarin, simplified)", "English (India)", "Spanish (Spain)", "French (France)",
			"Italian (Italy)", "Portuguese (Brazil)", "Russian (Russia)" };
	private final String[] tranlanguage = { "Afrikaans", "Arabic", "Bangla", "Bosnian (Latin)", "Bulgarian",
			"Cantonese (Traditional)", "Catalan", "Chinese Simplified", "Chinese Traditional", "Croatian", "Czech",
			"Danish", "Dutch", "English", "Estonian", "Fijian", "Filipino", "Finnish", "French", "German", "Greek",
			"Haitian Creole", "Hebrew", "Hindi", "Hmong Daw", "Hungarian", "Indonesian", "Italian", "Japanese",
			"Kiswahili", "Klingon", "Klingon (plqaD)", "Korean", "Latvian", "Lithuanian", "Malagasy", "Malay",
			"Maltese", "Norwegian", "Persian", "Polish", "Portuguese", "Queretaro Otomi", "Romanian", "Russian",
			"Samoan", "Serbian (Cyrillic)", "Serbian (Latin)", "Slovak", "Slovenian", "Spanish", "Swedish", "Tahitian",
			"Tamil", "Thai", "Tongan", "Turkish", "Ukrainian", "Urdu", "Vietnamese", "Welsh", "Yucatec Maya" };

	private AudioConfig getAudioConfig() {

		if (new File(SampleAudioInput).exists()) {
			recoResultTextArea.setText(recoResultTextArea.getText() + "\nInfo: Using AudioFile " + SampleAudioInput);

			// run from a file
			return AudioConfig.fromWavFileInput(SampleAudioInput);
		}

		// run from the microphone
		return AudioConfig.fromDefaultMicrophoneInput();
	}

	public static SpeechConfig getSpeechConfig() {
		SpeechConfig speechConfig = SpeechConfig.fromSubscription(SpeechSubscriptionKey, SpeechRegion);
		if (!DefaultGeometry) {
			speechConfig.setProperty("DeviceGeometry", DeviceGeometry);
			speechConfig.setProperty("SelectedGeometry", SelectedGeometry);
		}
		speechConfig.setSpeechRecognitionLanguage(RecoLanguage);
		speechConfig.setProperty(PropertyId.Speech_LogFilename, LogPath);

		return speechConfig;
	}

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainFrame = new FunctionsList();
					MainFrame.setVisible(true);
					MainFrame.setLocationRelativeTo(null);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public FunctionsList() {

		// put the keyword model file-kws.table in the same path as this application
		// runnable Jar file
		KeywordModel = JarLocation.getAbsolutePath() + File.separator + "kws.table";
		// log.text file will save in the same path as this application runnable Jar file
		LogPath = JarLocation.getAbsolutePath() + File.separator + "log.txt";
		// Note: point this to a wav file in case you don't want to
		// use the microphone. It will be used automatically, if
		// the file exists on disk.
		SampleAudioInput = JarLocation.getAbsolutePath() + File.separator + "kws-computer.wav";

		FrameDisplay fd = new FrameDisplay();

		setTitle("Speech Recognition Application");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, fd.getWidth(), fd.getLength());
		Font buttonFont = fd.getButtonFont();
		Font labelFont = fd.getLabelFont();
		Font textFont = fd.getTextFont();

		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		gblContentPane = new GridBagLayout();

		gblContentPane.columnWidths = new int[] { fd.getWidth() / 2 - 2, 0, fd.getWidth() / 2 - 2, 0 };
		gblContentPane.rowHeights = new int[] { 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
		gblContentPane.columnWeights = new double[] { 1.0, 1.0, 0.0, Double.MIN_VALUE };
		gblContentPane.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, Double.MIN_VALUE };
		contentPane.setLayout(gblContentPane);

		recoLanguageLabel = new JLabel("Recognize Language:  ");
		recoLanguageLabel.setVerticalAlignment(SwingConstants.TOP);
		recoLanguageLabel.setForeground(Color.GRAY);
		recoLanguageLabel.setFont(labelFont);
		recoLanguageLabel.setHorizontalAlignment(JLabel.LEFT);
		GridBagConstraints gbcRecoLanguageLabel = new GridBagConstraints();
		gbcRecoLanguageLabel.anchor = GridBagConstraints.EAST;
		gbcRecoLanguageLabel.insets = new Insets(0, 0, 5, 5);
		gbcRecoLanguageLabel.gridx = 0;
		gbcRecoLanguageLabel.gridy = 0;
		contentPane.add(recoLanguageLabel, gbcRecoLanguageLabel);

		recoLanguageComboBox = new JComboBox<String>(recolanguage);
		recoLanguageComboBox.setFont(labelFont);
		GridBagConstraints gbcRecoLanguageComboBox = new GridBagConstraints();
		gbcRecoLanguageComboBox.gridwidth = 2;
		gbcRecoLanguageComboBox.insets = new Insets(0, 0, 5, 0);
		gbcRecoLanguageComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbcRecoLanguageComboBox.gridx = 1;
		gbcRecoLanguageComboBox.gridy = 0;
		contentPane.add(recoLanguageComboBox, gbcRecoLanguageComboBox);

		recoOnceButton = new JButton("Recognize Once");
		recoOnceButton.setFont(buttonFont);
		GridBagConstraints gbcRecoOnceButton = new GridBagConstraints();
		gbcRecoOnceButton.insets = new Insets(0, 0, 5, 0);
		gbcRecoOnceButton.fill = GridBagConstraints.HORIZONTAL;
		gbcRecoOnceButton.gridwidth = 3;
		gbcRecoOnceButton.gridx = 0;
		gbcRecoOnceButton.gridy = 1;

		contentPane.add(recoOnceButton, gbcRecoOnceButton);

		recoContButton = new JButton("Recognize Continuously");
		recoContButton.setFont(buttonFont);
		GridBagConstraints gbcRecoContButton = new GridBagConstraints();
		gbcRecoContButton.insets = new Insets(0, 0, 5, 0);
		gbcRecoContButton.fill = GridBagConstraints.HORIZONTAL;
		gbcRecoContButton.gridwidth = 3;
		gbcRecoContButton.gridx = 0;
		gbcRecoContButton.gridy = 2;
		contentPane.add(recoContButton, gbcRecoContButton);

		recoKwsButton = new JButton("Recognize With Wake Word");
		recoKwsButton.setFont(buttonFont);
		GridBagConstraints gbcRecoKwsButton = new GridBagConstraints();
		gbcRecoKwsButton.insets = new Insets(0, 0, 5, 0);
		gbcRecoKwsButton.fill = GridBagConstraints.HORIZONTAL;
		gbcRecoKwsButton.gridwidth = 3;
		gbcRecoKwsButton.gridx = 0;
		gbcRecoKwsButton.gridy = 3;
		contentPane.add(recoKwsButton, gbcRecoKwsButton);

		recoIntentButton = new JButton("Recognize Intent");
		recoIntentButton.setFont(buttonFont);
		GridBagConstraints gbcRecoIntentButton = new GridBagConstraints();
		gbcRecoIntentButton.insets = new Insets(0, 0, 5, 0);
		gbcRecoIntentButton.fill = GridBagConstraints.HORIZONTAL;
		gbcRecoIntentButton.gridwidth = 3;
		gbcRecoIntentButton.gridx = 0;
		gbcRecoIntentButton.gridy = 4;
		contentPane.add(recoIntentButton, gbcRecoIntentButton);

		recoIntentkwsButton = new JButton("Recognize Intent With Wake Word");
		recoIntentkwsButton.setFont(buttonFont);
		GridBagConstraints gbcRecoIntentkwsButton = new GridBagConstraints();
		gbcRecoIntentkwsButton.insets = new Insets(0, 0, 5, 0);
		gbcRecoIntentkwsButton.fill = GridBagConstraints.HORIZONTAL;
		gbcRecoIntentkwsButton.gridwidth = 3;
		gbcRecoIntentkwsButton.gridx = 0;
		gbcRecoIntentkwsButton.gridy = 5;
		contentPane.add(recoIntentkwsButton, gbcRecoIntentkwsButton);

		ctsButton = new JButton("Conversation Transcription");
		ctsButton.setFont(buttonFont);
		GridBagConstraints gbcCtsButton = new GridBagConstraints();
		gbcCtsButton.insets = new Insets(0, 0, 5, 0);
		gbcCtsButton.fill = GridBagConstraints.HORIZONTAL;
		gbcCtsButton.gridwidth = 3;
		gbcCtsButton.gridx = 0;
		gbcCtsButton.gridy = 6;
		contentPane.add(ctsButton, gbcCtsButton);

		translateButton = new JButton("Translate to: ");
		translateButton.setFont(buttonFont);
		GridBagConstraints gbcTranslateButton = new GridBagConstraints();
		gbcTranslateButton.fill = GridBagConstraints.HORIZONTAL;
		gbcTranslateButton.insets = new Insets(0, 0, 5, 5);
		gbcTranslateButton.gridx = 0;
		gbcTranslateButton.gridy = 7;
		contentPane.add(translateButton, gbcTranslateButton);

		tranLanguageComboBox = new JComboBox<String>(tranlanguage);
		tranLanguageComboBox.setSelectedIndex(7);
		tranLanguageComboBox.setFont(labelFont);
		GridBagConstraints gbcTranLanguageComboBox = new GridBagConstraints();
		gbcTranLanguageComboBox.insets = new Insets(0, 0, 5, 0);
		gbcTranLanguageComboBox.gridwidth = 2;
		gbcTranLanguageComboBox.fill = GridBagConstraints.HORIZONTAL;
		gbcTranLanguageComboBox.gridx = 1;
		gbcTranLanguageComboBox.gridy = 7;
		contentPane.add(tranLanguageComboBox, gbcTranLanguageComboBox);

		recoResultTextArea = new JTextArea();
		recoResultTextArea.setEditable(false);
		recoResultTextArea.setLineWrap(true);
		recoResultTextArea.setWrapStyleWord(true);
		recoResultTextArea.setFont(textFont);
		GridBagConstraints gbcRecoResultTextArea = new GridBagConstraints();
		gbcRecoResultTextArea.gridwidth = 3;
		gbcRecoResultTextArea.insets = new Insets(0, 0, 0, 5);
		gbcRecoResultTextArea.fill = GridBagConstraints.BOTH;
		gbcRecoResultTextArea.gridx = 0;
		gbcRecoResultTextArea.gridy = 8;

		JScrollPane scrollPane = new JScrollPane(recoResultTextArea);

		scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setViewportBorder(new BevelBorder(BevelBorder.LOWERED, null, null, null, null));
		scrollPane.setPreferredSize(new Dimension(0, 0));

		contentPane.add(scrollPane, gbcRecoResultTextArea);

		///////////////////////////////////////////////////
		// check if we have a valid key
		///////////////////////////////////////////////////
		if (SpeechSubscriptionKey.startsWith("<") || SpeechSubscriptionKey.endsWith(">")) {
			recoResultTextArea.setText(
					"Error: Replace SpeechSubscriptionKey with your actual speech subscription key and re-compile!");
			return;
		}
		///////////////////////////////////////////////////
		// check if we have a valid microphone parameter
		///////////////////////////////////////////////////
		if (!DefaultGeometry) {
			if (DeviceGeometry.startsWith("<") || DeviceGeometry.endsWith(">")) {
				recoResultTextArea
						.setText("Error: Replace DeviceGeometry with your actual microphone parameter and re-compile");
				return;
			}
			if (SelectedGeometry.startsWith("<") || SelectedGeometry.endsWith(">")) {
				recoResultTextArea
						.setText("Error: Replace SelectedGeometry with your actual select parameter and re-compile!");
				return;
			}
		}
		if (LuisSubscriptionKey.startsWith("<") || LuisSubscriptionKey.endsWith(">")) {
			recoResultTextArea.setText(recoResultTextArea.getText()
					+ "\nWarning: Replace LuisSubscriptionKey with your actual Luis subscription key to use Intents!");
		}

		///////////////////////////////////////////////////
		// recognize Once with intermediate results
		///////////////////////////////////////////////////

		recoOnceButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				RecoLanguage = LanguageCode.getCode(0,
						recoLanguageComboBox.getItemAt(recoLanguageComboBox.getSelectedIndex()).toString());
				disableButtons();
				clearTextBox();
				try {
					System.out.println(" Speech Recognize Once , recognize language: " + RecoLanguage);
					final SpeechRecognizer reco = new SpeechRecognizer(getSpeechConfig(), getAudioConfig());
					reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
						final String s = speechRecognitionResultEventArgs.getResult().getText();
						System.out.println("Intermediate result received: " + s);
						setRecognizedText(s);
					});

					final Future<SpeechRecognitionResult> task = reco.recognizeOnceAsync();
					setOnTaskCompletedListener(task, result -> {
						final String s = result.getText();
						reco.close();
						System.out.println("Recognizer returned: " + s);
						setRecognizedText(s);
						enableButtons();
					});
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
					displayException(ex);
				}

			}
		});

		///////////////////////////////////////////////////
		// recognize continuously
		///////////////////////////////////////////////////
		recoContButton.addActionListener(new ActionListener() {
			private boolean continuousListeningStarted = false;
			private SpeechRecognizer reco = null;
			private String buttonText = "";
			private ArrayList<String> content = new ArrayList<>();

			public void actionPerformed(ActionEvent e) {
				RecoLanguage = LanguageCode.getCode(0,
						recoLanguageComboBox.getItemAt(recoLanguageComboBox.getSelectedIndex()).toString());
				disableButtons();

				if (continuousListeningStarted) {
					if (reco != null) {
						final Future<Void> task = reco.stopContinuousRecognitionAsync();
						setOnTaskCompletedListener(task, result -> {
							System.out.println("Continuous recognition stopped.");
							recoContButton.setText(buttonText);
							enableButtons();
							continuousListeningStarted = false;
						});
					} else {
						continuousListeningStarted = false;
					}

					return;
				}

				clearTextBox();

				try {
					content.clear();
					reco = new SpeechRecognizer(getSpeechConfig(), getAudioConfig());
					reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
						final String s = speechRecognitionResultEventArgs.getResult().getText();
						System.out.println("Intermediate result received: " + s);
						content.add(s);
						setRecognizedText(TextJoin(" ", content));
						content.remove(content.size() - 1);
					});

					reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
						final String s = speechRecognitionResultEventArgs.getResult().getText();
						System.out.println("Final result received: " + s);
						content.add(s);
						setRecognizedText(TextJoin(" ", content));
					});

					final Future<Void> task = reco.startContinuousRecognitionAsync();
					setOnTaskCompletedListener(task, result -> {
						continuousListeningStarted = true;
						buttonText = recoContButton.getText().toString();
						recoContButton.setText("Stop");
						recoContButton.setEnabled(true);

					});
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
					displayException(ex);
				}
			}
		});

		///////////////////////////////////////////////////
		// recognize with wake word
		///////////////////////////////////////////////////
		recoKwsButton.addActionListener(new ActionListener() {

			private static final String delimiter = "\n";
			private final ArrayList<String> content = new ArrayList<>();
			private boolean continuousListeningStarted = false;
			private SpeechRecognizer reco = null;
			private String buttonText = "";

			public void actionPerformed(ActionEvent e) {

				RecoLanguage = LanguageCode.getCode(0,
						recoLanguageComboBox.getItemAt(recoLanguageComboBox.getSelectedIndex()).toString());
				disableButtons();

				if (continuousListeningStarted) {
					if (reco != null) {
						final Future<Void> task = reco.stopKeywordRecognitionAsync();
						setOnTaskCompletedListener(task, result -> {
							System.out.println("Continuous recognition stopped.");
							recoKwsButton.setText(buttonText);
							enableButtons();
							continuousListeningStarted = false;
						});
					} else {
						continuousListeningStarted = false;
					}
					return;
				}

				clearTextBox();

				content.clear();
				content.add("");
				content.add("");
				try {
					reco = new SpeechRecognizer(getSpeechConfig(), getAudioConfig());
					reco.sessionStarted.addEventListener((o, sessionEventArgs) -> {
						System.out.println(
								"got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStarted");
						content.set(0, "KeywordModel `" + Keyword + "` detected");
						setRecognizedText(TextJoin(delimiter, content));

					});

					reco.sessionStopped.addEventListener((o, sessionEventArgs) -> System.out
							.println("got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStopped"));

					reco.recognizing.addEventListener((o, intermediateResultEventArgs) -> {
						final String s = intermediateResultEventArgs.getResult().getText();
						ResultReason rr = intermediateResultEventArgs.getResult().getReason();
						System.out.println("got a intermediate result: " + s + " result reason:" + rr.toString());
						if (rr == RecognizingSpeech) {
							Integer index = content.size() - 2;
							content.set(index + 1, index.toString() + ". " + s);
							setRecognizedText(TextJoin(delimiter, content));
						}
					});
					reco.recognized.addEventListener((o, finalResultEventArgs) -> {
						String s = finalResultEventArgs.getResult().getText();
						ResultReason rr = finalResultEventArgs.getResult().getReason();

						if (rr == RecognizedKeyword) {
							content.add("");
						}

						if (!s.isEmpty()) {
							Integer index = content.size() - 2;
							content.set(index + 1, index.toString() + ". " + s);
							content.set(0, "say `" + Keyword + "`...");
							setRecognizedText(TextJoin(delimiter, content));
							System.out.println("got a final result: " + " " + Integer.toString(index + 1) + " " + s
									+ " result reason:" + rr.toString());
						}

					});

					File kwsFile = new File(KeywordModel);
					if (kwsFile.exists() && kwsFile.isFile()) {
						final KeywordRecognitionModel keywordRecognitionModel = KeywordRecognitionModel
								.fromFile(KeywordModel);
						final Future<Void> task = reco.startKeywordRecognitionAsync(keywordRecognitionModel);
						setOnTaskCompletedListener(task, result -> {
							content.set(0, "say `" + Keyword + "`...");
							setRecognizedText(TextJoin(delimiter, content));
							continuousListeningStarted = true;

							buttonText = recoKwsButton.getText().toString();
							recoKwsButton.setText("Stop");
							recoKwsButton.setEnabled(true);
						});
					} else {
						recoResultTextArea.setText("Error: can not find the keyword table file");
						return;
					}

				} catch (Exception ex) {
					System.out.println(ex.getMessage());
					displayException(ex);

				}
			}
		});

		intentIdMap.put("1", "play music");
		intentIdMap.put("2", "stop");
		///////////////////////////////////////////////////
		// recognize intent
		///////////////////////////////////////////////////
		recoIntentButton.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				final ArrayList<String> content = new ArrayList<>();

				RecoLanguage = LanguageCode.getCode(0,
						recoLanguageComboBox.getItemAt(recoLanguageComboBox.getSelectedIndex()).toString());
				disableButtons();
				clearTextBox();

				content.add("");
				content.add("");
				try {
					final SpeechConfig speechIntentConfig = SpeechConfig.fromSubscription(LuisSubscriptionKey,
							LuisRegion);
					if (!DefaultGeometry) {
						speechIntentConfig.setProperty("DeviceGeometry", DeviceGeometry);
						speechIntentConfig.setProperty("SelectedGeometry", SelectedGeometry);
					}
					speechIntentConfig.setSpeechRecognitionLanguage(RecoLanguage);
					speechIntentConfig.setProperty(PropertyId.Speech_LogFilename, LogPath);
					IntentRecognizer reco = new IntentRecognizer(speechIntentConfig, getAudioConfig());

					LanguageUnderstandingModel intentModel = LanguageUnderstandingModel.fromAppId(LuisAppId);
					for (Map.Entry<String, String> entry : intentIdMap.entrySet()) {
						reco.addIntent(intentModel, entry.getValue(), entry.getKey());
						System.out.println("intent: " + entry.getValue() + " Intent Id: " + entry.getKey());
					}

					reco.recognizing.addEventListener((o, intentRecognitionResultEventArgs) -> {
						final String s = intentRecognitionResultEventArgs.getResult().getText();
						System.out.println("Intermediate result received: " + s);
						content.set(0, s);
						setRecognizedText(TextJoin("\n", content));
					});

					final Future<IntentRecognitionResult> task = reco.recognizeOnceAsync();
					setOnTaskCompletedListener(task, result -> {
						System.out.println("Intent recognition stopped.");
						String s = result.getText();

						if (result.getReason() != ResultReason.RecognizedIntent) {
							String errorDetails = (result.getReason() == ResultReason.Canceled)
									? CancellationDetails.fromResult(result).getErrorDetails()
									: "";
							s = "Intent failed with " + result.getReason()
									+ ". Did you enter your Language Understanding subscription?"
									+ System.lineSeparator() + errorDetails;
						}
						String intentId = result.getIntentId();
						System.out.println("IntentId: " + intentId);
						String intent = "";
						if (intentIdMap.containsKey(intentId)) {
							intent = intentIdMap.get(intentId);
						}

						System.out.println("S: " + s + ", intent: " + intent);
						content.set(0, s);
						content.set(1, " [intent: " + intent + "]");
						reco.close();
						setRecognizedText(TextJoin("\n", content));
						enableButtons();
					});
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
					displayException(ex);
				}
			}
		});

		///////////////////////////////////////////////////
		// recognize intent with wake word
		///////////////////////////////////////////////////
		recoIntentkwsButton.addActionListener(new ActionListener() {

			private static final String delimiter = "\n";
			private final ArrayList<String> content = new ArrayList<>();
			private boolean continuousListeningStarted = false;
			private IntentRecognizer reco = null;
			private String buttonText = "";

			public void actionPerformed(ActionEvent e) {

				RecoLanguage = LanguageCode.getCode(0,
						recoLanguageComboBox.getItemAt(recoLanguageComboBox.getSelectedIndex()).toString());
				disableButtons();

				if (continuousListeningStarted) {
					if (reco != null) {
						final Future<Void> task = reco.stopKeywordRecognitionAsync();
						setOnTaskCompletedListener(task, result -> {
							System.out.println("Continuous recognition stopped.");
							recoIntentkwsButton.setText(buttonText);
							enableButtons();
							continuousListeningStarted = false;
						});
					} else {
						continuousListeningStarted = false;
					}

					return;
				}

				clearTextBox();

				content.clear();
				content.add("");
				content.add("");
				try {
					final SpeechConfig intentSpeechConfig = SpeechConfig.fromSubscription(LuisSubscriptionKey,
							LuisRegion);
					intentSpeechConfig.setSpeechRecognitionLanguage(RecoLanguage);
					if (!DefaultGeometry) {
						intentSpeechConfig.setProperty("DeviceGeometry", DeviceGeometry);
						intentSpeechConfig.setProperty("SelectedGeometry", SelectedGeometry);
					}
					intentSpeechConfig.setProperty(PropertyId.Speech_LogFilename, LogPath);
					reco = new IntentRecognizer(intentSpeechConfig, getAudioConfig());

					LanguageUnderstandingModel intentModel = LanguageUnderstandingModel.fromAppId(LuisAppId);
					for (Map.Entry<String, String> entry : intentIdMap.entrySet()) {
						reco.addIntent(intentModel, entry.getValue(), entry.getKey());
						System.out.println("intent: " + entry.getValue() + " Intent Id: " + entry.getKey());
					}

					reco.sessionStarted.addEventListener((o, sessionEventArgs) -> {
						System.out.println(
								"got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStarted");
						content.set(0, "KeywordModel `" + Keyword + "` detected");
						setRecognizedText(TextJoin(delimiter, content));
					});

					reco.sessionStopped.addEventListener((o, sessionEventArgs) -> System.out
							.println("got a session (" + sessionEventArgs.getSessionId() + ")event: sessionStopped"));

					reco.recognizing.addEventListener((o, intermediateResultEventArgs) -> {
						final String s = intermediateResultEventArgs.getResult().getText();
						ResultReason rr = intermediateResultEventArgs.getResult().getReason();
						System.out.println("got a intermediate result: " + s + " result reason:" + rr.toString());
						if (rr == RecognizingSpeech) {
							Integer index = content.size() - 2;
							content.set(index + 1, index.toString() + ". " + s);
							setRecognizedText(TextJoin(delimiter, content));
						}
					});

					reco.recognized.addEventListener((o, finalResultEventArgs) -> {
						String s = finalResultEventArgs.getResult().getText();
						String intentId = finalResultEventArgs.getResult().getIntentId();
						System.out.println("IntentId: " + intentId);
						String intent = "";
						if (intentIdMap.containsKey(intentId)) {
							intent = intentIdMap.get(intentId);
						}

						ResultReason rr = finalResultEventArgs.getResult().getReason();
						System.out.println("got a final result: " + s + " result reason:" + rr.toString());
						if (rr == RecognizedKeyword) {
							content.add("");
						}
						if (!s.isEmpty()) {
							Integer index = content.size() - 2;
							content.set(index + 1, index.toString() + ". " + s + " [intent: " + intent + "]");
							content.set(0, "say `" + Keyword + "`...");
							setRecognizedText(TextJoin(delimiter, content));
						}
					});

					File kwsFile = new File(KeywordModel);
					if (kwsFile.exists() && kwsFile.isFile()) {

						final KeywordRecognitionModel keywordRecognitionModel = KeywordRecognitionModel
								.fromFile(KeywordModel);
						final Future<Void> task = reco.startKeywordRecognitionAsync(keywordRecognitionModel);
						setOnTaskCompletedListener(task, result -> {
							content.set(0, "say `" + Keyword + "`...");
							setRecognizedText(TextJoin(delimiter, content));
							continuousListeningStarted = true;
							buttonText = recoIntentkwsButton.getText().toString();
							recoIntentkwsButton.setText("Stop");
							recoIntentkwsButton.setEnabled(true);
						});
					} else {
						recoResultTextArea.setText("Error: can not find the keyword table file");
						return;
					}
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
					displayException(ex);
				}
			}
		});

		///////////////////////////////////////////////////
		// Conversation Transcription
		///////////////////////////////////////////////////
		ctsButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {

				Cts CtsFrame = new Cts();
				CtsFrame.setVisible(true);
				CtsFrame.setLocationRelativeTo(null);
				MainFrame.setVisible(false);
			}
		});

		///////////////////////////////////////////////////
		// recognize and translate
		///////////////////////////////////////////////////
		translateButton.addActionListener(new ActionListener() {

			private boolean continuousListeningStarted = false;
			private TranslationRecognizer reco = null;
			private String buttonText = "";
			private ArrayList<String> content = new ArrayList<>();
			private String TranLanguage;

			public void actionPerformed(ActionEvent e) {

				RecoLanguage = LanguageCode.getCode(0,
						recoLanguageComboBox.getItemAt(recoLanguageComboBox.getSelectedIndex()).toString());
				TranLanguage = LanguageCode.getCode(1,
						tranLanguageComboBox.getItemAt(tranLanguageComboBox.getSelectedIndex()).toString());
				disableButtons();
				if (continuousListeningStarted) {
					if (reco != null) {
						final Future<Void> task = reco.stopContinuousRecognitionAsync();
						setOnTaskCompletedListener(task, result -> {
							System.out.println("Continuous recognition stopped.");
							translateButton.setText(buttonText);
							enableButtons();
							continuousListeningStarted = false;
						});
					} else {
						continuousListeningStarted = false;
					}
					return;
				}

				clearTextBox();

				try {
					content.clear();
					final SpeechTranslationConfig translationSpeechConfig = SpeechTranslationConfig
							.fromSubscription(SpeechSubscriptionKey, SpeechRegion);
					if (!DefaultGeometry) {
						translationSpeechConfig.setProperty("DeviceGeometry", DeviceGeometry);
						translationSpeechConfig.setProperty("SelectedGeometry", SelectedGeometry);
					}
					translationSpeechConfig.addTargetLanguage(RecoLanguage);
					translationSpeechConfig.addTargetLanguage(TranLanguage);
					translationSpeechConfig.setSpeechRecognitionLanguage(RecoLanguage);
					translationSpeechConfig.setProperty(PropertyId.Speech_LogFilename, LogPath);

					reco = new TranslationRecognizer(translationSpeechConfig, getAudioConfig());
					reco.recognizing.addEventListener((o, speechRecognitionResultEventArgs) -> {
						final Map<String, String> translations = speechRecognitionResultEventArgs.getResult()
								.getTranslations();
						StringBuffer sb = new StringBuffer();
						for (String key : translations.keySet()) {
							sb.append(key + " -> '" + translations.get(key) + "'\n");
						}
						final String s = sb.toString();

						System.out.println("Intermediate result received: " + s);
						content.add(s);
						setRecognizedText(TextJoin(" ", content));
						content.remove(content.size() - 1);
					});

					reco.recognized.addEventListener((o, speechRecognitionResultEventArgs) -> {
						final Map<String, String> translations = speechRecognitionResultEventArgs.getResult()
								.getTranslations();
						StringBuffer sb = new StringBuffer();
						for (String key : translations.keySet()) {
							if (!translations.get(key).isEmpty()) {
								sb.append(key + " -> '" + translations.get(key) + "'\n");
							}
						}
						final String s = sb.toString();
						System.out.println("Final result received: " + s);
						if (!s.isEmpty()) {
							content.add(s);
						}
						setRecognizedText(TextJoin(" ", content));
					});

					final Future<Void> task = reco.startContinuousRecognitionAsync();
					setOnTaskCompletedListener(task, result -> {
						continuousListeningStarted = true;
						buttonText = translateButton.getText().toString();
						translateButton.setText("Stop");
						translateButton.setEnabled(true);

					});
				} catch (Exception ex) {
					System.out.println(ex.getMessage());
					displayException(ex);
				}
			}
		});
	}

	private void displayException(Exception ex) {
		recoResultTextArea.setText(ex.getMessage() + "\n" + ex.getStackTrace().toString());
	}

	private void clearTextBox() {
		setTextbox("");
	}

	private void setRecognizedText(final String s) {
		setTextbox(s);
	}

	private void setTextbox(final String s) {
		recoResultTextArea.setText(s);
		recoResultTextArea.setCaretPosition(recoResultTextArea.getDocument().getLength());
	}

	private void disableButtons() {
		recoOnceButton.setEnabled(false);
		recoContButton.setEnabled(false);
		recoKwsButton.setEnabled(false);
		recoIntentButton.setEnabled(false);
		recoIntentkwsButton.setEnabled(false);
		ctsButton.setEnabled(false);
		translateButton.setEnabled(false);
	}

	private void enableButtons() {
		recoOnceButton.setEnabled(true);
		recoContButton.setEnabled(true);
		recoKwsButton.setEnabled(true);
		recoIntentButton.setEnabled(true);
		recoIntentkwsButton.setEnabled(true);
		ctsButton.setEnabled(true);
		translateButton.setEnabled(true);
	}

	public static String TextJoin(CharSequence delimiter, ArrayList<String> tokens) {
		final int length = tokens.size();
		if (length == 0) {
			return "";
		}
		final StringBuilder sb = new StringBuilder();
		sb.append(tokens.get(0));
		for (int i = 1; i < length; i++) {
			sb.append(delimiter);
			sb.append(tokens.get(i));
		}
		return sb.toString();
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
}
