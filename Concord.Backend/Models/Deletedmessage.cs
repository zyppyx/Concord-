namespace ChatAPI.Models
{
    // registra quando um usuário apaga uma mensagem só para si
    public class DeletedMessage
    {
        public int Id { get; set; }
        public int MessageId { get; set; }
        public Message Message { get; set; } = null!;
        public int UserId { get; set; }
        public User User { get; set; } = null!;
    }
}