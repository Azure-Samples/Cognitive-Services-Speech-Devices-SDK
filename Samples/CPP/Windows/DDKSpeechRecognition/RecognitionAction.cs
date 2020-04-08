using NLog;
using System;
using System.Diagnostics;
using System.IO;
using System.Linq;

namespace DDKSpeechRecognition
{
    public class RecognitionAction
    {
        /// <summary>
        /// start the compiled sample app on devices
        /// </summary>
        /// <param name="menu1">the first level of menu in C++ source code</param>
        /// <param name="menu2">the second level of menu in C++ source code</param>
        /// <param name="DeviceName">selected device name</param>
        /// <param name="DeviceSN">selected device serial number</param>
        /// <param name="ipAddress">host pc ip address where TCP server is listening</param>
        public static void StartSampleApp(string menu1, string menu2, string DeviceName, string DeviceSN, string ipAddress, string port, Logger logger)
        {
            try
            {
                using (Process myProcess = new Process())
                {
                    myProcess.StartInfo.UseShellExecute = false;
                    myProcess.StartInfo.FileName = "cmd.exe";
                    myProcess.StartInfo.CreateNoWindow = true;
                    myProcess.StartInfo.RedirectStandardOutput = false;
                    if (DeviceName == Form1.DDK2 || DeviceName == Form1.DDK1 || DeviceName == Form1.URbetter)
                    {
                        // kill any remaining process
                        // set the LD_LIBRARY_PATH
                        // launch the compiled sampe app
                        myProcess.StartInfo.Arguments = $@"/c adb -s {DeviceSN} shell killall -9 helloworld;export LD_LIBRARY_PATH=/dev/sample/;cd /dev/sample;/dev/sample/helloworld" + " " + menu1 + " " + menu2 + " " + ipAddress + " " + port;
                    }
                    else if (DeviceName == Form1.Eden_Win)
                    {
                        // for Eden device which only has microphone, launch the sample app on the host pc 
                        myProcess.StartInfo.WorkingDirectory = Path.Combine(Environment.CurrentDirectory, @"docker\out\" + "Eden_Win");
                        myProcess.StartInfo.Arguments = @"/c ConsoleSampleCode.exe" + " " + menu1 + " " + menu2 + " " + ipAddress + " " + port;
                    }
                    myProcess.Start();
                }
            }
            catch (Exception ex)
            {
                //Console.WriteLine(e.Message);
                logger.Error(ex, "Failed to start sample app on device.");
            }
        }

        /// <summary>
        /// stop the running sample app when clicking stop button
        /// </summary>
        /// <param name="DeviceName">selected device name</param>
        /// <param name="DeviceSN">selected device serial number</param>
        public static void StopSampleAppOnDevice(string DeviceName, string DeviceSN, Logger logger)
        {
            try
            {
                if (DeviceName == Form1.DDK2 || DeviceName == Form1.URbetter)
                {
                    using (Process myProcess = new Process())
                    {
                        myProcess.StartInfo.UseShellExecute = false;
                        myProcess.StartInfo.FileName = "cmd.exe";
                        myProcess.StartInfo.CreateNoWindow = true;
                        myProcess.StartInfo.Arguments = $@"/c adb -s {DeviceSN} shell killall -9 helloworld";
                        myProcess.Start();

                        myProcess.WaitForExit();
                        myProcess.Close();
                    }
                }
                else if (DeviceName == Form1.DDK1)
                {
                    using (Process myProcess = new Process())
                    {
                        myProcess.StartInfo.UseShellExecute = false;
                        myProcess.StartInfo.FileName = "cmd.exe";
                        myProcess.StartInfo.CreateNoWindow = true;
                        myProcess.StartInfo.RedirectStandardError = true;
                        myProcess.StartInfo.RedirectStandardOutput = true;
                        myProcess.StartInfo.Arguments = $@"/c adb -s {DeviceSN} shell set ""`ps | grep helloworld`"";echo $2;kill $2";
                        myProcess.Start();

                        myProcess.WaitForExit();
                        string output = myProcess.StandardOutput.ReadToEnd();
                        string error = myProcess.StandardError.ReadToEnd();

                        myProcess.Close();
                    }
                }
                else if (DeviceName == Form1.Eden_Win)
                {
                    foreach (Process Proc in (from p in Process.GetProcesses()
                                              where p.ProcessName.Contains("ConsoleSampleCode")
                                              select p))
                    {
                        // "Kill" the process on host pc
                        Proc.Kill();
                    }
                }
            }
            catch (Exception ex)
            {
                //Console.WriteLine(e.Message);
                logger.Error(ex, "Failed to stop sample app on device.");
            }
        }
    }
}
