using System.Runtime.InteropServices;

namespace System.Windows.Forms
{
    /// <summary>
    /// This Control Extensions helps output pane stable when updating result
    /// </summary>
    public static class ControlExtensions
    {
        [System.Runtime.InteropServices.DllImport("user32.dll")]
        public static extern bool LockWindowUpdate(IntPtr hWndLock);

        public static void Suspend(this Control control)
        {
            LockWindowUpdate(control.Handle);
        }

        public static void Resume(this Control control)
        {
            LockWindowUpdate(IntPtr.Zero);
        }

        public static void AddContextMenu(this RichTextBox richTextBox)
        {
            if (richTextBox.ContextMenuStrip == null)
            {
                ContextMenuStrip cms = new ContextMenuStrip()
                {
                    ShowImageMargin = false
                };

                ToolStripMenuItem tsmiUndo = new ToolStripMenuItem("Undo");
                tsmiUndo.Click += (sender, e) => richTextBox.Undo();
                cms.Items.Add(tsmiUndo);

                ToolStripMenuItem tsmiRedo = new ToolStripMenuItem("Redo");
                tsmiRedo.Click += (sender, e) => richTextBox.Redo();
                cms.Items.Add(tsmiRedo);

                cms.Items.Add(new ToolStripSeparator());

                ToolStripMenuItem tsmiCut = new ToolStripMenuItem("Cut");
                tsmiCut.Click += (sender, e) => richTextBox.Cut();
                cms.Items.Add(tsmiCut);

                ToolStripMenuItem tsmiCopy = new ToolStripMenuItem("Copy");
                tsmiCopy.Click += (sender, e) => richTextBox.Copy();
                cms.Items.Add(tsmiCopy);

                ToolStripMenuItem tsmiPaste = new ToolStripMenuItem("Paste");
                tsmiPaste.Click += (sender, e) => richTextBox.Paste();
                cms.Items.Add(tsmiPaste);

                ToolStripMenuItem tsmiDelete = new ToolStripMenuItem("Delete");
                tsmiDelete.Click += (sender, e) => richTextBox.SelectedText = "";
                cms.Items.Add(tsmiDelete);

                cms.Items.Add(new ToolStripSeparator());

                ToolStripMenuItem tsmiSelectAll = new ToolStripMenuItem("Select All");
                tsmiSelectAll.Click += (sender, e) => richTextBox.SelectAll();
                cms.Items.Add(tsmiSelectAll);

                cms.Opening += (sender, e) =>
                {
                    tsmiUndo.Enabled = !richTextBox.ReadOnly && richTextBox.CanUndo;
                    tsmiRedo.Enabled = !richTextBox.ReadOnly && richTextBox.CanRedo;
                    tsmiCut.Enabled = !richTextBox.ReadOnly && richTextBox.SelectionLength > 0;
                    tsmiCopy.Enabled = richTextBox.SelectionLength > 0;
                    tsmiPaste.Enabled = !richTextBox.ReadOnly && Clipboard.ContainsText();
                    tsmiDelete.Enabled = !richTextBox.ReadOnly && richTextBox.SelectionLength > 0;
                    tsmiSelectAll.Enabled = richTextBox.TextLength > 0 && richTextBox.SelectionLength < richTextBox.TextLength;
                };

                richTextBox.ContextMenuStrip = cms;
            }
        }
    }
}
