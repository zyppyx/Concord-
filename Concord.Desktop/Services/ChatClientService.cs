using System.IO;
using System.Net.WebSockets;
using System.Text;
using System.Text.Json;
using System.Text.Json.Serialization;
using Concord___Definitive_Edition.Models;

namespace Concord___Definitive_Edition.Services;

public sealed class ChatClientService : IDisposable
{
    private readonly JsonSerializerOptions jsonOptions = new()
    {
        PropertyNameCaseInsensitive = true,
        NumberHandling = JsonNumberHandling.AllowReadingFromString
    };

    private ClientWebSocket? webSocket;
    private CancellationTokenSource? cancellation;

    // Reconexão com backoff exponencial
    private int retryCount;
    private bool intentionalDisconnect;
    private Models.SessionSnapshot? currentSession;
    private System.Threading.Timer? reconnectTimer;

    public bool IsConnected => webSocket?.State == WebSocketState.Open;
    public event Action<ChatSocketMessage>? MessageReceived;
    public event Action? FriendsChanged;
    public event Action<string>? StatusChanged;

    public async Task ConnectAsync(Models.SessionSnapshot session)
    {
        intentionalDisconnect = false;
        retryCount = 0;
        currentSession = session;
        await DoConnectAsync(session);
    }

    private async Task DoConnectAsync(Models.SessionSnapshot session)
    {
        Disconnect(intentional: false);

        cancellation = new CancellationTokenSource();
        webSocket = new ClientWebSocket();

        var token = Uri.EscapeDataString(session.ApiToken);
        try
        {
            await webSocket.ConnectAsync(new Uri($"{ApiConfic.WSURL}/chat?token={token}"), cancellation.Token);
            retryCount = 0;
            StatusChanged?.Invoke($"Conectado como {session.Username}");
            _ = ReceiveLoopAsync(cancellation.Token);
        }
        catch
        {
            if (!intentionalDisconnect)
                ScheduleReconnect();
        }
    }

    private void ScheduleReconnect()
    {
        var session = currentSession;
        if (session == null) { StatusChanged?.Invoke("Chat desconectado"); return; }

        retryCount++;
        // backoff exponencial: 2s, 4s, 8s, 16s, máx 30s
        var delaySeconds = Math.Min(2 * (int)Math.Pow(2, retryCount - 1), 30);
        StatusChanged?.Invoke($"Reconectando em {delaySeconds}s...");

        reconnectTimer?.Dispose();
        reconnectTimer = new System.Threading.Timer(async _ =>
        {
            if (!intentionalDisconnect)
            {
                StatusChanged?.Invoke("Reconectando...");
                await DoConnectAsync(session);
            }
        }, null, TimeSpan.FromSeconds(delaySeconds), Timeout.InfiniteTimeSpan);
    }

    public async Task SendMessageAsync(int toUserId, string text)
    {
        if (webSocket == null || webSocket.State != WebSocketState.Open)
            throw new InvalidOperationException("Chat desconectado.");

        var payload = JsonSerializer.Serialize(new { toUserId, text });
        var bytes = Encoding.UTF8.GetBytes(payload);
        await webSocket.SendAsync(bytes, WebSocketMessageType.Text, true, cancellation?.Token ?? CancellationToken.None);
    }

    public async Task SendFileAsync(int toUserId, string mediaBase64, string mediaType, string fileName, string? caption)
    {
        if (webSocket == null || webSocket.State != WebSocketState.Open)
            throw new InvalidOperationException("Chat desconectado.");

        var payload = JsonSerializer.Serialize(new
        {
            toUserId,
            mediaBase64,
            mediaType,
            fileName,
            text = caption
        });
        var bytes = Encoding.UTF8.GetBytes(payload);
        await webSocket.SendAsync(bytes, WebSocketMessageType.Text, true, cancellation?.Token ?? CancellationToken.None);
    }

    public void Disconnect(bool intentional = true)
    {
        if (intentional)
        {
            intentionalDisconnect = true;
            currentSession = null;
        }

        reconnectTimer?.Dispose();
        reconnectTimer = null;

        try
        {
            cancellation?.Cancel();
            if (webSocket?.State == WebSocketState.Open)
                webSocket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Logout", CancellationToken.None).Wait(1000);
        }
        catch { }
        finally
        {
            webSocket?.Dispose();
            webSocket = null;
            cancellation?.Dispose();
            cancellation = null;
        }

        if (intentional)
            StatusChanged?.Invoke("Chat desconectado");
    }

    public void Dispose() => Disconnect(intentional: true);

    private async Task ReceiveLoopAsync(CancellationToken token)
    {
        var buffer = new byte[65536]; // 64KB — suficiente para mídias pequenas em base64

        while (webSocket?.State == WebSocketState.Open && !token.IsCancellationRequested)
        {
            try
            {
                using var stream = new MemoryStream();
                WebSocketReceiveResult result;

                do
                {
                    result = await webSocket.ReceiveAsync(buffer, token);
                    if (result.MessageType == WebSocketMessageType.Close)
                    {
                        // código 1000 = fechamento limpo; qualquer outro reconecta
                        if (!intentionalDisconnect && result.CloseStatus != WebSocketCloseStatus.NormalClosure)
                            ScheduleReconnect();
                        else
                            StatusChanged?.Invoke("Chat desconectado");
                        return;
                    }
                    stream.Write(buffer, 0, result.Count);
                }
                while (!result.EndOfMessage);

                var json = Encoding.UTF8.GetString(stream.ToArray());
                var message = JsonSerializer.Deserialize<ChatSocketMessage>(json, jsonOptions);

                if (message == null) continue;

                if (message.Type == "status")
                {
                    FriendsChanged?.Invoke();
                    continue;
                }

                MessageReceived?.Invoke(message);
            }
            catch
            {
                if (!token.IsCancellationRequested && !intentionalDisconnect)
                    ScheduleReconnect();
                else
                    StatusChanged?.Invoke("Chat desconectado");
                return;
            }
        }
    }
}
