using System.Windows;
using Concord___Definitive_Edition.Models;
using Concord___Definitive_Edition.Services;

namespace Concord___Definitive_Edition;

public partial class FriendSearchWindow : Window
{
    private readonly ApiService api;
    private readonly List<FriendRequestResult> pendingRequests = new();
    private UserSearchResult? selectedUser;

    public FriendSearchWindow(ApiService api)
    {
        InitializeComponent();
        this.api = api;
        Loaded += async (_, _) => await LoadIncomingRequestsAsync();
    }

    private async void SearchButton_Click(object sender, RoutedEventArgs e)
    {
        StatusText.Text = "";
        selectedUser = null;
        SendRequestButton.IsEnabled = false;

        try
        {
            selectedUser = await api.FindUserAsync(IdBox.Text, UsernameBox.Text);
            if (selectedUser == null)
            {
                ResultNameText.Text = "Usuário não encontrado";
                ResultInfoText.Text = "Tente outro ID ou nome.";
                ResultImageBrush.ImageSource = null;
                return;
            }

            ResultNameText.Text = selectedUser.Username;
            ResultInfoText.Text = $"ID {selectedUser.Id} - criado em {selectedUser.AccountCreationDate:dd/MM/yyyy}";
            ResultImageBrush.ImageSource = ImageHelper.ImageFromBase64(selectedUser.ProfileImageBase64);
            SendRequestButton.IsEnabled = true;
        }
        catch (Exception ex)
        {
            ToastService.Show(ex.Message, ToastKind.Error);
        }
    }

    private async void SendRequestButton_Click(object sender, RoutedEventArgs e)
    {
        if (selectedUser == null)
        {
            return;
        }

        try
        {
            await api.SendFriendRequestAsync(selectedUser.Id);
            ToastService.Show("Pedido de amizade enviado!", ToastKind.Success);
            SendRequestButton.IsEnabled = false;
        }
        catch (Exception ex)
        {
            ToastService.Show(ex.Message, ToastKind.Error);
        }
    }

    private async void RefreshRequestsButton_Click(object sender, RoutedEventArgs e)
    {
        await LoadIncomingRequestsAsync();
    }

    private async void AcceptRequestButton_Click(object sender, RoutedEventArgs e)
    {
        await AnswerSelectedRequestAsync(true);
    }

    private async void RejectRequestButton_Click(object sender, RoutedEventArgs e)
    {
        await AnswerSelectedRequestAsync(false);
    }

    private async Task LoadIncomingRequestsAsync()
    {
        try
        {
            pendingRequests.Clear();
            RequestsList.Items.Clear();

            var requests = await api.GetIncomingRequestsAsync();
            pendingRequests.AddRange(requests);

            foreach (var request in pendingRequests)
            {
                RequestsList.Items.Add($"{request.FromUsername} (ID {request.FromUserId})");
            }

            StatusText.Text = pendingRequests.Count == 0 ? "Nenhum pedido recebido." : "";
        }
        catch (Exception ex)
        {
            ToastService.Show(ex.Message, ToastKind.Error);
        }
    }

    private async Task AnswerSelectedRequestAsync(bool accept)
    {
        if (RequestsList.SelectedIndex < 0 || RequestsList.SelectedIndex >= pendingRequests.Count)
        {
            ToastService.Show("Selecione um pedido primeiro.", ToastKind.Warning);
            return;
        }

        try
        {
            var request = pendingRequests[RequestsList.SelectedIndex];
            await api.AnswerFriendRequestAsync(request.Id, accept);
            ToastService.Show(accept ? "Pedido aceito!" : "Pedido recusado.", accept ? ToastKind.Success : ToastKind.Info);
            await LoadIncomingRequestsAsync();
        }
        catch (Exception ex)
        {
            ToastService.Show(ex.Message, ToastKind.Error);
        }
    }
}