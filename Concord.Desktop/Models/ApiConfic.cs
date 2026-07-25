namespace Concord___Definitive_Edition.Models;

public static class ApiConfic
{
    public const string BaseURL = "https://concord-api-v4xu.onrender.com";
    public static string WSURL => BaseURL.Replace("https://", "wss://");
}
