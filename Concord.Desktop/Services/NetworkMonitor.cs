using System.Net.NetworkInformation;

namespace Concord___Definitive_Edition.Services;

/// <summary>
/// Monitora conectividade de rede e dispara eventos quando o status muda.
/// Equivalente ao NetworkMonitor.kt do Android.
/// </summary>
public sealed class NetworkMonitor : IDisposable
{
    private bool _isOnline;
    private bool _disposed;

    public bool IsOnline => _isOnline;
    public event Action<bool>? OnlineStatusChanged;

    public NetworkMonitor()
    {
        _isOnline = CheckCurrentState();
        NetworkChange.NetworkAvailabilityChanged += OnNetworkAvailabilityChanged;
        NetworkChange.NetworkAddressChanged += OnNetworkAddressChanged;
    }

    private void OnNetworkAvailabilityChanged(object? sender, NetworkAvailabilityEventArgs e)
    {
        UpdateState(e.IsAvailable);
    }

    private void OnNetworkAddressChanged(object? sender, EventArgs e)
    {
        UpdateState(CheckCurrentState());
    }

    private void UpdateState(bool newState)
    {
        if (newState == _isOnline) return;
        _isOnline = newState;
        OnlineStatusChanged?.Invoke(_isOnline);
    }

    private static bool CheckCurrentState()
    {
        return NetworkInterface.GetIsNetworkAvailable();
    }

    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;
        NetworkChange.NetworkAvailabilityChanged -= OnNetworkAvailabilityChanged;
        NetworkChange.NetworkAddressChanged -= OnNetworkAddressChanged;
    }
}
