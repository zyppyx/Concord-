namespace ChatAPI.Models
{
    public class Message
    {
        public int Id { get; set; }
        public int FromUserId { get; set; }
        public User FromUser { get; set; } = null!;
        public int ToUserId { get; set; }
        public User ToUser { get; set; } = null!;
        public string? Text { get; set; }
        public string? MediaBase64 { get; set; }   // base64 do arquivo/audio/imagem
        public string? MediaType { get; set; }     // ex: "image/jpeg", "audio/mp4", "application/pdf"
        public string? FileName { get; set; }      // nome original do arquivo
        public DateTime SentAt { get; set; }
        public bool DeletedForEveryone { get; set; } = false;
    }
}