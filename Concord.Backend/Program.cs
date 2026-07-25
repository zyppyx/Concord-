using ChatAPI.Data;
using ChatAPI.Models;
using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using System.Text;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Collections.Concurrent;
using System.Net.WebSockets;
using System.Text.Json;
using System.Text.Json.Serialization;

var builder = WebApplication.CreateBuilder(args);
var key = builder.Configuration["Jwt:Key"];
string? connectionString;

if (!string.IsNullOrEmpty(Environment.GetEnvironmentVariable("PGHOST")))
{
    connectionString =
        $"Host={Environment.GetEnvironmentVariable("PGHOST")};" +
        $"Port={Environment.GetEnvironmentVariable("PGPORT")};" +
        $"Database={Environment.GetEnvironmentVariable("PGDATABASE")};" +
        $"Username={Environment.GetEnvironmentVariable("PGUSER")};" +
        $"Password={Environment.GetEnvironmentVariable("PGPASSWORD")}";
}
else
{
    connectionString = builder.Configuration.GetConnectionString("DefaultConnection");
}

builder.Services.AddDbContext<AppDbContext>(options =>
{
    options.UseNpgsql(connectionString);
});

builder.Services.ConfigureHttpJsonOptions(options =>
{
    options.SerializerOptions.ReferenceHandler =
        System.Text.Json.Serialization.ReferenceHandler.IgnoreCycles;
    options.SerializerOptions.Converters.Add(new DateTimeUtcConverter());
});

builder.Services
.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
.AddJwtBearer(options =>
{
    options.TokenValidationParameters =
        new TokenValidationParameters
        {
            ValidateIssuer = false,
            ValidateAudience = false,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey =
                new SymmetricSecurityKey(
                    Encoding.UTF8.GetBytes(key))
        };
});

builder.Services.AddAuthorization();
var app = builder.Build();
using (var scope = app.Services.CreateScope()){
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    db.Database.Migrate();
}
var connectedUsers = new ConcurrentDictionary<int, WebSocket>();
var tokenValidationParameters = new TokenValidationParameters
{
    ValidateIssuer = false,
    ValidateAudience = false,
    ValidateLifetime = true,
    ValidateIssuerSigningKey = true,
    IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(key))
};

app.UseAuthentication();
app.UseAuthorization();
app.UseWebSockets();

app.MapGet("/", () => "API ONLINE");

app.MapPost("/register", async (User user, AppDbContext db) =>
{
    if (string.IsNullOrWhiteSpace(user.Username) || string.IsNullOrWhiteSpace(user.Password))
        return Results.BadRequest("Nome de usuário e senha são obrigatórios.");

    var usernameExists = await db.Users
        .AnyAsync(u => u.Username.ToLower() == user.Username.ToLower().Trim());

    if (usernameExists)
        return Results.Conflict("Nome de usuário já está em uso. Escolha outro.");

    user.Username = user.Username.Trim();
    user.AccountCreationDate = DateTime.UtcNow;
    user.Password = BCrypt.Net.BCrypt.HashPassword(user.Password);
    db.Users.Add(user);
    await db.SaveChangesAsync();
    return Results.Ok(new { sucess = true, msg = "SUCESSFULY_CREATED_NEW_USER" });
});

app.MapGet("/readall", async (AppDbContext db) =>
     await db.Users
        .Select(u => new { u.Id, u.Username,
            Friends = u.Friends.Select(f => new { f.FriendUserId })
        }).ToListAsync());

app.MapGet("/allusers/{id}", async (int id, AppDbContext db) =>
{
    var user = await db.Users.Where(u => u.Id == id)
        .Select(u => new { u.Id, u.Username, u.AccountCreationDate, u.ProfileImageBase64 })
        .FirstOrDefaultAsync();
    return user == null ? Results.NotFound("Usuario nao encontrado") : Results.Ok(user);
});

app.MapGet("/users/search/{username}", async (string username, AppDbContext db) =>
{
    var users = await db.Users
        .Where(u => u.Username.Contains(username))
        .Select(u => new { u.Id, u.Username, u.AccountCreationDate, u.ProfileImageBase64 })
        .ToListAsync();
    return Results.Ok(users);
});

app.MapGet("/profile/{id}", async (int id, AppDbContext db) =>
{
    var profile = await db.Users.Where(u => u.Id == id)
        .Select(u => new { u.Id, u.Username, u.AccountCreationDate, u.ProfileImageBase64 })
        .FirstOrDefaultAsync();
    return profile == null ? Results.NotFound("Usuario nao encontrado") : Results.Ok(profile);
});

app.MapPut("/profile/photo", async (ProfilePhotoDto photo, HttpContext context, AppDbContext db) =>
{
    var currentUserId = GetCurrentUserId(context);
    if (currentUserId == null) return Results.Unauthorized();
    if (string.IsNullOrWhiteSpace(photo.ProfileImageBase64)) return Results.BadRequest("Foto invalida");
    var user = await db.Users.FindAsync(currentUserId.Value);
    if (user == null) return Results.NotFound("Usuario nao encontrado");
    user.ProfileImageBase64 = photo.ProfileImageBase64.Trim();
    await db.SaveChangesAsync();
    return Results.Ok(new { user.Id, user.Username, user.ProfileImageBase64 });
}).RequireAuthorization();

app.MapDelete("/deleteuser/{id}", async (int id, AppDbContext db) =>
{
    var user = await db.Users.FindAsync(id);
    if (user == null) return Results.NotFound("Usuário não encontrado");
    db.Friends.RemoveRange(db.Friends.Where(f => f.UserId == id || f.FriendUserId == id));
    db.Messages.RemoveRange(db.Messages.Where(m => m.FromUserId == id || m.ToUserId == id));
    db.FriendRequests.RemoveRange(db.FriendRequests.Where(r => r.FromUserId == id || r.ToUserId == id));
    db.Users.Remove(user);
    await db.SaveChangesAsync();
    return Results.Ok("Usuário deletado");
});

app.MapPost("/addfriend", async (int userId, int friendId, AppDbContext db) =>
{
    if (userId == friendId) return Results.BadRequest("Você não pode adicionar você mesmo");
    var usersExist = await db.Users.CountAsync(u => u.Id == userId || u.Id == friendId) == 2;
    if (!usersExist) return Results.NotFound("Usuário não encontrado");
    var alreadyFriends = await db.Friends.AnyAsync(f =>
        (f.UserId == userId && f.FriendUserId == friendId) ||
        (f.UserId == friendId && f.FriendUserId == userId));
    if (alreadyFriends) return Results.Conflict("Vocês já são amigos!");
    db.Friends.AddRange(
        new Friend { UserId = userId, FriendUserId = friendId },
        new Friend { UserId = friendId, FriendUserId = userId });
    await db.SaveChangesAsync();
    return Results.Ok("Amigo adicionado");
});

app.MapPost("/friendrequests/{toUserId}", async (int toUserId, HttpContext context, AppDbContext db) =>
{
    var currentUserId = GetCurrentUserId(context);
    if (currentUserId == null) return Results.Unauthorized();
    if (currentUserId.Value == toUserId) return Results.BadRequest("Voce nao pode enviar pedido para voce mesmo");
    var usersExist = await db.Users.CountAsync(u => u.Id == currentUserId.Value || u.Id == toUserId) == 2;
    if (!usersExist) return Results.NotFound("Usuario nao encontrado");
    var alreadyFriends = await db.Friends.AnyAsync(f =>
        (f.UserId == currentUserId.Value && f.FriendUserId == toUserId) ||
        (f.UserId == toUserId && f.FriendUserId == currentUserId.Value));
    if (alreadyFriends) return Results.Conflict("Voces ja sao amigos");
    var requestExists = await db.FriendRequests.AnyAsync(r =>
        (r.FromUserId == currentUserId.Value && r.ToUserId == toUserId) ||
        (r.FromUserId == toUserId && r.ToUserId == currentUserId.Value));
    if (requestExists) return Results.Conflict("Ja existe um pedido de amizade pendente");
    var request = new FriendRequest { FromUserId = currentUserId.Value, ToUserId = toUserId, CreatedAt = DateTime.UtcNow };
    db.FriendRequests.Add(request);
    await db.SaveChangesAsync();
    return Results.Ok(new { request.Id, request.FromUserId, request.ToUserId, request.CreatedAt });
}).RequireAuthorization();

app.MapGet("/friendrequests/incoming", async (HttpContext context, AppDbContext db) =>
{
    var currentUserId = GetCurrentUserId(context);
    if (currentUserId == null) return Results.Unauthorized();
    var requests = await db.FriendRequests
        .Where(r => r.ToUserId == currentUserId.Value)
        .Include(r => r.FromUser)
        .OrderByDescending(r => r.CreatedAt)
        .Select(r => new { r.Id, r.FromUserId, FromUsername = r.FromUser.Username, FromProfileImageBase64 = r.FromUser.ProfileImageBase64, r.CreatedAt })
        .ToListAsync();
    return Results.Ok(requests);
}).RequireAuthorization();

app.MapPost("/friendrequests/{requestId}/accept", async (int requestId, HttpContext context, AppDbContext db) =>
{
    var currentUserId = GetCurrentUserId(context);
    if (currentUserId == null) return Results.Unauthorized();
    var request = await db.FriendRequests.FindAsync(requestId);
    if (request == null || request.ToUserId != currentUserId.Value) return Results.NotFound("Pedido de amizade nao encontrado");
    var alreadyFriends = await db.Friends.AnyAsync(f =>
        (f.UserId == request.FromUserId && f.FriendUserId == request.ToUserId) ||
        (f.UserId == request.ToUserId && f.FriendUserId == request.FromUserId));
    if (!alreadyFriends)
    {
        db.Friends.AddRange(
            new Friend { UserId = request.FromUserId, FriendUserId = request.ToUserId },
            new Friend { UserId = request.ToUserId, FriendUserId = request.FromUserId });
    }
    db.FriendRequests.Remove(request);
    await db.SaveChangesAsync();
    return Results.Ok("Pedido aceito");
}).RequireAuthorization();

app.MapDelete("/friendrequests/{requestId}", async (int requestId, HttpContext context, AppDbContext db) =>
{
    var currentUserId = GetCurrentUserId(context);
    if (currentUserId == null) return Results.Unauthorized();
    var request = await db.FriendRequests.FindAsync(requestId);
    if (request == null || (request.ToUserId != currentUserId.Value && request.FromUserId != currentUserId.Value))
        return Results.NotFound("Pedido de amizade nao encontrado");
    db.FriendRequests.Remove(request);
    await db.SaveChangesAsync();
    return Results.Ok("Pedido removido");
}).RequireAuthorization();

app.Map("/chat", async (HttpContext context, AppDbContext db) =>
{
    if (!context.WebSockets.IsWebSocketRequest)
    {
        context.Response.StatusCode = StatusCodes.Status400BadRequest;
        await context.Response.WriteAsync("Use uma conexao WebSocket.");
        return;
    }

    var token = context.Request.Query["token"].ToString();
    var currentUserId = GetUserIdFromToken(token, tokenValidationParameters);
    if (currentUserId == null) { context.Response.StatusCode = StatusCodes.Status401Unauthorized; return; }

    using var socket = await context.WebSockets.AcceptWebSocketAsync();
    connectedUsers[currentUserId.Value] = socket;
    await BroadcastStatus(currentUserId.Value, true);

    try
    {
        while (socket.State == WebSocketState.Open)
        {
            var rawMessage = await ReceiveTextMessage(socket);
            if (rawMessage == null) break;

            var message = JsonSerializer.Deserialize<ChatMessageDto>(rawMessage,
                new JsonSerializerOptions { PropertyNameCaseInsensitive = true });

            if (message == null || message.ToUserId <= 0)
            {
                await SendTextMessage(socket, JsonSerializer.Serialize(new { type = "error", text = "Mensagem invalida" }));
                continue;
            }

            // valida: precisa ter texto OU mídia
            var hasText = !string.IsNullOrWhiteSpace(message.Text);
            var hasMedia = !string.IsNullOrWhiteSpace(message.MediaBase64) && !string.IsNullOrWhiteSpace(message.MediaType);
            if (!hasText && !hasMedia)
            {
                await SendTextMessage(socket, JsonSerializer.Serialize(new { type = "error", text = "Mensagem invalida" }));
                continue;
            }

            var areFriends = await db.Friends.AnyAsync(f =>
                (f.UserId == currentUserId.Value && f.FriendUserId == message.ToUserId) ||
                (f.UserId == message.ToUserId && f.FriendUserId == currentUserId.Value));

            if (!areFriends)
            {
                await SendTextMessage(socket, JsonSerializer.Serialize(new { type = "error", text = "Voce so pode enviar mensagens para amigos" }));
                continue;
            }

            var sentAt = DateTime.UtcNow;
            var storedMessage = new Message
            {
                FromUserId = currentUserId.Value,
                ToUserId = message.ToUserId,
                Text = hasText ? message.Text!.Trim() : null,
                MediaBase64 = hasMedia ? message.MediaBase64 : null,
                MediaType = hasMedia ? message.MediaType : null,
                FileName = message.FileName,
                SentAt = sentAt
            };

            db.Messages.Add(storedMessage);
            await db.SaveChangesAsync();

            var payload = JsonSerializer.Serialize(new ChatMessageResponseDto
            {
                Type = "message",
                Id = storedMessage.Id,
                FromUserId = currentUserId.Value,
                ToUserId = message.ToUserId,
                Text = storedMessage.Text,
                MediaBase64 = storedMessage.MediaBase64,
                MediaType = storedMessage.MediaType,
                FileName = storedMessage.FileName,
                SentAt = sentAt
            });

            await SendTextMessage(socket, payload);

            if (connectedUsers.TryGetValue(message.ToUserId, out var destination) &&
                destination.State == WebSocketState.Open)
                await SendTextMessage(destination, payload);
        }
    }
    finally
    {
        connectedUsers.TryRemove(currentUserId.Value, out _);
        await BroadcastStatus(currentUserId.Value, false);
    }
});

app.MapGet("/messages/{friendId}", async (int friendId, int? take, HttpContext context, AppDbContext db) =>
{
    var currentUserIdText = context.User.FindFirstValue(ClaimTypes.NameIdentifier);
    if (!int.TryParse(currentUserIdText, out var currentUserId)) return Results.Unauthorized();

    var areFriends = await db.Friends.AnyAsync(f =>
        (f.UserId == currentUserId && f.FriendUserId == friendId) ||
        (f.UserId == friendId && f.FriendUserId == currentUserId));
    if (!areFriends) return Results.Forbid();

    var messageLimit = Math.Clamp(take ?? 50, 1, 100);

    var messages = await db.Messages
        .Where(m =>
            ((m.FromUserId == currentUserId && m.ToUserId == friendId) ||
             (m.FromUserId == friendId && m.ToUserId == currentUserId)) &&
            !db.DeletedMessages.Any(d => d.MessageId == m.Id && d.UserId == currentUserId))
        .OrderByDescending(m => m.SentAt)
        .Take(messageLimit)
        .OrderBy(m => m.SentAt)
        .Select(m => new ChatMessageResponseDto
        {
            Type = "message",
            Id = m.Id,
            FromUserId = m.FromUserId,
            ToUserId = m.ToUserId,
            Text = m.DeletedForEveryone ? null : m.Text,
            MediaBase64 = m.DeletedForEveryone ? null : m.MediaBase64,
            MediaType = m.DeletedForEveryone ? null : m.MediaType,
            FileName = m.DeletedForEveryone ? null : m.FileName,
            DeletedForEveryone = m.DeletedForEveryone,
            SentAt = m.SentAt
        })
        .ToListAsync();

    return Results.Ok(messages);
}).RequireAuthorization();

app.MapDelete("/messages/{id}", async (int id, bool? forEveryone, HttpContext context, AppDbContext db) =>
{
    var currentUserIdText = context.User.FindFirstValue(ClaimTypes.NameIdentifier);
    if (!int.TryParse(currentUserIdText, out var currentUserId)) return Results.Unauthorized();

    var message = await db.Messages.FindAsync(id);
    if (message == null) return Results.NotFound("Mensagem não encontrada.");

    if (forEveryone == true)
    {
        if (message.FromUserId != currentUserId) return Results.Forbid();
        message.DeletedForEveryone = true;
        await db.SaveChangesAsync();
        return Results.Ok(new { deletedForEveryone = true, messageId = id });
    }

    if (message.FromUserId != currentUserId && message.ToUserId != currentUserId) return Results.Forbid();

    var alreadyDeleted = await db.DeletedMessages.AnyAsync(d => d.MessageId == id && d.UserId == currentUserId);
    if (!alreadyDeleted)
    {
        db.DeletedMessages.Add(new DeletedMessage { MessageId = id, UserId = currentUserId });
        await db.SaveChangesAsync();
    }

    return Results.Ok(new { deletedForMe = true, messageId = id });
}).RequireAuthorization();

app.MapGet("/friends/{userId}", async (int userId, AppDbContext db) =>
{
    var friends = await db.Friends
        .Where(f => f.UserId == userId || f.FriendUserId == userId)
        .Include(f => f.User).Include(f => f.FriendUser)
        .ToListAsync();

    var result = friends.Select(f => new
    {
        Id = f.UserId == userId ? f.FriendUser.Id : f.User.Id,
        Username = f.UserId == userId ? f.FriendUser.Username : f.User.Username,
        ProfileImageBase64 = f.UserId == userId ? f.FriendUser.ProfileImageBase64 : f.User.ProfileImageBase64,
        IsOnline = connectedUsers.ContainsKey(f.UserId == userId ? f.FriendUser.Id : f.User.Id)
    }).Distinct().ToList();

    return Results.Ok(result);
});

app.MapPost("/login", async (User login, AppDbContext db) =>
{
    var user = await db.Users.FirstOrDefaultAsync(x => x.Username == login.Username);
    if (user == null) return Results.BadRequest("Usuário não encontrado");
    bool correctPassword = BCrypt.Net.BCrypt.Verify(login.Password, user.Password);
    if (!correctPassword) return Results.BadRequest("Senha incorreta");

    var claims = new[]
    {
        new Claim(ClaimTypes.NameIdentifier, user.Id.ToString()),
        new Claim(ClaimTypes.Name, user.Username)
    };

    var signingKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(key));
    var creds = new SigningCredentials(signingKey, SecurityAlgorithms.HmacSha256);
    var token = new JwtSecurityToken(claims: claims, expires: DateTime.UtcNow.AddDays(7), signingCredentials: creds);
    var tokenString = new JwtSecurityTokenHandler().WriteToken(token);

    return Results.Ok(new { user.Id, user.Username, tokenString });
});

var port = Environment.GetEnvironmentVariable("PORT") ?? "8080";
app.Urls.Add($"http://0.0.0.0:{port}");
app.Run();

static int? GetUserIdFromToken(string token, TokenValidationParameters validationParameters)
{
    if (string.IsNullOrWhiteSpace(token)) return null;
    try
    {
        var handler = new JwtSecurityTokenHandler();
        var principal = handler.ValidateToken(token, validationParameters, out _);
        var id = principal.FindFirstValue(ClaimTypes.NameIdentifier);
        return int.TryParse(id, out var userId) ? userId : null;
    }
    catch { return null; }
}

static int? GetCurrentUserId(HttpContext context)
{
    var userIdText = context.User.FindFirstValue(ClaimTypes.NameIdentifier);
    return int.TryParse(userIdText, out var userId) ? userId : null;
}

static async Task<string?> ReceiveTextMessage(WebSocket socket)
{
    var buffer = new byte[1024 * 1024 * 10]; // 10MB para suportar mídia
    using var stream = new MemoryStream();
    while (true)
    {
        var result = await socket.ReceiveAsync(buffer, CancellationToken.None);
        if (result.MessageType == WebSocketMessageType.Close)
        {
            if (socket.State == WebSocketState.Open || socket.State == WebSocketState.CloseReceived)
                await socket.CloseAsync(WebSocketCloseStatus.NormalClosure, "Closing", CancellationToken.None);
            return null;
        }
        stream.Write(buffer, 0, result.Count);
        if (result.EndOfMessage) return Encoding.UTF8.GetString(stream.ToArray());
    }
}

static Task SendTextMessage(WebSocket socket, string text)
{
    var bytes = Encoding.UTF8.GetBytes(text);
    return socket.SendAsync(bytes, WebSocketMessageType.Text, true, CancellationToken.None);
}

async Task BroadcastStatus(int userId, bool online)
{
    var payload = JsonSerializer.Serialize(new { type = "status", fromUserId = userId, online });
    var tasks = connectedUsers.Values
        .Where(s => s.State == WebSocketState.Open)
        .Select(s => SendTextMessage(s, payload));
    await Task.WhenAll(tasks);
}

public class ChatMessageDto
{
    public int ToUserId { get; set; }
    public string? Text { get; set; }
    public string? MediaBase64 { get; set; }
    public string? MediaType { get; set; }
    public string? FileName { get; set; }
}

public class ChatMessageResponseDto
{
    public string Type { get; set; } = "message";
    public int Id { get; set; }
    public int FromUserId { get; set; }
    public int ToUserId { get; set; }
    public string? Text { get; set; }
    public string? MediaBase64 { get; set; }
    public string? MediaType { get; set; }
    public string? FileName { get; set; }
    public bool DeletedForEveryone { get; set; }
    public DateTime SentAt { get; set; }
}

public class ProfilePhotoDto
{
    public string ProfileImageBase64 { get; set; } = "";
}

public class DateTimeUtcConverter : JsonConverter<DateTime>
{
    public override DateTime Read(ref Utf8JsonReader reader, Type t, JsonSerializerOptions o)
        => DateTime.Parse(reader.GetString()!).ToUniversalTime();
    public override void Write(Utf8JsonWriter writer, DateTime value, JsonSerializerOptions o)
        => writer.WriteStringValue(value.ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ssZ"));
}