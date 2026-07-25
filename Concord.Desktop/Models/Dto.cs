namespace Concord___Definitive_Edition.Models;

public class FriendUser
{
    public int Id { get; set; }
    public string Username { get; set; } = "";
    public string? ProfileImageBase64 { get; set; }
    public bool IsOnline { get; set; }
}

public class LoginResponse
{
    public int Id { get; set; }
    public string TokenString { get; set; } = "";
    public string Username { get; set; } = "";
}

public class ApiResponse
{
    public bool Success { get; set; }
    public string Msg { get; set; } = "";
}

public class ChatSocketMessage
{
    public int Id { get; set; }
    public bool Online { get; set; }
    public string Type { get; set; } = "";
    public int FromUserId { get; set; }
    public int ToUserId { get; set; }
    public string? Text { get; set; }
    public DateTime SentAt { get; set; }

    // Media / file
    public string? MediaBase64 { get; set; }
    public string? MediaType { get; set; }
    public string? FileName { get; set; }
    public string? FileUrl { get; set; }
    public string? FileMimeType { get; set; }
    public long FileSize { get; set; }

    // Delete flags
    public bool DeletedForEveryone { get; set; }
    public bool DeletedForMe { get; set; }

    // Local dedup key (set client-side before echo arrives)
    public string LocalId { get; set; } = "";
}

public class UserSearchResult
{
    public int Id { get; set; }
    public string Username { get; set; } = "";
    public DateTime AccountCreationDate { get; set; }
    public string? ProfileImageBase64 { get; set; }
}

public class FriendRequestResult
{
    public int Id { get; set; }
    public int FromUserId { get; set; }
    public string FromUsername { get; set; } = "";
    public string? FromProfileImageBase64 { get; set; }
    public DateTime CreatedAt { get; set; }
}
