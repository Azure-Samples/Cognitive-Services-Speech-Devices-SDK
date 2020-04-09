namespace DDKSpeechRecognition
{
    partial class Form1
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            System.ComponentModel.ComponentResourceManager resources = new System.ComponentModel.ComponentResourceManager(typeof(Form1));
            this.continueRichTextBox = new System.Windows.Forms.RichTextBox();
            this.groupBox1 = new System.Windows.Forms.GroupBox();
            this.CTSButton = new System.Windows.Forms.Button();
            this.DirectLineButton = new System.Windows.Forms.Button();
            this.CustomCommandsButton = new System.Windows.Forms.Button();
            this.KeywordButton = new System.Windows.Forms.Button();
            this.ContinousRecognitionButton = new System.Windows.Forms.Button();
            this.SetupGroupBox = new System.Windows.Forms.GroupBox();
            this.HostIPAddressCheckBox = new System.Windows.Forms.CheckBox();
            this.IPAddressTextBox = new System.Windows.Forms.TextBox();
            this.ApplyConfigButton = new System.Windows.Forms.Button();
            this.EditConfigButton = new System.Windows.Forms.Button();
            this.WiFiButton = new System.Windows.Forms.Button();
            this.SetupButton = new System.Windows.Forms.Button();
            this.groupBox3 = new System.Windows.Forms.GroupBox();
            this.EdenLinuxRadioButton = new System.Windows.Forms.RadioButton();
            this.EdenWinRadioButton = new System.Windows.Forms.RadioButton();
            this.DDK1RadioButton = new System.Windows.Forms.RadioButton();
            this.DDK2RadioButton = new System.Windows.Forms.RadioButton();
            this.groupBox1.SuspendLayout();
            this.SetupGroupBox.SuspendLayout();
            this.groupBox3.SuspendLayout();
            this.SuspendLayout();
            // 
            // continueRichTextBox
            // 
            this.continueRichTextBox.Font = new System.Drawing.Font("Microsoft Sans Serif", 12F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.continueRichTextBox.Location = new System.Drawing.Point(31, 281);
            this.continueRichTextBox.Name = "continueRichTextBox";
            this.continueRichTextBox.Size = new System.Drawing.Size(538, 475);
            this.continueRichTextBox.TabIndex = 1;
            this.continueRichTextBox.Text = "";
            // 
            // groupBox1
            // 
            this.groupBox1.Controls.Add(this.CTSButton);
            this.groupBox1.Controls.Add(this.DirectLineButton);
            this.groupBox1.Controls.Add(this.CustomCommandsButton);
            this.groupBox1.Controls.Add(this.KeywordButton);
            this.groupBox1.Controls.Add(this.ContinousRecognitionButton);
            this.groupBox1.Font = new System.Drawing.Font("Microsoft Sans Serif", 9F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.groupBox1.Location = new System.Drawing.Point(31, 26);
            this.groupBox1.Name = "groupBox1";
            this.groupBox1.Size = new System.Drawing.Size(333, 249);
            this.groupBox1.TabIndex = 7;
            this.groupBox1.TabStop = false;
            this.groupBox1.Text = "Recognition";
            // 
            // CTSButton
            // 
            this.CTSButton.Font = new System.Drawing.Font("Microsoft Sans Serif", 9.75F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.CTSButton.Location = new System.Drawing.Point(3, 105);
            this.CTSButton.Name = "CTSButton";
            this.CTSButton.Size = new System.Drawing.Size(326, 34);
            this.CTSButton.TabIndex = 11;
            this.CTSButton.Text = "Conversation Transcription";
            this.CTSButton.UseVisualStyleBackColor = true;
            this.CTSButton.Click += new System.EventHandler(this.CTSButton_Click);
            // 
            // DirectLineButton
            // 
            this.DirectLineButton.Font = new System.Drawing.Font("Microsoft Sans Serif", 9.75F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.DirectLineButton.Location = new System.Drawing.Point(3, 149);
            this.DirectLineButton.Name = "DirectLineButton";
            this.DirectLineButton.Size = new System.Drawing.Size(326, 34);
            this.DirectLineButton.TabIndex = 10;
            this.DirectLineButton.Text = "Direct Line dialog (preview)";
            this.DirectLineButton.UseVisualStyleBackColor = true;
            this.DirectLineButton.Click += new System.EventHandler(this.DirectLineButton_Click);
            // 
            // CustomCommandsButton
            // 
            this.CustomCommandsButton.Font = new System.Drawing.Font("Microsoft Sans Serif", 9.75F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.CustomCommandsButton.Location = new System.Drawing.Point(3, 193);
            this.CustomCommandsButton.Name = "CustomCommandsButton";
            this.CustomCommandsButton.Size = new System.Drawing.Size(326, 34);
            this.CustomCommandsButton.TabIndex = 9;
            this.CustomCommandsButton.Text = "Custom Commands dialog (preview)";
            this.CustomCommandsButton.UseVisualStyleBackColor = true;
            this.CustomCommandsButton.Click += new System.EventHandler(this.CustomCommandsButton_Click);
            // 
            // KeywordButton
            // 
            this.KeywordButton.Font = new System.Drawing.Font("Microsoft Sans Serif", 9.75F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.KeywordButton.Location = new System.Drawing.Point(3, 61);
            this.KeywordButton.Name = "KeywordButton";
            this.KeywordButton.Size = new System.Drawing.Size(326, 34);
            this.KeywordButton.TabIndex = 8;
            this.KeywordButton.Text = "Keyword Recognition";
            this.KeywordButton.UseVisualStyleBackColor = true;
            this.KeywordButton.Click += new System.EventHandler(this.KeywordButton_Click);
            // 
            // ContinousRecognitionButton
            // 
            this.ContinousRecognitionButton.Font = new System.Drawing.Font("Microsoft Sans Serif", 9.75F, System.Drawing.FontStyle.Bold, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.ContinousRecognitionButton.Location = new System.Drawing.Point(3, 17);
            this.ContinousRecognitionButton.Name = "ContinousRecognitionButton";
            this.ContinousRecognitionButton.Size = new System.Drawing.Size(326, 34);
            this.ContinousRecognitionButton.TabIndex = 7;
            this.ContinousRecognitionButton.Text = "Continuous Recognition";
            this.ContinousRecognitionButton.UseVisualStyleBackColor = true;
            this.ContinousRecognitionButton.Click += new System.EventHandler(this.ContinousRecognitionButton_Click);
            // 
            // SetupGroupBox
            // 
            this.SetupGroupBox.Controls.Add(this.HostIPAddressCheckBox);
            this.SetupGroupBox.Controls.Add(this.IPAddressTextBox);
            this.SetupGroupBox.Controls.Add(this.ApplyConfigButton);
            this.SetupGroupBox.Controls.Add(this.EditConfigButton);
            this.SetupGroupBox.Controls.Add(this.WiFiButton);
            this.SetupGroupBox.Controls.Add(this.SetupButton);
            this.SetupGroupBox.Font = new System.Drawing.Font("Microsoft Sans Serif", 9F, System.Drawing.FontStyle.Regular, System.Drawing.GraphicsUnit.Point, ((byte)(0)));
            this.SetupGroupBox.Location = new System.Drawing.Point(388, 97);
            this.SetupGroupBox.Name = "SetupGroupBox";
            this.SetupGroupBox.Size = new System.Drawing.Size(181, 178);
            this.SetupGroupBox.TabIndex = 8;
            this.SetupGroupBox.TabStop = false;
            this.SetupGroupBox.Text = "Setup Device";
            // 
            // HostIPAddressCheckBox
            // 
            this.HostIPAddressCheckBox.AutoSize = true;
            this.HostIPAddressCheckBox.Location = new System.Drawing.Point(149, 152);
            this.HostIPAddressCheckBox.Name = "HostIPAddressCheckBox";
            this.HostIPAddressCheckBox.Size = new System.Drawing.Size(15, 14);
            this.HostIPAddressCheckBox.TabIndex = 9;
            this.HostIPAddressCheckBox.UseVisualStyleBackColor = true;
            this.HostIPAddressCheckBox.CheckedChanged += new System.EventHandler(this.HostIPAddressCheckBox_CheckedChanged);
            // 
            // IPAddressTextBox
            // 
            this.IPAddressTextBox.Location = new System.Drawing.Point(19, 148);
            this.IPAddressTextBox.Name = "IPAddressTextBox";
            this.IPAddressTextBox.ReadOnly = true;
            this.IPAddressTextBox.Size = new System.Drawing.Size(128, 21);
            this.IPAddressTextBox.TabIndex = 8;
            this.IPAddressTextBox.TextChanged += new System.EventHandler(this.IPAddressTextBox_TextChanged);
            // 
            // ApplyConfigButton
            // 
            this.ApplyConfigButton.Location = new System.Drawing.Point(19, 114);
            this.ApplyConfigButton.Name = "ApplyConfigButton";
            this.ApplyConfigButton.Size = new System.Drawing.Size(145, 27);
            this.ApplyConfigButton.TabIndex = 7;
            this.ApplyConfigButton.Text = "Apply Config";
            this.ApplyConfigButton.UseVisualStyleBackColor = true;
            this.ApplyConfigButton.Click += new System.EventHandler(this.ApplyConfigButton_Click);
            // 
            // EditConfigButton
            // 
            this.EditConfigButton.Location = new System.Drawing.Point(19, 81);
            this.EditConfigButton.Name = "EditConfigButton";
            this.EditConfigButton.Size = new System.Drawing.Size(145, 27);
            this.EditConfigButton.TabIndex = 6;
            this.EditConfigButton.Text = "Edit Config";
            this.EditConfigButton.UseVisualStyleBackColor = true;
            this.EditConfigButton.Click += new System.EventHandler(this.EditConfigButton_Click);
            // 
            // WiFiButton
            // 
            this.WiFiButton.Location = new System.Drawing.Point(19, 20);
            this.WiFiButton.Name = "WiFiButton";
            this.WiFiButton.Size = new System.Drawing.Size(145, 27);
            this.WiFiButton.TabIndex = 5;
            this.WiFiButton.Text = "Connect to WiFi";
            this.WiFiButton.UseVisualStyleBackColor = true;
            this.WiFiButton.Click += new System.EventHandler(this.WiFiButton_Click);
            // 
            // SetupButton
            // 
            this.SetupButton.Location = new System.Drawing.Point(19, 51);
            this.SetupButton.Name = "SetupButton";
            this.SetupButton.Size = new System.Drawing.Size(145, 27);
            this.SetupButton.TabIndex = 4;
            this.SetupButton.Text = "Setup";
            this.SetupButton.UseVisualStyleBackColor = true;
            this.SetupButton.Click += new System.EventHandler(this.SetupButton_Click);
            // 
            // groupBox3
            // 
            this.groupBox3.Controls.Add(this.EdenLinuxRadioButton);
            this.groupBox3.Controls.Add(this.EdenWinRadioButton);
            this.groupBox3.Controls.Add(this.DDK1RadioButton);
            this.groupBox3.Controls.Add(this.DDK2RadioButton);
            this.groupBox3.Location = new System.Drawing.Point(388, 26);
            this.groupBox3.Name = "groupBox3";
            this.groupBox3.Size = new System.Drawing.Size(181, 65);
            this.groupBox3.TabIndex = 9;
            this.groupBox3.TabStop = false;
            this.groupBox3.Text = "Select Device";
            // 
            // EdenLinuxRadioButton
            // 
            this.EdenLinuxRadioButton.AutoSize = true;
            this.EdenLinuxRadioButton.Location = new System.Drawing.Point(76, 41);
            this.EdenLinuxRadioButton.Name = "EdenLinuxRadioButton";
            this.EdenLinuxRadioButton.Size = new System.Drawing.Size(63, 17);
            this.EdenLinuxRadioButton.TabIndex = 3;
            this.EdenLinuxRadioButton.Text = "Urbetter";
            this.EdenLinuxRadioButton.UseVisualStyleBackColor = true;
            this.EdenLinuxRadioButton.CheckedChanged += new System.EventHandler(this.EdenLinuxRadioButton_CheckedChanged);
            // 
            // EdenWinRadioButton
            // 
            this.EdenWinRadioButton.AutoSize = true;
            this.EdenWinRadioButton.Location = new System.Drawing.Point(76, 17);
            this.EdenWinRadioButton.Name = "EdenWinRadioButton";
            this.EdenWinRadioButton.Size = new System.Drawing.Size(103, 17);
            this.EdenWinRadioButton.TabIndex = 2;
            this.EdenWinRadioButton.Text = "Azure Kinect DK";
            this.EdenWinRadioButton.UseVisualStyleBackColor = true;
            this.EdenWinRadioButton.CheckedChanged += new System.EventHandler(this.EdenWinRadioButton_CheckedChanged);
            // 
            // DDK1RadioButton
            // 
            this.DDK1RadioButton.AutoSize = true;
            this.DDK1RadioButton.Location = new System.Drawing.Point(9, 41);
            this.DDK1RadioButton.Name = "DDK1RadioButton";
            this.DDK1RadioButton.Size = new System.Drawing.Size(54, 17);
            this.DDK1RadioButton.TabIndex = 1;
            this.DDK1RadioButton.Text = "DDK1";
            this.DDK1RadioButton.UseVisualStyleBackColor = true;
            this.DDK1RadioButton.CheckedChanged += new System.EventHandler(this.DDK1RadioButton_CheckedChanged);
            // 
            // DDK2RadioButton
            // 
            this.DDK2RadioButton.AutoSize = true;
            this.DDK2RadioButton.Checked = true;
            this.DDK2RadioButton.Location = new System.Drawing.Point(9, 17);
            this.DDK2RadioButton.Name = "DDK2RadioButton";
            this.DDK2RadioButton.Size = new System.Drawing.Size(54, 17);
            this.DDK2RadioButton.TabIndex = 0;
            this.DDK2RadioButton.TabStop = true;
            this.DDK2RadioButton.Text = "DDK2";
            this.DDK2RadioButton.UseVisualStyleBackColor = true;
            this.DDK2RadioButton.CheckedChanged += new System.EventHandler(this.DDK2RadioButton_CheckedChanged);
            // 
            // Form1
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(6F, 13F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(604, 768);
            this.Controls.Add(this.groupBox3);
            this.Controls.Add(this.SetupGroupBox);
            this.Controls.Add(this.groupBox1);
            this.Controls.Add(this.continueRichTextBox);
            this.Icon = ((System.Drawing.Icon)(resources.GetObject("$this.Icon")));
            this.Name = "Form1";
            this.Text = "DDKSpeechRecognition_v1.11 (Press F1 for help)";
            this.groupBox1.ResumeLayout(false);
            this.SetupGroupBox.ResumeLayout(false);
            this.SetupGroupBox.PerformLayout();
            this.groupBox3.ResumeLayout(false);
            this.groupBox3.PerformLayout();
            this.ResumeLayout(false);

        }

        #endregion
        private System.Windows.Forms.RichTextBox continueRichTextBox;
        private System.Windows.Forms.GroupBox groupBox1;
        private System.Windows.Forms.Button DirectLineButton;
        private System.Windows.Forms.Button CustomCommandsButton;
        private System.Windows.Forms.Button KeywordButton;
        private System.Windows.Forms.Button ContinousRecognitionButton;
        private System.Windows.Forms.GroupBox SetupGroupBox;
        private System.Windows.Forms.Button WiFiButton;
        private System.Windows.Forms.Button SetupButton;
        private System.Windows.Forms.Button CTSButton;
        private System.Windows.Forms.Button ApplyConfigButton;
        private System.Windows.Forms.Button EditConfigButton;
        private System.Windows.Forms.GroupBox groupBox3;
        private System.Windows.Forms.RadioButton EdenLinuxRadioButton;
        private System.Windows.Forms.RadioButton EdenWinRadioButton;
        private System.Windows.Forms.RadioButton DDK1RadioButton;
        private System.Windows.Forms.RadioButton DDK2RadioButton;
        private System.Windows.Forms.CheckBox HostIPAddressCheckBox;
        private System.Windows.Forms.TextBox IPAddressTextBox;
    }
}

