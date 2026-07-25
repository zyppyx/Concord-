using System.IO;
using System.Net.Http;
using System.Net.Http.Headers;
using System.Text;
using System.Text.Json;
using Concord___Definitive_Edition.Models;

namespace Concord___Definitive_Edition.Services;

public sealed class ApiService
{
    private readonly JsonSerializerOptions jsonOptions = new()
    {
        PropertyNameCaseInsensitive = true
    };

    // ── Conectividade ────────────────────────────────────────────────────────
    public async Task<bool> IsOnlineAsync()
    {
        try
        {
            using var client = new HttpClient { Timeout = TimeSpan.FromSeconds(5) };
            var response = await client.GetAsync($"{ApiConfic.BaseURL}/");
            return response.IsSuccessStatusCode;
        }
        catch
        {
            return false;
        }
    }

    // ── Auth ─────────────────────────────────────────────────────────────────
    public async Task<LoginResponse> LoginAsync(string username, string password)
    {
        using var client = new HttpClient();
        var json = JsonSerializer.Serialize(new { username, password });
        using var content = new StringContent(json, Encoding.UTF8, "application/json");
        var response = await client.PostAsync($"{ApiConfic.BaseURL}/login", content);
        var result = await response.Content.ReadAsStringAsync();

        if (!response.IsSuccessStatusCode)
            throw new InvalidOperationException(string.IsNullOrWhiteSpace(result) ? "Login falhou." : result);

        return JsonSerializer.Deserialize<LoginResponse>(result, jsonOptions)
            ?? throw new InvalidOperationException("Resposta de login invalida.");
    }

    public async Task<string> RegisterAsync(string username, string password)
    {
        using var client = new HttpClient();
        var json = JsonSerializer.Serialize(new { Username = username, Password = password });
        using var content = new StringContent(json, Encoding.UTF8, "application/json");
        var response = await client.PostAsync($"{ApiConfic.BaseURL}/register", content);
        var result = await response.Content.ReadAsStringAsync();

        if (!response.IsSuccessStatusCode)
            throw new InvalidOperationException(string.IsNullOrWhiteSpace(result) ? "Cadastro falhou." : result);

        return "Conta criada com sucesso!";
    }

    // ── Amigos ───────────────────────────────────────────────────────────────
    public async Task<List<FriendUser>> GetFriendsAsync()
    {
        using var client = CreateAuthorizedClient();
        var response = await client.GetStringAsync($"{ApiConfic.BaseURL}/friends/{Session.UserId}");
        return JsonSerializer.Deserialize<List<FriendUser>>(response, jsonOptions) ?? new List<FriendUser>();
    }

    // ── Mensagens ────────────────────────────────────────────────────────────
    public async Task<List<ChatSocketMessage>> GetMessagesAsync(int friendId)
    {
        using var client = CreateAuthorizedClient();
        var response = await client.GetStringAsync($"{ApiConfic.BaseURL}/messages/{friendId}?take=50");
        return JsonSerializer.Deserialize<List<ChatSocketMessage>>(response, jsonOptions) ?? new List<ChatSocketMessage>();
    }

    public async Task DeleteMessageAsync(int messageId, bool forEveryone)
    {
        using var client = CreateAuthorizedClient();
        var response = await client.DeleteAsync($"{ApiConfic.BaseURL}/messages/{messageId}?forEveryone={forEveryone}");
        if (!response.IsSuccessStatusCode)
        {
            var err = await response.Content.ReadAsStringAsync();
            throw new InvalidOperationException(err.IfBlank("Não foi possível apagar a mensagem."));
        }
    }

    // ── Perfil ───────────────────────────────────────────────────────────────
    public async Task UploadProfilePhotoAsync(string fileName)
    {
        var bytes = await File.ReadAllBytesAsync(fileName);
        var extension = Path.GetExtension(fileName).ToLowerInvariant();
        var mimeType = extension == ".png" ? "image/png" : "image/jpeg";
        var profileImageBase64 = $"data:{mimeType};base64,{Convert.ToBase64String(bytes)}";

        using var client = CreateAuthorizedClient();
        var json = JsonSerializer.Serialize(new { profileImageBase64 });
        using var content = new StringContent(json, Encoding.UTF8, "application/json");
        var response = await client.PutAsync($"{ApiConfic.BaseURL}/profile/photo", content);
        var result = await response.Content.ReadAsStringAsync();

        if (!response.IsSuccessStatusCode)
            throw new InvalidOperationException(result);
    }

    public async Task<UserSearchResult?> GetOwnProfileAsync()
    {
        try
        {
            using var client = CreateAuthorizedClient();
            var response = await client.GetAsync($"{ApiConfic.BaseURL}/profile/{Session.UserId}");
            if (!response.IsSuccessStatusCode) return null;
            var json = await response.Content.ReadAsStringAsync();
            return JsonSerializer.Deserialize<UserSearchResult>(json, jsonOptions);
        }
        catch { return null; }
    }

    // ── Conta ────────────────────────────────────────────────────────────────
    public async Task DeleteAccountAsync()
    {
        using var client = CreateAuthorizedClient();
        var response = await client.DeleteAsync($"{ApiConfic.BaseURL}/deleteuser/{Session.UserId}");
        if (!response.IsSuccessStatusCode)
        {
            var err = await response.Content.ReadAsStringAsync();
            throw new InvalidOperationException(err.IfBlank("Não foi possível excluir a conta."));
        }
    }

    // ── Busca de usuários ────────────────────────────────────────────────────
    public async Task<UserSearchResult?> FindUserAsync(string idText, string usernameText)
    {
        using var client = new HttpClient();

        if (int.TryParse(idText.Trim(), out var friendId))
        {
            var response = await client.GetAsync($"{ApiConfic.BaseURL}/allusers/{friendId}");
            if (!response.IsSuccessStatusCode) return null;
            var json = await response.Content.ReadAsStringAsync();
            return JsonSerializer.Deserialize<UserSearchResult>(json, jsonOptions);
        }

        var username = usernameText.Trim();
        if (string.IsNullOrWhiteSpace(username)) return null;

        var searchName = Uri.EscapeDataString(username);
        var searchResponse = await client.GetAsync($"{ApiConfic.BaseURL}/users/search/{searchName}");
        if (!searchResponse.IsSuccessStatusCode) return null;

        var searchJson = await searchResponse.Content.ReadAsStringAsync();
        var users = JsonSerializer.Deserialize<List<UserSearchResult>>(searchJson, jsonOptions);

        return users?.FirstOrDefault(u => string.Equals(u.Username, username, StringComparison.OrdinalIgnoreCase))
            ?? users?.FirstOrDefault();
    }

    // ── Pedidos de amizade ───────────────────────────────────────────────────
    public async Task<string> SendFriendRequestAsync(int friendId)
    {
        using var client = CreateAuthorizedClient();
        var response = await client.PostAsync($"{ApiConfic.BaseURL}/friendrequests/{friendId}", null);
        var result = await response.Content.ReadAsStringAsync();

        if (!response.IsSuccessStatusCode)
            throw new InvalidOperationException(result);

        return ExtractMessage(result);
    }

    public async Task<List<FriendRequestResult>> GetIncomingRequestsAsync()
    {
        using var client = CreateAuthorizedClient();
        var response = await client.GetAsync($"{ApiConfic.BaseURL}/friendrequests/incoming");
        var result = await response.Content.ReadAsStringAsync();

        if (!response.IsSuccessStatusCode)
            throw new InvalidOperationException(result);

        return JsonSerializer.Deserialize<List<FriendRequestResult>>(result, jsonOptions) ?? new List<FriendRequestResult>();
    }

    public async Task<string> AnswerFriendRequestAsync(int requestId, bool accept)
    {
        using var client = CreateAuthorizedClient();
        var response = accept
            ? await client.PostAsync($"{ApiConfic.BaseURL}/friendrequests/{requestId}/accept", null)
            : await client.DeleteAsync($"{ApiConfic.BaseURL}/friendrequests/{requestId}");

        var result = await response.Content.ReadAsStringAsync();
        if (!response.IsSuccessStatusCode)
            throw new InvalidOperationException(result);

        return ExtractMessage(result);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    private string ExtractMessage(string raw)
    {
        if (string.IsNullOrWhiteSpace(raw)) return "";
        try
        {
            using var doc = JsonDocument.Parse(raw);
            var root = doc.RootElement;
            foreach (var key in new[] { "msg", "message", "Msg", "Message" })
                if (root.TryGetProperty(key, out var prop))
                    return prop.GetString() ?? raw;
        }
        catch { }
        return raw.Trim('"');
    }

    private static HttpClient CreateAuthorizedClient()
    {
        var client = new HttpClient();
        client.DefaultRequestHeaders.Authorization =
            new AuthenticationHeaderValue("Bearer", Session.ApiToken);
        return client;
    }
}

internal static class StringExt
{
    public static string IfBlank(this string? value, string fallback)
        => string.IsNullOrWhiteSpace(value) ? fallback : value!;
}
