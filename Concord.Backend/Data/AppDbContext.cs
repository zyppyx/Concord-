using Microsoft.EntityFrameworkCore;
using ChatAPI.Models;

namespace ChatAPI.Data
{
    public class AppDbContext : DbContext
    {
        public DbSet<User> Users { get; set; }
        public DbSet <Friend> Friends{ get; set;}
        public DbSet<Message> Messages { get; set; }
    public DbSet<DeletedMessage> DeletedMessages { get; set; }
        public DbSet<FriendRequest> FriendRequests { get; set; }
        public AppDbContext(DbContextOptions<AppDbContext> options)
            : base(options)
        {

        }
       protected override void OnModelCreating(
    ModelBuilder modelBuilder)
{
    modelBuilder.Entity<Friend>()
        .HasOne(f => f.User)
        .WithMany(u => u.Friends)
        .HasForeignKey(f => f.UserId)
        .OnDelete(DeleteBehavior.Restrict);

    modelBuilder.Entity<Friend>()
        .HasOne(f => f.FriendUser)
        .WithMany()
        .HasForeignKey(f => f.FriendUserId)
        .OnDelete(DeleteBehavior.Restrict);

    modelBuilder.Entity<Message>()
        .HasOne(m => m.FromUser)
        .WithMany()
        .HasForeignKey(m => m.FromUserId)
        .OnDelete(DeleteBehavior.Restrict);

    modelBuilder.Entity<Message>()
        .HasOne(m => m.ToUser)
        .WithMany()
        .HasForeignKey(m => m.ToUserId)
        .OnDelete(DeleteBehavior.Restrict);

    modelBuilder.Entity<Message>()
        .HasIndex(m => new { m.FromUserId, m.ToUserId, m.SentAt });

    modelBuilder.Entity<Message>()
        .HasIndex(m => new { m.ToUserId, m.FromUserId, m.SentAt });

    modelBuilder.Entity<FriendRequest>()
        .HasOne(r => r.FromUser)
        .WithMany()
        .HasForeignKey(r => r.FromUserId)
        .OnDelete(DeleteBehavior.Restrict);

    modelBuilder.Entity<FriendRequest>()
        .HasOne(r => r.ToUser)
        .WithMany()
        .HasForeignKey(r => r.ToUserId)
        .OnDelete(DeleteBehavior.Restrict);

    modelBuilder.Entity<FriendRequest>()
        .HasIndex(r => new { r.FromUserId, r.ToUserId })
        .IsUnique();
}
        
    }
}