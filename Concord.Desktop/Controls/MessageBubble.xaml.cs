using System.IO;
using System.Windows;
using System.Windows.Controls;
using System.Windows.Input;

using System.Windows.Media;
using System.Windows.Media.Imaging;
using Concord___Definitive_Edition.Models;
using Color = System.Windows.Media.Color;
using ColorConverter = System.Windows.Media.ColorConverter;
using HorizontalAlignment = System.Windows.HorizontalAlignment;
using SolidColorBrush = System.Windows.Media.SolidColorBrush;

namespace Concord___Definitive_Edition.Controls
{
    public partial class MessageBubble : System.Windows.Controls.UserControl
    {
        private ChatSocketMessage? _sourceMessage;

        public MessageBubble()
        {
            InitializeComponent();
            Loaded += MessageBubble_Loaded;
            MouseRightButtonUp += MessageBubble_RightClick;
            MediaImage.MouseLeftButtonUp += MediaImage_Click;
            FileBorder.MouseLeftButtonUp += FileBorder_Click;
        }

        // ── Propriedades de dependência ──────────────────────────────────────

        public static readonly DependencyProperty MessageProperty =
            DependencyProperty.Register(nameof(Message), typeof(string), typeof(MessageBubble), new PropertyMetadata(""));

        public string Message
        {
            get => (string)GetValue(MessageProperty);
            set => SetValue(MessageProperty, value);
        }

        public static readonly DependencyProperty TimeProperty =
            DependencyProperty.Register(nameof(Time), typeof(string), typeof(MessageBubble), new PropertyMetadata(""));

        public string Time
        {
            get => (string)GetValue(TimeProperty);
            set => SetValue(TimeProperty, value);
        }

        public static readonly DependencyProperty IsMineProperty =
            DependencyProperty.Register(nameof(IsMine), typeof(bool), typeof(MessageBubble), new PropertyMetadata(false, OnIsMineChanged));

        public bool IsMine
        {
            get => (bool)GetValue(IsMineProperty);
            set => SetValue(IsMineProperty, value);
        }

        // ── Evento de delete ─────────────────────────────────────────────────

        public event Action<int, bool>? DeleteRequested; // (messageId, forEveryone)

        // ── Vinculação com o modelo ──────────────────────────────────────────

        public void Bind(ChatSocketMessage msg)
        {
            _sourceMessage = msg;

            // Mensagem apagada
            if (msg.DeletedForEveryone)
            {
                Message = "🚫 Mensagem apagada";
                MessageText.FontStyle = FontStyles.Italic;
                MessageText.Foreground = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#AAFFFFFF"));
                return;
            }

            // Texto normal
            if (!string.IsNullOrWhiteSpace(msg.Text))
                Message = msg.Text;

            // Mídia
            if (!string.IsNullOrWhiteSpace(msg.MediaBase64))
            {
                var mime = (msg.MediaType ?? msg.FileMimeType ?? "").ToLowerInvariant();
                if (mime.StartsWith("image/"))
                {
                    TryShowImage(msg.MediaBase64);
                }
                else if (mime.StartsWith("audio/"))
                {
                    Message = $"🎵 {msg.FileName ?? "Áudio"}";
                }
                else
                {
                    ShowFile(msg.FileName ?? "Arquivo");
                }
            }
            else if (!string.IsNullOrWhiteSpace(msg.FileName))
            {
                ShowFile(msg.FileName);
            }
        }

        // ── Helpers ──────────────────────────────────────────────────────────

        private void TryShowImage(string base64)
        {
            try
            {
                var pureBase64 = base64.Contains(',') ? base64.Split(',')[1] : base64;
                var bytes = Convert.FromBase64String(pureBase64);
                using var ms = new MemoryStream(bytes);
                var bmp = new BitmapImage();
                bmp.BeginInit();
                bmp.CacheOption = BitmapCacheOption.OnLoad;
                bmp.StreamSource = ms;
                bmp.EndInit();
                bmp.Freeze();
                MediaImage.Source = bmp;
                MediaImage.Visibility = Visibility.Visible;
            }
            catch
            {
                Message = "🖼️ Imagem (não foi possível exibir)";
            }
        }

        private void ShowFile(string fileName)
        {
            FileNameText.Text = fileName;
            FileBorder.Visibility = Visibility.Visible;
        }

        // ── Eventos ──────────────────────────────────────────────────────────

        private void MessageBubble_Loaded(object sender, RoutedEventArgs e) => ApplySide();

        private static void OnIsMineChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
            => ((MessageBubble)d).ApplySide();

        private void ApplySide()
        {
            if (IsMine)
            {
                BubbleBorder.HorizontalAlignment = HorizontalAlignment.Right;
                BubbleBorder.Background = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#5865F2"));
            }
            else
            {
                BubbleBorder.HorizontalAlignment = HorizontalAlignment.Left;
                BubbleBorder.Background = new SolidColorBrush((Color)ColorConverter.ConvertFromString("#2B2F42"));
            }
        }

        private void MessageBubble_RightClick(object sender, MouseButtonEventArgs e)
        {
            if (_sourceMessage == null || _sourceMessage.DeletedForEveryone) return;

            var menu = new System.Windows.Controls.ContextMenu();

            if (IsMine)
            {
                var deleteForAll = new System.Windows.Controls.MenuItem
                {
                    Header = "🗑 Apagar para todos"
                };
                deleteForAll.Click += (_, _) => DeleteRequested?.Invoke(_sourceMessage.Id, true);
                menu.Items.Add(deleteForAll);
            }

            var deleteForMe = new System.Windows.Controls.MenuItem
            {
                Header = "🗑 Apagar para mim"
            };
            deleteForMe.Click += (_, _) => DeleteRequested?.Invoke(_sourceMessage.Id, false);
            menu.Items.Add(deleteForMe);

            menu.PlacementTarget = this;
            menu.IsOpen = true;
        }

        private void MediaImage_Click(object sender, MouseButtonEventArgs e)
        {
            // Abre a imagem no visualizador padrão do Windows
            if (_sourceMessage?.MediaBase64 is not string b64) return;
            try
            {
                var pureBase64 = b64.Contains(',') ? b64.Split(',')[1] : b64;
                var bytes = Convert.FromBase64String(pureBase64);
                var ext = _sourceMessage.FileMimeType?.Contains("png") == true ? ".png" : ".jpg";
                var tmp = Path.Combine(Path.GetTempPath(), $"concord_img_{Guid.NewGuid()}{ext}");
                File.WriteAllBytes(tmp, bytes);
                System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo(tmp) { UseShellExecute = true });
            }
            catch { }
        }

        private void FileBorder_Click(object sender, MouseButtonEventArgs e)
        {
            if (_sourceMessage?.MediaBase64 is not string b64) return;
            try
            {
                var pureBase64 = b64.Contains(',') ? b64.Split(',')[1] : b64;
                var bytes = Convert.FromBase64String(pureBase64);
                var fileName = _sourceMessage.FileName ?? "arquivo";
                var saveDialog = new Microsoft.Win32.SaveFileDialog { FileName = fileName };
                if (saveDialog.ShowDialog() == true)
                    File.WriteAllBytes(saveDialog.FileName, bytes);
            }
            catch { }
        }
    }
}
