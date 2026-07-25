using System.IO;
using System.Text.Json;
using Concord___Definitive_Edition.Models;

namespace Concord___Definitive_Edition.Services;

/// <summary>
/// Cache local de conversas em arquivo JSON — padrão stale-while-revalidate.
/// </summary>
public sealed class ConversationCache
{
    private static readonly string CacheDir =
        Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), "Concord");

    private static readonly JsonSerializerOptions Options = new() { PropertyNameCaseInsensitive = true };

    public void Save(int userId, List<CachedConversation> conversations)
    {
        try
        {
            Directory.CreateDirectory(CacheDir);
            var path = FilePath(userId);
            File.WriteAllText(path, JsonSerializer.Serialize(conversations));
        }
        catch { /* cache é best-effort */ }
    }

    public List<CachedConversation> Load(int userId)
    {
        try
        {
            var path = FilePath(userId);
            if (!File.Exists(path)) return new();
            var json = File.ReadAllText(path);
            return JsonSerializer.Deserialize<List<CachedConversation>>(json, Options) ?? new();
        }
        catch { return new(); }
    }

    public void Clear(int userId)
    {
        try { File.Delete(FilePath(userId)); }
        catch { }
    }

    private static string FilePath(int userId) =>
        Path.Combine(CacheDir, $"conversations_{userId}.json");
}

public class CachedConversation
{
    public int FriendId { get; set; }
    public string FriendName { get; set; } = "";
    public string? ProfileImageBase64 { get; set; }
    public bool IsOnline { get; set; }
    public string LastMessage { get; set; } = "";
    public string LastMessageTime { get; set; } = "";
}
