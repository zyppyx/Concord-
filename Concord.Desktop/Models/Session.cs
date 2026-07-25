namespace Concord___Definitive_Edition.Models;

public static class Session
{
    public static string ApiToken { get; set; } = "";
    public static string Username { get; set; } = "";
    public static bool IsLogged { get; set; }
    public static int UserId { get; set; }
    public static string? ProfileImageBase64 { get; set; }

    public static void Clear()
    {
        ApiToken = "";
        Username = "";
        UserId = 0;
        IsLogged = false;
        ProfileImageBase64 = null;
    }
}

/// <summary>
/// Snapshot imutável da sessão — passado ao ChatClientService para reconexão
/// sem dependência no estático Session.
/// </summary>
public class SessionSnapshot
{
    public string ApiToken { get; set; } = "";
    public string Username { get; set; } = "";
    public int UserId { get; set; }
}
