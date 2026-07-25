namespace ChatAPI.Models
{
    public class User
    {
        public int Id { get; set; }

        public string Username { get; set; }

        public string Password { get; set; }

        public DateTime AccountCreationDate {get; set;}

        public string? ProfileImageBase64 { get; set; }

        public List <Friend> Friends {get; set;} = new ();

    }
}
