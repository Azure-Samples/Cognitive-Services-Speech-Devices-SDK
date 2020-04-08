using NLog;
using System;
using System.Collections.Generic;
using System.Configuration;
using System.Diagnostics;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Text;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using System.Windows.Forms;

namespace DDKSpeechRecognition
{
    public partial class Form1 : Form
    {
        // device names
        // https://docs.microsoft.com/en-us/azure/cognitive-services/speech-service/get-speech-devices-sdk
        private static string DeviceName = "DDK2"; //Roobo Smart Audio Dev Kit 2
        private static string DeviceSN = "DDK2";
        public const string DDK2 = "DDK2"; //Roobo Smart Audio Dev Kit 2
        public static string DDK2_SN = "DDK2";
        public const string DDK1 = "DDK1"; //Roobo Smart Audio Dev Kit
        public static string DDK1_SN = "0123456789ABCDEF";
        public const string URbetter = "URbetter";
        public static string URbetter_SN = "0123456789ABCDEF";
        public const string Eden_Win = "Azure Kinect DK";
        // result log 
        public static string Resultlog = "";
        public static List<string> Contents = new List<string>();
        public static List<string> KWSContents = new List<string>();
        // timer 
        private static Timer DelayTimer = new Timer();
        private const int DelayTimerInterval = 1000;
        // messages
        private const string InitalMessage = "1. Click 'Connect to WiFi' button see steps to connect device to WiFi \n" +
                "2. Click the 'Setup' button to initialize device \n" +
                "3. Click the 'Continuous Recognition' button to run. (If device was setup before, just click 'Continuous Recognition')";
        private static string WIFIMessage_DDK2 = $@"1. Connect DDK2 to PC" + Environment.NewLine +
                $@"2. Run 'adb -s {DeviceSN} shell test_cmd_wifi -s <wifi_name> -p <password>' in command window";
        private static string WIFIMessage_DDK1 = $@"1. Connect DDK1 to PC" + Environment.NewLine +
                "2. Install Vysor on PC and connect DDK1 to WiFi through Vysor emulator UI settings";
        private static string WIFIMessage_Eden = "Please check if your host PC connect to internet.";
        private const string KeywordMessage = "Please say computer (or your custom keyword) ...\n";
        private const string ContinousMessage = "Please say something ...\n";
        private const string CustomCommandsMessage = "Please say: Computer, turn on/off the tv/fan, set the temperature or set alarm\n";
        private const string DirectLineWaitMessage = "Connecting to bot, please wait...\nPlease say something, then the echo bot will repeat it\n";
        private const string ConnectedToWIFIMessage = "Great, device was connected to WiFi";
        private const string HowToConnectToWIFIMessage = "How to connect to WiFi?";
        private const string URbetterConnectToWIFIStepsMessage = "Edit the opened conf file WiFi info\nClose this message window\nClick the Connect to WiFi button again\n";
        private static string CurrentStartMessage = string.Empty;
        public const string HostPCNotConnectToInternet = "Looks host PC is not connected to internet, please connect to internet and restart this application.";
        private const string FailedToOpenHelp = "Failed to open help file.";
        private const string HelpFileName = @"Assets\help.html";
        private const string HelpShortcutKey = "F1";
        // button names
        private const string ContinousRecognitionButtonText = "Continuous Recognition";
        private const string KeywordRecognitionButtonText = "Keyword Recognition";
        private const string CustomCommandsDialogButtonText = "Custom Commands dialog (preview)";
        private const string DirectLineDialogButtonText = "Direct Line dialog";
        private const string CTSButtonText = "Conversation Transcription";
        private static Button CurrentButton;
        private const string Stop = "Stop";
        private const string Loading = "Loading...";
        // TCP server
        public static string IPAddressV4_Server = string.Empty;
        public static string IPAddressV4_Local = string.Empty;
        public static bool TCPServerStarted = false;
        private static bool TcpServerListeningIsDone = false;
        private static int PortNum = 11000;
        // CTS text
        private const string Unidentified = "Unidentified :";
        private const string GUEST = "GUEST :";
        private static Regex RegExp;
        // Direct line text
        private const string Greeting = "Greeting: Hello and welcome!\n";
        // Log
        private static readonly NLog.Logger Logger = NLog.LogManager.GetCurrentClassLogger();
        private string logName = "DDKLog.txt";

        public Form1()
        {
            InitializeComponent();
            continueRichTextBox.Text = InitalMessage;
            continueRichTextBox.HideSelection = false;
            // Define the border style of the form to a dialog box.
            this.FormBorderStyle = FormBorderStyle.FixedDialog;
            // Set the MaximizeBox to false to remove the maximize box.
            this.MaximizeBox = false;
            // Set the start position of the form to the center of the screen.
            this.StartPosition = FormStartPosition.CenterScreen;
            // add shortcut for F1
            this.KeyPreview = true;
            this.KeyDown += new KeyEventHandler(Form1_KeyDown);
            // add richtext box right click 
            this.continueRichTextBox.AddContextMenu();

            // read Device SN from app.config
            ReadSNFromConfig();

            // config log
            ConfigLog();

            // check if user has ADB command
            SetupAction.SanityCheckADB(Logger);

            // generate random port between 10000 - 11000
            Random r = new Random();
            PortNum = r.Next(10000, 11000);

            // get host PC IPV4 address
            IPAddressTextBox.Text = SetupAction.GetUsingIPV4() + ":" + PortNum.ToString();
            IPAddressV4_Local = IPAddressTextBox.Text;

            // add different color for person and Guest in CTS
            // if you added a person's voice signature in config.json file, you can update GUEST to Person_Name | GUEST
            RegExp = new Regex(GUEST, RegexOptions.Compiled | RegexOptions.RightToLeft);

            Logger.Info("Form was launched.");
        }

        /// <summary>
        /// setup the device with compiled sample app and libs
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void SetupButton_Click(object sender, EventArgs e)
        {
            if (DeviceName == DDK2)
            {
                SetupAction.Deploy(SetupButton, "all", DeviceSN, DeviceName, "arm64", Logger);
            }
            else if (DeviceName == URbetter)
            {
                SetupAction.Deploy(SetupButton, "all", DeviceSN, DeviceName, "arm64_U", Logger);
            }
            else if (DeviceName == DDK1)
            {
                SetupAction.Deploy(SetupButton, "all", DeviceSN, DeviceName, "arm", Logger);
            }
        }

        /// <summary>
        /// Check if Device connected to WiFi, if not pop up message box to mention how to connect
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void WiFiButton_Click(object sender, EventArgs e)
        {
            if (SetupAction.DetectWiFiConnection(WiFiButton, DeviceName, DeviceSN, Logger))
            {
                MessageBox.Show(ConnectedToWIFIMessage, ConnectedToWIFIMessage);
            }
            else
            {
                if (DeviceName == URbetter)
                {
                    // maybe user need update the wpa_supplicant.conf file, push it in and run other two commands to connect to wifi
                    SetupAction.UpdateWPASupplicant(Logger);
                    DialogResult result = MessageBox.Show(URbetterConnectToWIFIStepsMessage, HowToConnectToWIFIMessage, MessageBoxButtons.OK);
                    if (result == DialogResult.OK)
                    {
                        SetupAction.PushNewWPASupplicant(DeviceSN, Logger);
                    }
                }
                else if (DeviceName == DDK2)
                {
                    MessageBox.Show(WIFIMessage_DDK2, HowToConnectToWIFIMessage);
                }
                else if (DeviceName == DDK1)
                {
                    MessageBox.Show(WIFIMessage_DDK1, HowToConnectToWIFIMessage);
                }
                else if (DeviceName == Eden_Win)
                {
                    MessageBox.Show(WIFIMessage_Eden, HowToConnectToWIFIMessage);
                }
            }
        }

        #region Click Speech Recognition function buttons
        private void ContinousRecognitionButton_Click(object sender, EventArgs e)
        {
            RecognitionButtonClick(ContinousRecognitionButton, ContinousRecognitionButtonText, ContinousMessage, "1", "2");
        }

        private void KeywordButton_Click(object sender, EventArgs e)
        {
            RecognitionButtonClick(KeywordButton, KeywordRecognitionButtonText, KeywordMessage, "1", "7");
        }

        private void CustomCommandsButton_Click(object sender, EventArgs e)
        {
            RecognitionButtonClick(CustomCommandsButton, CustomCommandsDialogButtonText, CustomCommandsMessage, "6", "1");
        }

        private void DirectLineButton_Click(object sender, EventArgs e)
        {
            RecognitionButtonClick(DirectLineButton, DirectLineDialogButtonText, DirectLineWaitMessage, "6", "2");
        }

        private void CTSButton_Click(object sender, EventArgs e)
        {
            RecognitionButtonClick(CTSButton, CTSButtonText, ContinousMessage, "5", "3");
        }

        private void EditConfigButton_Click(object sender, EventArgs e)
        {
            OpenConfigFile();
        }
        #endregion

        /// <summary>
        /// Apply the changes of config file to device
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void ApplyConfigButton_Click(object sender, EventArgs e)
        {
            string appDataFilePath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "config.json");

            if (File.Exists(appDataFilePath))
            {
                // push the config file to device from appdata folder
                try
                {
                    using (Process myProcess = new Process())
                    {
                        myProcess.StartInfo.UseShellExecute = false;
                        myProcess.StartInfo.FileName = "cmd.exe";
                        myProcess.StartInfo.CreateNoWindow = true;
                        myProcess.StartInfo.WorkingDirectory = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
                        myProcess.StartInfo.RedirectStandardError = true;
                        myProcess.StartInfo.Arguments = $@"/c adb -s {DeviceSN} push config.json /dev/sample/";
                        myProcess.Start();

                        string error = myProcess.StandardError.ReadToEnd();
                        myProcess.Close();
                    }
                }
                catch (Exception ex)
                {
                    //Console.WriteLine(ex.Message);
                    Logger.Error(ex, "Failed to push new wpa file to device.");
                }
            }
        }

        /// <summary>
        /// Handle the actions after clicking recognition buttons
        /// </summary>
        /// <param name="button">button clicked</param>
        /// <param name="buttonText">clicked button text</param>
        /// <param name="startMessage">prompt message</param>
        /// <param name="menu1">C++ sample code first level</param>
        /// <param name="menu2">C++ sample code second level</param>
        private void RecognitionButtonClick(Button button, string buttonText, string startMessage, string menu1, string menu2)
        {
            Logger.Info("Clicked on {button}", buttonText);
            //launch TCP server to listen
            if (!TCPServerStarted)
            {
                StartTCPServerAsync().ConfigureAwait(false);
                TCPServerStarted = true;
            }

            // share out the current click button
            CurrentButton = button;
            CurrentStartMessage = startMessage;

            DisableAllButtons();

            if (button.Text == buttonText)
            {
                // clean the result log
                //Resultlog = string.Empty;
                Contents.Clear();
                KWSContents.Clear();

                // add prompt message for each speech recognition scenarios
                Contents.Add(startMessage);

                button.Text = Loading;
                // start the sample app on device, menu 1 and menu 2 are based on the C++ console app
                Task.Run(() => RecognitionAction.StartSampleApp(menu1, menu2, DeviceName, DeviceSN, IPAddressV4_Server, PortNum.ToString(), Logger));

                // force to delay 1 second to let app starts on device
                DelayTimer.Tick += DelayStart_Tick;
                DelayTimer.Interval = DelayTimerInterval;
                DelayTimer.Start();
                Logger.Info("Started recognition on {button}", buttonText);
            }
            else if (button.Text == Stop)
            {
                // stop the sample app on device
                RecognitionAction.StopSampleAppOnDevice(DeviceName, DeviceSN, Logger);

                // if there is no result recieved, still showing initial prompt message
                if (continueRichTextBox.Text == startMessage)
                {
                    continueRichTextBox.Text = InitalMessage;
                }
                else if (CurrentButton == CTSButton)
                {
                    // color all the user name for CTS feature
                    UpdateOutputPane(false);
                }

                // handle buttons 
                EnableAllButtons();
                button.Text = buttonText;
                Logger.Info("Finished recognition on {button}", buttonText);
            }
        }

        #region Device button selection
        private void EdenWinRadioButton_CheckedChanged(object sender, EventArgs e)
        {
            DeviceName = Eden_Win;
            SetupButton.Enabled = false;
            ApplyConfigButton.Enabled = false;
        }

        private void DDK2RadioButton_CheckedChanged(object sender, EventArgs e)
        {
            DeviceName = DDK2;
            DeviceSN = DDK2;
            SetupButton.Enabled = true;
            ApplyConfigButton.Enabled = true;
        }

        private void DDK1RadioButton_CheckedChanged(object sender, EventArgs e)
        {
            DeviceName = DDK1;
            DeviceSN = DDK1_SN;
            SetupButton.Enabled = true;
            ApplyConfigButton.Enabled = true;
        }

        private void EdenLinuxRadioButton_CheckedChanged(object sender, EventArgs e)
        {
            DeviceName = URbetter;
            DeviceSN = URbetter_SN;
            SetupButton.Enabled = true;
            ApplyConfigButton.Enabled = true;
        }
        #endregion

        #region IP address settings
        /// <summary>
        /// toggle the read property of IP address text box
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void HostIPAddressCheckBox_CheckedChanged(object sender, EventArgs e)
        {
            IPAddressTextBox.ReadOnly = !IPAddressTextBox.ReadOnly;
            if (IPAddressTextBox.ReadOnly)
            {
                // restore the IP address to current host pc IP address
                IPAddressTextBox.Text = IPAddressV4_Local;
            }
        }

        /// <summary>
        /// set the new host TCP server IP and send info to device. Device will send result text to the specificed TCP server.
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void IPAddressTextBox_TextChanged(object sender, EventArgs e)
        {
            if (!IPAddressTextBox.ReadOnly)
            {
                // update the IP address where device will send TCP client message to
                IPAddressV4_Server = IPAddressTextBox.Text.Split(':')[0];
                PortNum = Convert.ToInt32(IPAddressTextBox.Text.Split(':')[1]);
            }
        }
        #endregion

        #region Helper Functions
        /// <summary>
        /// delay 1 second to let sample app starts on device
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void DelayStart_Tick(object sender, EventArgs e)
        {
            CurrentButton.Enabled = true;
            CurrentButton.Text = Stop;
            continueRichTextBox.Text = CurrentStartMessage;
            DelayTimer.Stop();
        }

        /// <summary>
        /// disable all buttons
        /// </summary>
        private void DisableAllButtons()
        {
            ContinousRecognitionButton.Enabled = false;
            KeywordButton.Enabled = false;
            CTSButton.Enabled = false;
            DirectLineButton.Enabled = false;
            CustomCommandsButton.Enabled = false;
        }

        /// <summary>
        ///  enable all buttons
        /// </summary>
        private void EnableAllButtons()
        {
            ContinousRecognitionButton.Enabled = true;
            KeywordButton.Enabled = true;
            CTSButton.Enabled = true;
            DirectLineButton.Enabled = true;
            CustomCommandsButton.Enabled = true;
        }

        /// <summary>
        /// when user press F1 key, pop up the help file in html format
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void Form1_KeyDown(object sender, KeyEventArgs e)
        {
            if (e.KeyCode.ToString() == HelpShortcutKey)
            {
                try
                {
                    using (Process myProcess = Process.Start(Path.Combine(Environment.CurrentDirectory, HelpFileName)))
                    {
                        myProcess.Close();
                    }
                }
                catch (Exception ex)
                {
                    //Console.WriteLine(FailedToOpenHelp);
                    Logger.Error(ex, "Failed to start F1 help file.");
                }
            }
        }

        /// <summary>
        ///  start the TCP Service on Host PC and listening the message from device
        ///  https://docs.microsoft.com/en-us/dotnet/framework/network-programming/using-tcp-services
        /// </summary>
        /// <returns></returns>
        private async Task StartTCPServerAsync()
        {
            Logger.Info("Started TCP server on host pc.");
            try
            {
                var listener = new TcpListener(IPAddress.Any, PortNum);
                listener.Start();

                while (!TcpServerListeningIsDone)
                {
                    Console.WriteLine("Waiting for connection...");
                    TcpClient client = await listener.AcceptTcpClientAsync().ConfigureAwait(false);

                    Console.WriteLine("Connection accepted.");
                    NetworkStream ns = client.GetStream();

                    try
                    {
                        byte[] bytes = new byte[1024];
                        int bytesRead = await ns.ReadAsync(bytes, 0, bytes.Length).ConfigureAwait(false);
                        StringBuilder sb = new StringBuilder();
                        string content = string.Empty;
                        if (bytesRead > 0)
                        {
                            sb.Append(Encoding.ASCII.GetString(
                                bytes, 0, bytesRead));

                            content = sb.ToString();

                            string[] split = content.Split('>');
                            if (split.Length >= 2)
                            {
                                // Hack for direct line echo bot to get response from bot, then update the first line of output as greeting
                                if (CurrentButton == DirectLineButton && split[1] == Greeting)
                                {
                                    Contents[0] = split[1];
                                }
                                // hack for CTS feature, update Unidentified to GUEST
                                else if (CurrentButton == CTSButton)
                                {
                                    // accept all intermediate result
                                    // if there are $ref$ userid, which means the echo channel is not empty and input audio was wrong
                                    Contents.Add(split[1].Replace(Unidentified, GUEST));
                                }
                                else
                                {
                                    Contents.Add(split[1]);
                                }
                            }

                            // update the resultlog 
                            if (Contents.Count > 0)
                            {
                                Resultlog = string.Empty;
                                foreach (string c in Contents)
                                {
                                    Resultlog += c;
                                }
                            }

                            // update the ouput pane
                            UpdateOutputPane();

                            // if the message is the intermediate result, remove the previous intermediate item
                            if (split[0] == "ing")
                            {
                                if (Contents.Count > 0)
                                {
                                    Contents.RemoveAt(Contents.Count - 1);
                                }
                            }
                        }
                        ns.Close();
                        client.Close();
                    }
                    catch (Exception ex)
                    {
                        //Console.WriteLine(e.ToString());
                        Logger.Error(ex, "Failed to get response from TCP client.");
                    }
                }
                listener.Stop();
                Logger.Info("Closed TCP listener on host pc.");
            }
            catch (Exception ex)
            {
                //Console.WriteLine(e.ToString());
                Logger.Error(ex, "Failed to launch TCP server.");
            }
        }

        /// <summary>
        /// update output pane base on TCP response
        /// </summary>
        private void UpdateOutputPane(bool onlyColorCurrentPage = true)
        {
            try
            {
                if (continueRichTextBox.InvokeRequired)
                {
                    continueRichTextBox.BeginInvoke(new Action<bool>(UpdateOutputPane), onlyColorCurrentPage);
                    return;
                }

                continueRichTextBox.Suspend();
                continueRichTextBox.Clear();
                continueRichTextBox.SelectedText = Resultlog;
                int i = 0;
                if (CurrentButton == CTSButton)
                {
                    foreach (Match match in RegExp.Matches(Resultlog))
                    {
                        // color thousands lines in richtextbox and refresh frequently cause performance issue
                        // so during recognition, only color current page where user can see
                        // when user click stop button, color all text
                        i++;
                        if (i > 25 && onlyColorCurrentPage)
                        {
                            break;
                        }
                        // select the user name and color to distinguish
                        continueRichTextBox.Select(match.Index, match.Length);
                        if (match.Value == "Person_Name :")
                        {
                            continueRichTextBox.SelectionColor = Color.Blue;
                        }
                        else if (match.Value == GUEST)
                        {
                            continueRichTextBox.SelectionColor = Color.Green;
                        }
                    }
                    continueRichTextBox.SelectionStart = continueRichTextBox.Text.Length;
                }
                continueRichTextBox.Resume();
            }
            catch (Exception ex)
            {
                //Console.WriteLine(e.Message);
                Logger.Error(ex, "Failed to update output pane.");
            }
        }

        /// <summary>
        /// config log to temp folder with name DDKLog.txt
        /// </summary>
        private void ConfigLog()
        {
            var config = new NLog.Config.LoggingConfiguration();

            // Targets where to log to: File and Console
            var logfile = new NLog.Targets.FileTarget("logfile") { FileName = Path.Combine(Path.GetTempPath(), logName) };

            // Rules for mapping loggers to targets            
            config.AddRule(LogLevel.Info, LogLevel.Fatal, logfile);

            // Apply config           
            NLog.LogManager.Configuration = config;
        }

        /// <summary>
        /// open config file with admin
        /// </summary>
        private void OpenConfigFile()
        {
            try
            {
                using (Process myProcess = new Process())
                {
                    myProcess.StartInfo.UseShellExecute = false;
                    myProcess.StartInfo.FileName = "notepad.exe";
                    myProcess.StartInfo.CreateNoWindow = true;
                    myProcess.StartInfo.WorkingDirectory = Environment.CurrentDirectory;
                    if (DeviceName == Eden_Win)
                    {
                        myProcess.StartInfo.Arguments = @" .\docker\out\Eden_Win\config.json";
                    }
                    else
                    {
                        myProcess.StartInfo.Arguments = @" .\docker\out\config.json";
                    }
                    myProcess.Start();

                    myProcess.Close();
                }
            }
            catch (Exception ex)
            {
                //Console.WriteLine(ex.Message);
                Logger.Error(ex, FailedToOpenHelp);
            }
        }

        /// <summary>
        /// read device SN from config file, in case there are customize SN on device
        /// </summary>
        private void ReadSNFromConfig()
        {
            DDK1_SN = ConfigurationManager.AppSettings["DDK1"];
            DDK2_SN = ConfigurationManager.AppSettings["DDK2"];
            URbetter_SN = ConfigurationManager.AppSettings["URbetter"];
        }
        #endregion
    }
}
