using System.Windows;
using Concord___Definitive_Edition.Models;
using Concord___Definitive_Edition.Services;
using Microsoft.Win32;
using OpenFileDialog = Microsoft.Win32.OpenFileDialog;

namespace Concord___Definitive_Edition;

public partial class AccountManagementWindow : Window
{
    private readonly ApiService api;
    private readonly Func<Task> refreshFriends;
    private readonly Action logout;

    public AccountManagementWindow(ApiService api, Func<Task> refreshFriends, Action logout)
    {
        InitializeComponent();
        this.api            = api;
        this.refreshFriends = refreshFriends;
        this.logout         = logout;
        UsernameText.Text   = Session.Username;
        StartWithWindowsCheckBox.IsChecked = Start.IsEnabled();
    }

    private async void PhotoButton_Click(object sender, RoutedEventArgs e)
    {
        var dialog = new OpenFileDialog
        {
            Filter = "Imagens|*.png;*.jpg;*.jpeg;*.bmp",
            Title  = "Escolha sua foto de perfil"
        };

        if (dialog.ShowDialog(this) != true) return;

        try
        {
            await api.UploadProfilePhotoAsync(dialog.FileName);
            await refreshFriends();
            StatusText.Text = "Foto de perfil atualizada.";
        }
        catch (Exception ex)
        {
            StatusText.Text = ex.Message;
        }
    }

    private async void FindFriendsButton_Click(object sender, RoutedEventArgs e) => await OpenFriendsWindowAsync();
    private async void FriendRequestsButton_Click(object sender, RoutedEventArgs e) => await OpenFriendsWindowAsync();

    private async void RefreshButton_Click(object sender, RoutedEventArgs e)
    {
        await refreshFriends();
        StatusText.Text = "Contatos atualizados.";
    }

    private void LogoutButton_Click(object sender, RoutedEventArgs e)
    {
        logout();
        Close();
    }

    private async void DeleteAccountButton_Click(object sender, RoutedEventArgs e)
    {
        var confirm = System.Windows.MessageBox.Show(
            "Tem certeza que deseja excluir sua conta permanentemente?\nEsta ação não pode ser desfeita.",
            "Excluir conta",
            MessageBoxButton.YesNo,
            MessageBoxImage.Warning);

        if (confirm != MessageBoxResult.Yes) return;

        try
        {
            DeleteAccountButton.IsEnabled = false;
            StatusText.Text = "Excluindo conta...";
            await api.DeleteAccountAsync();
            logout();
            Close();
        }
        catch (Exception ex)
        {
            DeleteAccountButton.IsEnabled = true;
            StatusText.Text = ex.Message;
        }
    }

    private void StartWithWindowsCheckBox_Changed(object sender, RoutedEventArgs e)
    {
        if (!IsLoaded) return;

        try
        {
            Start.SetEnabled(StartWithWindowsCheckBox.IsChecked == true);
            StatusText.Text = StartWithWindowsCheckBox.IsChecked == true
                ? "Concord vai iniciar com o Windows."
                : "Concord não vai iniciar com o Windows.";
        }
        catch (Exception ex)
        {
            StatusText.Text = ex.Message;
            StartWithWindowsCheckBox.IsChecked = Start.IsEnabled();
        }
    }

    private async Task OpenFriendsWindowAsync()
    {
        var dialog = new FriendSearchWindow(api) { Owner = this };
        dialog.ShowDialog();
        await refreshFriends();
    }
}
