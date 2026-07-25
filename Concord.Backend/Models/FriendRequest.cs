namespace ChatAPI.Models
{
    public class FriendRequest
    {
        public int Id { get; set; }
        public int FromUserId { get; set; }
        public User FromUser { get; set; } = null!;
        public int ToUserId { get; set; }
        public User ToUser { get; set; } = null!;
        public DateTime CreatedAt { get; set; }
    }
}
