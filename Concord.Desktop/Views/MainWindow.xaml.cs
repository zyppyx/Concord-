using System.IO;
using System.Windows;
using System.ComponentModel;
using System.Windows.Controls;
using System.Windows.Input;
using Concord___Definitive_Edition.Controls;
using Concord___Definitive_Edition.Models;
using Concord___Definitive_Edition.Services;
using Microsoft.Win32;
using System.Windows.Threading;
using OpenFileDialog = Microsoft.Win32.OpenFileDialog;

namespace Concord___Definitive_Edition;

public partial class MainWindow : Window
{
    private readonly ApiService api = new();
    private readonly ChatClientService chat = new();
    private readonly NotificationService notifications = new();
    private readonly ConversationCache conversationCache = new();
    private readonly NetworkMonitor networkMonitor = new();

    private readonly Dictionary<int, string> friendsById = new();
    private readonly Dictionary<int, ContactCard> contactCards = new();
    private readonly HashSet<int> seenMessageIds = new();       // deduplicação
    private readonly HashSet<int> seenRequestIds = new();

    private int? selectedFriendId;
    private string selectedFriendName = "";
    private bool suppressNotifications;
    private bool isClosingForExit;

    private DispatcherTimer? friendRequestTimer;

    public MainWindow()
    {
        InitializeComponent();

        // ── Socket ────────────────────────────────────────────────────────────
        chat.MessageReceived += message => Dispatcher.Invoke(() => AppendChatMessage(message));
        chat.FriendsChanged  += () => Dispatcher.Invoke(async () => await LoadFriendsAsync());
        chat.StatusChanged   += status => Dispatcher.Invoke(() => StatusText.Text = status);

        // ── Notificações do system tray ──────────────────────────────────────
        notifications.OpenRequested += () => Dispatcher.Invoke(ShowFromBackground);
        notifications.ExitRequested += () => Dispatcher.Invoke(() => { isClosingForExit = true; Close(); });

        // ── Monitor de rede ───────────────────────────────────────────────────
        networkMonitor.OnlineStatusChanged += isOnline => Dispatcher.Invoke(() =>
        {
            OfflineBanner.Visibility = isOnline ? Visibility.Collapsed : Visibility.Visible;

            if (isOnline && Session.IsLogged && !chat.IsConnected)
            {
                StatusText.Text = "Conexão restaurada. Reconectando...";
                _ = chat.ConnectAsync(GetCurrentSession());
            }
        });

        // Mostra banner imediatamente se já offline
        if (!networkMonitor.IsOnline)
            OfflineBanner.Visibility = Visibility.Visible;
    }

    // ── Login / Logout ────────────────────────────────────────────────────────

    private async void LoginButton_Click(object sender, RoutedEventArgs e)
    {
        if (Session.IsLogged)
        {
            await OpenAccountManagementHudAsync();
            return;
        }

        if (!networkMonitor.IsOnline)
        {
            ToastService.Show("Sem conexão com a internet.", ToastKind.Warning);
            return;
        }

        if (!await api.IsOnlineAsync())
        {
            ToastService.Show("O servidor não respondeu. Tente mais tarde.", ToastKind.Warning);
            return;
        }

        var dialog = new AuthWindow(api) { Owner = this };
        if (dialog.ShowDialog() != true) return;

        await StartLoggedExperienceAsync();
    }

    private async Task StartLoggedExperienceAsync()
    {
        UpdateLoginUi();

        // stale-while-revalidate: exibe cache imediatamente
        var cached = conversationCache.Load(Session.UserId);
        if (cached.Count > 0)
        {
            RenderCachedConversations(cached);
            LoadingText.Visibility = Visibility.Visible;
        }
        else
        {
            LoadingText.Visibility = Visibility.Visible;
        }

        await LoadFriendsAsync();
        LoadingText.Visibility = Visibility.Collapsed;

        try
        {
            await chat.ConnectAsync(GetCurrentSession());
            StartFriendRequestPolling();
        }
        catch (Exception ex)
        {
            StatusText.Text = "Login feito, mas chat desconectado";
            ToastService.Show(ex.Message, ToastKind.Warning);
        }
    }

    private static Models.SessionSnapshot GetCurrentSession() => new()
    {
        ApiToken = Session.ApiToken,
        Username = Session.Username,
        UserId = Session.UserId
    };

    // ── Carregar amigos / conversas ───────────────────────────────────────────

    private async Task LoadFriendsAsync()
    {
        if (!Session.IsLogged) return;

        try
        {
            var friends = await api.GetFriendsAsync();
            ContactsPanel.Children.Clear();
            friendsById.Clear();
            contactCards.Clear();

            foreach (var friend in friends)
            {
                friendsById[friend.Id] = friend.Username;

                var card = new ContactCard
                {
                    FriendName = friend.Username,
                    IsOnline   = friend.IsOnline,
                    IsSelected = selectedFriendId == friend.Id,
                    ProfileImage = ImageHelper.ImageFromBase64(friend.ProfileImageBase64),
                    Tag = friend
                };

                card.MouseLeftButtonUp += async (_, _) => await SelectFriendAsync(friend);
                contactCards[friend.Id] = card;
                ContactsPanel.Children.Add(card);
            }

            if (friends.Count == 0)
            {
                ChatTitle.Text = "Nenhum amigo ainda";
                StatusText.Text = "Use o botão + para procurar alguém";
            }

            // Atualiza cache
            conversationCache.Save(Session.UserId, friends.Select(f => new CachedConversation
            {
                FriendId            = f.Id,
                FriendName          = f.Username,
                ProfileImageBase64  = f.ProfileImageBase64,
                IsOnline            = f.IsOnline
            }).ToList());
        }
        catch (Exception ex)
        {
            ToastService.Show(ex.Message, ToastKind.Error);
        }
    }

    private void RenderCachedConversations(List<CachedConversation> cached)
    {
        ContactsPanel.Children.Clear();
        friendsById.Clear();
        contactCards.Clear();

        foreach (var c in cached)
        {
            friendsById[c.FriendId] = c.FriendName;

            var stub = new FriendUser
            {
                Id = c.FriendId, Username = c.FriendName,
                IsOnline = c.IsOnline, ProfileImageBase64 = c.ProfileImageBase64
            };

            var card = new ContactCard
            {
                FriendName = c.FriendName,
                IsOnline   = c.IsOnline,
                IsSelected = selectedFriendId == c.FriendId,
                ProfileImage = ImageHelper.ImageFromBase64(c.ProfileImageBase64),
                Tag = stub
            };

            card.MouseLeftButtonUp += async (_, _) => await SelectFriendAsync(stub);
            contactCards[c.FriendId] = card;
            ContactsPanel.Children.Add(card);
        }
    }

    // ── Selecionar amigo / carregar mensagens ─────────────────────────────────

    private async Task SelectFriendAsync(FriendUser friend)
    {
        selectedFriendId   = friend.Id;
        selectedFriendName = friend.Username;
        ChatTitle.Text     = friend.Username;
        StatusText.Text    = friend.IsOnline ? "Online" : "Offline";
        SendButton.IsEnabled   = chat.IsConnected;
        AttachButton.IsEnabled = chat.IsConnected;
        AudioButton.IsEnabled  = chat.IsConnected;
        MessagesPanel.Children.Clear();
        seenMessageIds.Clear();

        foreach (var card in contactCards.Values)
            card.IsSelected = card.Tag is FriendUser sel && sel.Id == friend.Id;

        // Loading amigável
        ChatLoadingText.Visibility = Visibility.Visible;

        try
        {
            var messages = await api.GetMessagesAsync(friend.Id);
            suppressNotifications = true;
            ChatLoadingText.Visibility = Visibility.Collapsed;

            foreach (var message in messages)
                AppendChatMessage(message);

            suppressNotifications = false;
        }
        catch (Exception ex)
        {
            suppressNotifications = false;
            ChatLoadingText.Visibility = Visibility.Collapsed;
            ToastService.Show(ex.Message, ToastKind.Error);
        }
    }

    // ── Enviar mensagem de texto ──────────────────────────────────────────────

    private async void SendButton_Click(object sender, RoutedEventArgs e)
        => await SendCurrentMessageAsync();

    private async void MessageTextBox_KeyDown(object sender, System.Windows.Input.KeyEventArgs e)
    {
        if (e.Key != Key.Enter) return;
        e.Handled = true;
        await SendCurrentMessageAsync();
    }

    private async Task SendCurrentMessageAsync()
    {
        if (selectedFriendId == null)
        {
            ToastService.Show("Escolha um amigo antes de enviar mensagem.", ToastKind.Info);
            return;
        }

        var text = MessageTextBox.Text.Trim();
        if (string.IsNullOrWhiteSpace(text)) return;

        try
        {
            await chat.SendMessageAsync(selectedFriendId.Value, text);
            MessageTextBox.Clear();
        }
        catch (Exception ex)
        {
            ToastService.Show(ex.Message, ToastKind.Warning);
        }
    }

    // ── Enviar foto / arquivo ─────────────────────────────────────────────────

    private async void AttachButton_Click(object sender, RoutedEventArgs e)
    {
        if (selectedFriendId == null)
        {
            ToastService.Show("Escolha um amigo antes de enviar arquivo.", ToastKind.Info);
            return;
        }

        var dialog = new OpenFileDialog
        {
            Title = "Enviar foto ou arquivo",
            Filter = "Imagens|*.png;*.jpg;*.jpeg;*.gif;*.webp|Áudio|*.mp3;*.ogg;*.wav;*.m4a|Todos os arquivos|*.*"
        };

        if (dialog.ShowDialog(this) != true) return;

        try
        {
            AttachButton.IsEnabled = false;
            SendButton.IsEnabled   = false;

            var bytes    = await File.ReadAllBytesAsync(dialog.FileName);
            var fileName = Path.GetFileName(dialog.FileName);
            var ext      = Path.GetExtension(dialog.FileName).ToLowerInvariant();
            var mime     = ExtToMime(ext);
            var base64   = Convert.ToBase64String(bytes);
            var caption  = MessageTextBox.Text.Trim().NullIfBlank();

            await chat.SendFileAsync(selectedFriendId.Value, base64, mime, fileName, caption);
            if (caption != null) MessageTextBox.Clear();

            ToastService.Show($"📎 {fileName} enviado!", ToastKind.Success);
        }
        catch (Exception ex)
        {
            ToastService.Show(ex.Message, ToastKind.Error);
        }
        finally
        {
            AttachButton.IsEnabled = chat.IsConnected;
            SendButton.IsEnabled   = chat.IsConnected;
        }
    }

    // ── Gravar e enviar áudio ─────────────────────────────────────────────────

    private async void AudioButton_Click(object sender, RoutedEventArgs e)
    {
        if (selectedFriendId == null)
        {
            ToastService.Show("Escolha um amigo antes de gravar áudio.", ToastKind.Info);
            return;
        }

        // Abre uma janela de seleção de arquivo de áudio já gravado
        // (gravação de microfone requer NAudio ou similar — aqui usamos seleção de arquivo)
        var dialog = new OpenFileDialog
        {
            Title = "Selecionar arquivo de áudio",
            Filter = "Áudio|*.mp3;*.ogg;*.wav;*.m4a;*.flac;*.aac"
        };

        if (dialog.ShowDialog(this) != true) return;

        try
        {
            AudioButton.IsEnabled = false;
            var bytes    = await File.ReadAllBytesAsync(dialog.FileName);
            var fileName = Path.GetFileName(dialog.FileName);
            var ext      = Path.GetExtension(dialog.FileName).ToLowerInvariant();
            var mime     = ExtToMime(ext);
            var base64   = Convert.ToBase64String(bytes);

            await chat.SendFileAsync(selectedFriendId.Value, base64, mime, fileName, caption: null);
            ToastService.Show($"🎵 Áudio enviado!", ToastKind.Success);
        }
        catch (Exception ex)
        {
            ToastService.Show(ex.Message, ToastKind.Error);
        }
        finally
        {
            AudioButton.IsEnabled = chat.IsConnected;
        }
    }

    // ── Receber e exibir mensagens ────────────────────────────────────────────

    private void AppendChatMessage(ChatSocketMessage message)
    {
        if (message.Type == "error")
        {
            MessagesPanel.Children.Add(new MessageBubble { Message = $"Sistema: {message.Text}", IsMine = false });
            ScrollToLastMessage();
            return;
        }

        // Deduplicação: ignora mensagem com Id já visto (eco do próprio servidor)
        if (message.Id != 0 && !seenMessageIds.Add(message.Id))
            return;

        var isMine      = message.FromUserId == Session.UserId;
        var otherUserId = isMine ? message.ToUserId : message.FromUserId;
        var sender      = isMine ? "Você" : friendsById.GetValueOrDefault(message.FromUserId, selectedFriendName);

        // Notificação nativa se fora de foco ou em outra conversa
        if (!isMine && !suppressNotifications && ShouldShowNotification(otherUserId))
        {
            var notifText = message.Text ?? (message.FileName != null ? $"[{message.FileName}]" : "[mídia]");
            notifications.ShowMessage(sender, notifText);
            ToastService.Show($"{sender}: {notifText}", ToastKind.Info);
        }

        // Só renderiza se for a conversa aberta
        if (selectedFriendId != null && selectedFriendId != otherUserId)
            return;

        var time = message.SentAt == default
            ? DateTime.Now.ToString("HH:mm")
            : message.SentAt.ToLocalTime().ToString("HH:mm");

        var label = isMine ? "Você" : sender;
        var bubble = new MessageBubble
        {
            Message = string.IsNullOrWhiteSpace(message.Text) ? "" : $"{label}: {message.Text}",
            Time    = time,
            IsMine  = isMine
        };

        bubble.Bind(message);
        bubble.DeleteRequested += async (msgId, forEveryone) => await DeleteMessageAsync(msgId, forEveryone, bubble);

        MessagesPanel.Children.Add(bubble);
        ScrollToLastMessage();
    }

    // ── Apagar mensagem ───────────────────────────────────────────────────────

    private async Task DeleteMessageAsync(int messageId, bool forEveryone, MessageBubble bubble)
    {
        if (messageId == 0)
        {
            ToastService.Show("Esta mensagem ainda não foi confirmada pelo servidor.", ToastKind.Warning);
            return;
        }

        try
        {
            await api.DeleteMessageAsync(messageId, forEveryone);

            if (forEveryone)
            {
                // Marca visualmente como apagada
                bubble.Message = "🚫 Mensagem apagada";
            }
            else
            {
                // Remove da tela
                MessagesPanel.Children.Remove(bubble);
            }
        }
        catch (Exception ex)
        {
            ToastService.Show(ex.Message, ToastKind.Error);
        }
    }

    // ── Foto de perfil ────────────────────────────────────────────────────────

    private async void PhotoButton_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFileDialog
        {
            Filter = "Imagens|*.png;*.jpg;*.jpeg;*.bmp",
            Title  = "Escolha sua foto de perfil"
        };

        if (dialog.ShowDialog(this) != true) return;

        try
        {
            await api.UploadProfilePhotoAsync(dialog.FileName);
            await LoadFriendsAsync();
            ToastService.Show("Foto de perfil atualizada!", ToastKind.Success);
        }
        catch (Exception ex)
        {
            ToastService.Show(ex.Message, ToastKind.Error);
        }
    }

    // ── Amigos ────────────────────────────────────────────────────────────────

    private async void FindFriendButton_Click(object sender, RoutedEventArgs e) => await OpenFriendManagementAsync();
    private async void FriendRequestsButton_Click(object sender, RoutedEventArgs e) => await OpenFriendManagementAsync();
    private async void RefreshButton_Click(object sender, RoutedEventArgs e) => await LoadFriendsAsync();

    private async Task OpenFriendManagementAsync()
    {
        var dialog = new FriendSearchWindow(api) { Owner = this };
        dialog.ShowDialog();
        await LoadFriendsAsync();
    }

    // ── Conta ─────────────────────────────────────────────────────────────────

    private void LogoutButton_Click(object sender, RoutedEventArgs e) => Logout();

    private async Task OpenAccountManagementHudAsync()
    {
        var hud = new AccountManagementWindow(api, LoadFriendsAsync, Logout) { Owner = this };
        hud.ShowDialog();
        await LoadFriendsAsync();
    }

    private void Logout()
    {
        chat.Disconnect();
        StopFriendRequestPolling();
        conversationCache.Clear(Session.UserId);
        Session.Clear();

        selectedFriendId   = null;
        selectedFriendName = "";
        friendsById.Clear();
        contactCards.Clear();
        seenMessageIds.Clear();
        ContactsPanel.Children.Clear();
        MessagesPanel.Children.Clear();
        ChatTitle.Text     = "Escolha uma conversa";
        StatusText.Text    = "Entre na sua conta para começar";
        SendButton.IsEnabled   = false;
        AttachButton.IsEnabled = false;
        AudioButton.IsEnabled  = false;
        UpdateLoginUi();
    }

    private void UpdateLoginUi()
    {
        LoginButton.Visibility = Visibility.Visible;
        LoginButton.ToolTip    = Session.IsLogged ? "Gerenciar conta" : "Entrar";
    }

    // ── Polling de pedidos de amizade ─────────────────────────────────────────

    private void StartFriendRequestPolling()
    {
        seenRequestIds.Clear();
        friendRequestTimer = new DispatcherTimer { Interval = TimeSpan.FromSeconds(30) };
        friendRequestTimer.Tick += async (_, _) => await CheckFriendRequestsAsync();
        friendRequestTimer.Start();
    }

    private void StopFriendRequestPolling()
    {
        friendRequestTimer?.Stop();
        friendRequestTimer = null;
    }

    private async Task CheckFriendRequestsAsync()
    {
        if (!Session.IsLogged) return;
        try
        {
            var requests = await api.GetIncomingRequestsAsync();
            foreach (var req in requests)
                if (seenRequestIds.Add(req.Id))
                    ToastService.Show($"Pedido de amizade de {req.FromUsername}!", ToastKind.Info);
        }
        catch { /* polling silencioso */ }
    }

    // ── Janela / ciclo de vida ────────────────────────────────────────────────

    private bool ShouldShowNotification(int otherUserId) =>
        !IsVisible || WindowState == WindowState.Minimized || !IsActive || selectedFriendId != otherUserId;

    private void ShowFromBackground()
    {
        Show();
        ShowInTaskbar = true;
        WindowState   = WindowState.Normal;
        Activate();
    }

    private void ScrollToLastMessage() => MessagesScrollViewer.ScrollToEnd();

    protected override void OnStateChanged(EventArgs e)
    {
        base.OnStateChanged(e);
        if (WindowState == WindowState.Minimized) { Hide(); ShowInTaskbar = false; }
    }

    protected override void OnClosing(CancelEventArgs e)
    {
        if (!isClosingForExit)
        {
            e.Cancel = true;
            Hide();
            ShowInTaskbar = false;
            ToastService.Show("Concord em segundo plano — recebendo notificações.", ToastKind.Info);
            return;
        }
        base.OnClosing(e);
    }

    protected override void OnClosed(EventArgs e)
    {
        StopFriendRequestPolling();
        chat.Dispose();
        notifications.Dispose();
        networkMonitor.Dispose();
        base.OnClosed(e);
    }

    // ── Utilitários ───────────────────────────────────────────────────────────

    private static string ExtToMime(string ext) => ext switch
    {
        ".png"  => "image/png",
        ".jpg"  => "image/jpeg",
        ".jpeg" => "image/jpeg",
        ".gif"  => "image/gif",
        ".webp" => "image/webp",
        ".mp3"  => "audio/mpeg",
        ".ogg"  => "audio/ogg",
        ".wav"  => "audio/wav",
        ".m4a"  => "audio/mp4",
        ".aac"  => "audio/aac",
        ".flac" => "audio/flac",
        ".pdf"  => "application/pdf",
        ".zip"  => "application/zip",
        _       => "application/octet-stream"
    };
}

internal static class StringExtensions
{
    public static string? NullIfBlank(this string? s) =>
        string.IsNullOrWhiteSpace(s) ? null : s;
}
