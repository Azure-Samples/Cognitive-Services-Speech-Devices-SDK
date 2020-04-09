using NLog;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net.NetworkInformation;
using System.Net.Sockets;
using System.Windows.Forms;

namespace DDKSpeechRecognition
{
    public class SetupAction
    {
        private const string InstallADBcommand = "Please download Android SDK Platform-Tools for Windows from https://dl.google.com/android/repository/platform-tools-latest-windows.zip" + "\nAdd unzipped folder to Path Environment variables\nClick Yes to download; No to cancel\nRestart the app to apply the change.";
        private static bool ADBCommandInstallation = false;

        /// <summary>
        /// Deploy the complied sample app and related libs to device
        /// </summary>
        /// <param name="button">deploy button</param>
        /// <param name="input">deploy all or just config file</param>
        /// <param name="deviceSN">selected device serial number</param>
        /// <param name="deviceName">selected device name</param>
        /// <param name="cpuType">selected device CPU type, like arm, arm64</param>
        public static void Deploy(Button button, string input, string deviceSN, string deviceName, string cpuType, Logger logger)
        {
            logger.Info("Started to deploy libs to device.");
            // disable deploy button utill all the files are copied
            button.Enabled = false;

            try
            {
                using (Process myProcess = new Process())
                {
                    myProcess.StartInfo.UseShellExecute = false;
                    myProcess.StartInfo.FileName = "cmd.exe";
                    myProcess.StartInfo.CreateNoWindow = true;
                    myProcess.StartInfo.WorkingDirectory = Environment.CurrentDirectory;
                    myProcess.StartInfo.RedirectStandardError = true;
                    // call the deploy bat file
                    myProcess.StartInfo.Arguments = $@"/c .\docker\deploy.bat " + input + " " + deviceSN + " " + deviceName + " " +cpuType;
                    myProcess.Start();

                    myProcess.WaitForExit();
                    string ouput = myProcess.StandardError.ReadToEnd();
                    button.Enabled = true;

                    myProcess.Close();
                }

                logger.Info("Finished to deploy libs to device.");
            }
            catch (Exception ex)
            {
                //Console.WriteLine(ex.Message);
                logger.Error(ex, "Failed to deploy libs to device.");
            }
        }

        /// <summary>
        /// sanity check if user has ADB tool installed
        /// </summary>
        /// <returns>if ADB tool installed</returns>
        private static bool VerifyADBInstallation(Logger logger)
        {
            try
            {
                if (!ADBCommandInstallation)
                {
                    using (Process myProcess = new Process())
                    {
                        myProcess.StartInfo.UseShellExecute = false;
                        myProcess.StartInfo.FileName = "cmd.exe";
                        myProcess.StartInfo.CreateNoWindow = true;
                        myProcess.StartInfo.WorkingDirectory = Environment.CurrentDirectory;
                        myProcess.StartInfo.RedirectStandardError = true;
                        myProcess.StartInfo.Arguments = $@"/c adb version -a";
                        myProcess.Start();

                        myProcess.WaitForExit();
                        string ouput = myProcess.StandardError.ReadToEnd();
                        if (ouput.Contains("is not recognized"))
                        {
                            ADBCommandInstallation = false;
                        }
                        else
                        {
                            ADBCommandInstallation = true;
                        }

                        myProcess.Close();
                    }
                }
            }
            catch (Exception ex)
            {
                //Console.WriteLine(ex.Message);
                logger.Error(ex, "Failed to verify adb command installation on host pc.");
            }

            return ADBCommandInstallation;
        }

        /// <summary>
        /// if user did not install ADB tool, pop up message to mention install before using this tool
        /// when clicking yes to close the message box, start to download the offical ADB tool
        /// </summary>
        public static void SanityCheckADB(Logger logger)
        {
            // sanity check for the adb command installation
            if (!VerifyADBInstallation(logger))
            {
                try
                {
                    if (MessageBox.Show(InstallADBcommand, "Please install adb command!", MessageBoxButtons.YesNo, MessageBoxIcon.Asterisk) == DialogResult.Yes)
                    {
                        Process.Start("https://dl.google.com/android/repository/platform-tools-latest-windows.zip");
                    }
                }
                catch (Exception ex)
                {
                    logger.Error(ex, "Failed to launch messagebox to help user get adb zip file.");
                }
            }
        }

        /// <summary>
        /// Detect if device connects to WiFi
        /// </summary>
        /// <param name="wifiButton">wifi button</param>
        /// <param name="deviceName">selected device name</param>
        /// <param name="deviceSN">selected device serial number</param>
        /// <returns>if devices connectes to WiFi</returns>
        public static bool DetectWiFiConnection(Button wifiButton, string deviceName, string deviceSN, Logger logger)
        {
            logger.Info("Started to detect WiFi connection on device.");
            wifiButton.Enabled = false;
            try
            {
                if (deviceName == Form1.DDK2 || deviceName == Form1.DDK1)
                {
                    using (Process myProcess = new Process())
                    {
                        myProcess.StartInfo.UseShellExecute = false;
                        myProcess.StartInfo.FileName = "cmd.exe";
                        myProcess.StartInfo.CreateNoWindow = true;
                        myProcess.StartInfo.WorkingDirectory = Environment.CurrentDirectory;
                        myProcess.StartInfo.RedirectStandardOutput = true;
                        myProcess.StartInfo.Arguments = $@"/c adb -s {deviceSN} shell ping -w1 www.microsoft.com";
                        myProcess.Start();

                        myProcess.WaitForExit();
                        wifiButton.Enabled = true;
                        logger.Info("Finished to detect WiFi connection on device.");
                        string output = myProcess.StandardOutput.ReadToEnd();
                        if (output.Contains("0 packets received, 100% packet loss"))
                        {
                            return false;
                        }
                        else if (output.Contains("0% packet loss"))
                        {
                            return true;
                        }

                        myProcess.Close();
                    }
                }
                else if (deviceName == Form1.Eden_Win)
                {
                    using (Process myProcess = new Process())
                    {
                        myProcess.StartInfo.UseShellExecute = false;
                        myProcess.StartInfo.FileName = "cmd.exe";
                        myProcess.StartInfo.CreateNoWindow = true;
                        myProcess.StartInfo.WorkingDirectory = Environment.CurrentDirectory;
                        myProcess.StartInfo.RedirectStandardOutput = true;
                        myProcess.StartInfo.Arguments = $@"/c ping www.microsoft.com -n 1";
                        myProcess.Start();

                        myProcess.WaitForExit();
                        wifiButton.Enabled = true;
                        logger.Info("Finished to detect WiFi connection on device.");
                        string output = myProcess.StandardOutput.ReadToEnd();
                        if (output.Contains("100% loss"))
                        {
                            return false;
                        }
                        else if (output.Contains("0% loss"))
                        {
                            return true;
                        }

                        myProcess.Close();
                    }
                }
                else if (deviceName == Form1.URbetter)
                {
                    using (Process myProcess = new Process())
                    {
                        myProcess.StartInfo.UseShellExecute = false;
                        myProcess.StartInfo.FileName = "cmd.exe";
                        myProcess.StartInfo.CreateNoWindow = true;
                        myProcess.StartInfo.WorkingDirectory = Environment.CurrentDirectory;
                        myProcess.StartInfo.RedirectStandardOutput = true;
                        myProcess.StartInfo.RedirectStandardError = true;
                        myProcess.StartInfo.Arguments = $@"/c adb -s {deviceSN} shell killall wpa_supplicant;wpa_supplicant -B -iwlan0 -c/data/cfg/wpa_supplicant.conf;./sbin/dhcpcd /etc/dhcpcd.conf;sleep 10;ping -w1 www.microsoft.com";
                        myProcess.Start();

                        myProcess.WaitForExit();
                        wifiButton.Enabled = true;
                        logger.Info("Finished to detect WiFi connection on device.");
                        string error = myProcess.StandardError.ReadToEnd();
                        string output = myProcess.StandardOutput.ReadToEnd();
                        if (output.Contains("0 packets received, 100% packet loss"))
                        {
                            return false;
                        }
                        else if (output.Contains("0% packet loss"))
                        {
                            return true;
                        }

                        myProcess.Close();
                    }
                }
            }
            catch (Exception ex)
            {
                //Console.WriteLine(ex.Message);
                logger.Error(ex, "Failed to detect WiFi connection on device.");
            }

            return false;
        }

        /// <summary>
        /// get all valid and using IP V4 addresses on type
        /// </summary>
        /// <param name="_type">Ethernet or Wireless80211</param>
        /// <returns>list of valid IP V4 addresses</returns>
        private static string[] GetAllLocalIPv4(NetworkInterfaceType _type)
        {
            List<string> ipAddrList = new List<string>();
            foreach (NetworkInterface item in NetworkInterface.GetAllNetworkInterfaces())
            {
                if (item.NetworkInterfaceType == _type && item.OperationalStatus == OperationalStatus.Up &&
                    !item.Description.Contains("Hyper-V") &&
                    !item.Description.Contains("VirtualBox") &&
                    !item.Description.Contains("Software") &&
                    !item.Description.Contains("Loopback"))
                {
                    foreach (UnicastIPAddressInformation ip in item.GetIPProperties().UnicastAddresses)
                    {
                        if (ip.Address.AddressFamily == AddressFamily.InterNetwork)
                        {
                            ipAddrList.Add(ip.Address.ToString());
                        }
                    }
                }
            }
            return ipAddrList.ToArray();
        }

        /// <summary>
        /// query using Ethernet or Wireless80211 network IP V4 address
        /// </summary>
        /// <returns>the default or using one</returns>
        public static string GetUsingIPV4()
        {
            var result = GetAllLocalIPv4(NetworkInterfaceType.Wireless80211);

            if (result.Length > 0)
            {
                Form1.IPAddressV4_Server = result.FirstOrDefault();
                return Form1.IPAddressV4_Server;
            }

            result = GetAllLocalIPv4(NetworkInterfaceType.Ethernet);

            if (result.Length > 0)
            {
                Form1.IPAddressV4_Server = result.FirstOrDefault();
                return Form1.IPAddressV4_Server;
            }

            MessageBox.Show(Form1.HostPCNotConnectToInternet);
            return "IP address was not found";
        }

        /// <summary>
        /// update wpa_supplicant.conf for URbetter to connect to WiFi
        /// </summary>
        public static void UpdateWPASupplicant(Logger logger)
        {
            // hack for the user who install the tool from EXE and MSI
            CopyWPAFileToAppDataFolder(logger);

            // open the config.json file with notepad 
            try
            {
                using (Process myProcess = new Process())
                {
                    myProcess.StartInfo.UseShellExecute = false;
                    myProcess.StartInfo.FileName = "notepad.exe";
                    myProcess.StartInfo.CreateNoWindow = true;
                    myProcess.StartInfo.WorkingDirectory = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
                    myProcess.StartInfo.Arguments = @" wpa_supplicant.conf";
                    myProcess.Start();

                    myProcess.Close();
                }
            }
            catch (Exception ex)
            {
                //Console.WriteLine(ex.Message);
                logger.Error(ex, "Failed to update wpa file.");
            }
        }

        /// <summary>
        /// if user install from EXE, they could not save the change under program file folder
        /// </summary>
        private static void CopyWPAFileToAppDataFolder(Logger logger)
        {
            // open the config.json file with notepad 
            string appDataPath = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "wpa_supplicant.conf");

            if (File.Exists(appDataPath))
            {
                // copied before, just open the copied one
                return;
            }
            else
            {
                try
                {
                    using (Process myProcess = new Process())
                    {
                        myProcess.StartInfo.UseShellExecute = false;
                        myProcess.StartInfo.FileName = "cmd.exe";
                        myProcess.StartInfo.CreateNoWindow = true;
                        myProcess.StartInfo.WorkingDirectory = Environment.CurrentDirectory;
                        myProcess.StartInfo.Arguments = @"/c copy .\docker\out\URbetter\NonMicrosoft\wpa_supplicant.conf " + appDataPath;
                        myProcess.Start();

                        myProcess.Close();
                    }
                }
                catch (Exception ex)
                {
                    //Console.WriteLine(ex.Message);
                    logger.Error(ex, "Failed to copy wpa file to temp folder.");
                }
            }
        }

        /// <summary>
        /// push the wpa_supplicant.conf file to URbetter device
        /// </summary>
        public static void PushNewWPASupplicant(string deviceSN, Logger logger)
        {
            // push the config.json file with to device 
            try
            {
                using (Process myProcess = new Process())
                {
                    myProcess.StartInfo.UseShellExecute = false;
                    myProcess.StartInfo.FileName = "cmd.exe";
                    myProcess.StartInfo.CreateNoWindow = true;
                    myProcess.StartInfo.WorkingDirectory = Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData);
                    myProcess.StartInfo.RedirectStandardError = true;
                    myProcess.StartInfo.Arguments = $@"/c adb -s {deviceSN} push wpa_supplicant.conf /userdata/cfg/";
                    myProcess.Start();

                    string error = myProcess.StandardError.ReadToEnd();
                    myProcess.Close();
                }
            }
            catch (Exception ex)
            {
                //Console.WriteLine(ex.Message);
                logger.Error(ex, "Failed to push new wpa file to device.");
            }
        }
    }
}
