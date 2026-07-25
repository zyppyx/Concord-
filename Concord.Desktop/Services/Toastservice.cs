using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Animation;
using System.Windows.Threading;

namespace Concord___Definitive_Edition.Services;

public enum ToastKind { Info, Success, Warning, Error }

public static class ToastService
{
    private const double ToastWidth = 320;
    private const double ToastHeight = 64;
    private const double Margin = 16;
    private const double DisplayMs = 3500;
    private const double AnimationMs = 220;

    private static readonly System.Windows.Media.Brush White =
        new SolidColorBrush(System.Windows.Media.Colors.White);

    private static readonly System.Windows.Media.Brush Transparent =
        new SolidColorBrush(System.Windows.Media.Colors.Transparent);

    private static readonly Dictionary<ToastKind, (string bg, string icon)> Palette = new()
    {
        [ToastKind.Info] = ("#2B2F42", "💬"),
        [ToastKind.Success] = ("#2D6A4F", "✔"),
        [ToastKind.Warning] = ("#ffb833", "⚠"),
        [ToastKind.Error] = ("#cc0000", "✖"),
    };

    public static void Show(string message, ToastKind kind = ToastKind.Info)
    {
        System.Windows.Application.Current.Dispatcher.Invoke(() => ShowOnUiThread(message, kind));
    }

    private static void ShowOnUiThread(string message, ToastKind kind)
    {
        var (bg, icon) = Palette[kind];

        var window = new System.Windows.Window
        {
            Width = ToastWidth,
            Height = ToastHeight,
            WindowStyle = System.Windows.WindowStyle.None,
            AllowsTransparency = true,
            Background = Transparent,
            ShowInTaskbar = false,
            Topmost = true,
            IsHitTestVisible = false,
            ResizeMode = System.Windows.ResizeMode.NoResize,
        };

        PositionWindow(window);

        var border = new Border
        {
            Background = new SolidColorBrush((System.Windows.Media.Color)
                               System.Windows.Media.ColorConverter.ConvertFromString(bg)),
            CornerRadius = new System.Windows.CornerRadius(10),
            Padding = new System.Windows.Thickness(14, 0, 14, 0),
            Effect = new System.Windows.Media.Effects.DropShadowEffect
            {
                BlurRadius = 14,
                Opacity = 0.45,
                ShadowDepth = 2,
                Color = System.Windows.Media.Colors.Black
            }
        };

        var row = new Grid();
        row.ColumnDefinitions.Add(new ColumnDefinition { Width = System.Windows.GridLength.Auto });
        row.ColumnDefinitions.Add(new ColumnDefinition { Width = new System.Windows.GridLength(1, System.Windows.GridUnitType.Star) });

        var iconBlock = new TextBlock
        {
            Text = icon,
            FontSize = 18,
            VerticalAlignment = System.Windows.VerticalAlignment.Center,
            Margin = new System.Windows.Thickness(0, 0, 10, 0),
            Foreground = White
        };

        var textBlock = new TextBlock
        {
            Text = message,
            Foreground = White,
            FontSize = 13,
            TextWrapping = System.Windows.TextWrapping.Wrap,
            VerticalAlignment = System.Windows.VerticalAlignment.Center,
            MaxHeight = ToastHeight - 16
        };

        Grid.SetColumn(iconBlock, 0);
        Grid.SetColumn(textBlock, 1);
        row.Children.Add(iconBlock);
        row.Children.Add(textBlock);

        border.Child = row;
        window.Content = border;

        window.Opacity = 0;
        window.Show();

        var fadeIn = new DoubleAnimation(0, 1, TimeSpan.FromMilliseconds(AnimationMs));
        window.BeginAnimation(System.Windows.UIElement.OpacityProperty, fadeIn);

        var timer = new DispatcherTimer { Interval = TimeSpan.FromMilliseconds(DisplayMs) };
        timer.Tick += (_, _) =>
        {
            timer.Stop();
            var fadeOut = new DoubleAnimation(1, 0, TimeSpan.FromMilliseconds(AnimationMs));
            fadeOut.Completed += (_, _) => window.Close();
            window.BeginAnimation(System.Windows.UIElement.OpacityProperty, fadeOut);
        };
        timer.Start();
    }

    private static void PositionWindow(System.Windows.Window window)
    {
        var area = System.Windows.SystemParameters.WorkArea;
        window.Left = area.Right - ToastWidth - Margin;
        window.Top = area.Bottom - ToastHeight - Margin;
    }
}