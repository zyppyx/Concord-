using System.Windows;
using System.Windows.Media;
using Concord___Definitive_Edition.Services;
using static Concord___Definitive_Edition.Services.ToastService;

namespace Concord___Definitive_Edition;

public partial class RegisterWindow : Window
{
    private bool UsernameClicked = false;
    private bool PassClicked = false;
    private bool ConfPassClicked = false;
    private readonly ApiService api;

    public RegisterWindow(ApiService api)
    {
        InitializeComponent();
        this.api = api;
        UsernameBox.Focus();
        
    }

    private async void CreateButton_Click(object sender, RoutedEventArgs e)
    {
        

        var password = GetPassword();
        var confirmPassword = GetConfirmPassword();

        if (password != confirmPassword)
        {
            ToastService.Show("As Senhas Não coincidem!", ToastKind.Warning);
            return;
        }

        try
        {
            var result = await api.RegisterAsync(UsernameBox.Text.Trim(), password);
            ToastService.Show("Conta criada com sucesso!", ToastKind.Success);
            DialogResult = true;
        }
        catch (Exception ex)
        {
            ToastService.Show(ex.Message, ToastKind.Error);
        }
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
            ConfirmPasswordTextBox.Text = ConfirmPasswordBox.Password;
            PasswordTextBox.Visibility = Visibility.Visible;
            ConfirmPasswordTextBox.Visibility = Visibility.Visible;
            PasswordBox.Visibility = Visibility.Collapsed;
            ConfirmPasswordBox.Visibility = Visibility.Collapsed;
            return;
        }

        PasswordBox.Password = PasswordTextBox.Text;
        ConfirmPasswordBox.Password = ConfirmPasswordTextBox.Text;
        PasswordBox.Visibility = Visibility.Visible;
        ConfirmPasswordBox.Visibility = Visibility.Visible;
        PasswordTextBox.Visibility = Visibility.Collapsed;
        ConfirmPasswordTextBox.Visibility = Visibility.Collapsed;
    }

    private string GetPassword()
    {
        return ShowPasswordCheckBox.IsChecked == true
            ? PasswordTextBox.Text
            : PasswordBox.Password;
    }

    private string GetConfirmPassword()
    {
        return ShowPasswordCheckBox.IsChecked == true
            ? ConfirmPasswordTextBox.Text
            : ConfirmPasswordBox.Password;
    }

    private void UsernameBox_PreviewMouseDown(object sender, System.Windows.Input.MouseButtonEventArgs e)
    {
        UsernameClicked = true;
        UsernameBox.Foreground = System.Windows.Media.Brushes.White;
        OverrideTextBoxText();
    }
    private void OverrideTextBoxText()
    {
        if(UsernameClicked = true) {
            if (UsernameBox.Text == "Nome de Usuário")
            {
                UsernameBox.Text = null;
            }
        }
        

    }
}