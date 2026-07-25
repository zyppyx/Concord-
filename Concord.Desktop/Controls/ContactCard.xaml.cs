using System.Windows;
using System.Windows.Controls;
using System.Windows.Media;
using System.Windows.Media.Imaging;
using Brushes = System.Windows.Media.Brushes;
using Color = System.Windows.Media.Color;
using ColorConverter = System.Windows.Media.ColorConverter;
using SolidColorBrush = System.Windows.Media.SolidColorBrush;

namespace Concord___Definitive_Edition.Controls;

public partial class ContactCard : System.Windows.Controls.UserControl
{
    public ContactCard()
    {
        InitializeComponent();
    }

    public static readonly DependencyProperty FriendNameProperty =
        DependencyProperty.Register(nameof(FriendName), typeof(string), typeof(ContactCard), new PropertyMetadata("", OnVisualPropertyChanged));

    public string FriendName
    {
        get => (string)GetValue(FriendNameProperty);
        set => SetValue(FriendNameProperty, value);
    }

    public static readonly DependencyProperty IsOnlineProperty =
        DependencyProperty.Register(nameof(IsOnline), typeof(bool), typeof(ContactCard), new PropertyMetadata(false, OnVisualPropertyChanged));

    public bool IsOnline
    {
        get => (bool)GetValue(IsOnlineProperty);
        set => SetValue(IsOnlineProperty, value);
    }

    public static readonly DependencyProperty IsSelectedProperty =
        DependencyProperty.Register(nameof(IsSelected), typeof(bool), typeof(ContactCard), new PropertyMetadata(false, OnVisualPropertyChanged));

    public bool IsSelected
    {
        get => (bool)GetValue(IsSelectedProperty);
        set => SetValue(IsSelectedProperty, value);
    }

    public static readonly DependencyProperty ProfileImageProperty =
        DependencyProperty.Register(nameof(ProfileImage), typeof(BitmapImage), typeof(ContactCard), new PropertyMetadata(null, OnVisualPropertyChanged));

    public BitmapImage? ProfileImage
    {
        get => (BitmapImage?)GetValue(ProfileImageProperty);
        set => SetValue(ProfileImageProperty, value);
    }

    public static readonly DependencyProperty InitialProperty =
        DependencyProperty.Register(nameof(Initial), typeof(string), typeof(ContactCard), new PropertyMetadata("?"));

    public string Initial
    {
        get => (string)GetValue(InitialProperty);
        private set => SetValue(InitialProperty, value);
    }

    public static readonly DependencyProperty StatusTextProperty =
        DependencyProperty.Register(nameof(StatusText), typeof(string), typeof(ContactCard), new PropertyMetadata("Offline"));

    public string StatusText
    {
        get => (string)GetValue(StatusTextProperty);
        private set => SetValue(StatusTextProperty, value);
    }

    public static readonly DependencyProperty StatusBrushProperty =
        DependencyProperty.Register(nameof(StatusBrush), typeof(System.Windows.Media.Brush), typeof(ContactCard), new PropertyMetadata(Brushes.Gray));

    public System.Windows.Media.Brush StatusBrush
    {
        get => (System.Windows.Media.Brush)GetValue(StatusBrushProperty);
        private set => SetValue(StatusBrushProperty, value);
    }

    public static readonly DependencyProperty CardBackgroundProperty =
        DependencyProperty.Register(nameof(CardBackground), typeof(System.Windows.Media.Brush), typeof(ContactCard), new PropertyMetadata(new SolidColorBrush((Color)ColorConverter.ConvertFromString("#252836"))));

    public System.Windows.Media.Brush CardBackground
    {
        get => (System.Windows.Media.Brush)GetValue(CardBackgroundProperty);
        private set => SetValue(CardBackgroundProperty, value);
    }

    public static readonly DependencyProperty ProfileImageOpacityProperty =
        DependencyProperty.Register(nameof(ProfileImageOpacity), typeof(double), typeof(ContactCard), new PropertyMetadata(0d));

    public double ProfileImageOpacity
    {
        get => (double)GetValue(ProfileImageOpacityProperty);
        private set => SetValue(ProfileImageOpacityProperty, value);
    }

    public static readonly DependencyProperty InitialOpacityProperty =
        DependencyProperty.Register(nameof(InitialOpacity), typeof(double), typeof(ContactCard), new PropertyMetadata(1d));

    public double InitialOpacity
    {
        get => (double)GetValue(InitialOpacityProperty);
        private set => SetValue(InitialOpacityProperty, value);
    }

    private static void OnVisualPropertyChanged(DependencyObject d, DependencyPropertyChangedEventArgs e)
    {
        var card = (ContactCard)d;
        card.InvalidatePropertyVisuals();
    }

    private void InvalidatePropertyVisuals()
    {
        Initial = string.IsNullOrWhiteSpace(FriendName) ? "?" : FriendName[0].ToString().ToUpperInvariant();
        StatusText = IsOnline ? "Online" : "Offline";
        StatusBrush = IsOnline ? Brushes.LimeGreen : Brushes.Gray;
        CardBackground = new SolidColorBrush((Color)ColorConverter.ConvertFromString(IsSelected ? "#3A4060" : "#252836"));
        ProfileImageOpacity = ProfileImage == null ? 0 : 1;
        InitialOpacity = ProfileImage == null ? 1 : 0;
    }
}
