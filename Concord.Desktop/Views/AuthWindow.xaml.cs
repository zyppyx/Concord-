using System.Windows;
using Concord___Definitive_Edition.Models;
using Concord___Definitive_Edition.Services;

namespace Concord___Definitive_Edition;

public partial class AuthWindow : Window
{
    private readonly ApiService api;

    public AuthWindow(ApiService api)
    {
        InitializeComponent();
        this.api = api;
        UsernameBox.Focus();
    }

    private async void LoginButton_Click(object sender, RoutedEventArgs e)
    {
        

        try
        {
            var login = await api.LoginAsync(UsernameBox.Text.Trim(), GetPassword());
            Session.UserId = login.Id;
            Session.ApiToken = login.TokenString;
            Session.Username = login.Username;
            Session.IsLogged = true;

            DialogResult = true;
        }
        catch (Exception ex)
        {
            ToastService.Show(ex.Message, ToastKind.Error);
        }
    }

    private void RegisterButton_Click(object sender, RoutedEventArgs e)
    {
        var register = new RegisterWindow(api) { Owner = this };
        register.ShowDialog();
    }

    private void CancelButton_Click(object sender, RoutedEventArgs e)
    {
        DialogResult = false;
    }

    private void ShowPasswordCheckBox_Changed(object sender, RoutedEventArgs e)
    {
        if (ShowPasswordCheckBox.IsChecked == true)
        {
            PasswordTextBox.Text = PasswordBox.Password;
            PasswordTextBox.Visibility = Visibility.Visible;
            PasswordBox.Visibility = Visibility.Collapsed;
            return;
        }

        PasswordBox.Password = PasswordTextBox.Text;
        PasswordBox.Visibility = Visibility.Visible;
        PasswordTextBox.Visibility = Visibility.Collapsed;
    }

    private string GetPassword()
    {
        return ShowPasswordCheckBox.IsChecked == true
            ? PasswordTextBox.Text
            : PasswordBox.Password;
    }
}
