namespace Concord___Definitive_Edition.Services;

public sealed class NotificationService : IDisposable
{
    private readonly System.Windows.Forms.NotifyIcon notifyIcon;
    private bool disposed;

    public NotificationService()
    {
        notifyIcon = new System.Windows.Forms.NotifyIcon
        {
            Icon = System.Drawing.SystemIcons.Application,
            Text = "Concord",
            Visible = true,
            ContextMenuStrip = BuildContextMenu()
        };

        notifyIcon.DoubleClick += (_, _) => OpenRequested?.Invoke();
    }

    public event Action? OpenRequested;
    public event Action? ExitRequested;

    public void ShowMessage(string title, string message)
    {
        if (disposed)
        {
            return;
        }

        notifyIcon.BalloonTipTitle = title;
        notifyIcon.BalloonTipText = message.Length > 180 ? $"{message[..177]}..." : message;
        notifyIcon.BalloonTipIcon = System.Windows.Forms.ToolTipIcon.Info;
        notifyIcon.ShowBalloonTip(5000);
    }

    public void Dispose()
    {
        if (disposed)
        {
            return;
        }

        disposed = true;
        notifyIcon.Visible = false;
        notifyIcon.Dispose();
    }

    private System.Windows.Forms.ContextMenuStrip BuildContextMenu()
    {
        var menu = new System.Windows.Forms.ContextMenuStrip();

        var openItem = new System.Windows.Forms.ToolStripMenuItem("Abrir Concord");
        openItem.Click += (_, _) => OpenRequested?.Invoke();

        var exitItem = new System.Windows.Forms.ToolStripMenuItem("Sair");
        exitItem.Click += (_, _) => ExitRequested?.Invoke();

        menu.Items.Add(openItem);
        menu.Items.Add(exitItem);

        return menu;
    }
}
