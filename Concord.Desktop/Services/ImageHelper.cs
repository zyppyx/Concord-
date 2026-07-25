using System.IO;
using System.Windows.Media.Imaging;

namespace Concord___Definitive_Edition.Services;

public static class ImageHelper
{
    public static BitmapImage? ImageFromBase64(string? profileImageBase64)
    {
        if (string.IsNullOrWhiteSpace(profileImageBase64))
        {
            return null;
        }

        var commaIndex = profileImageBase64.IndexOf(',');
        var base64 = commaIndex >= 0
            ? profileImageBase64[(commaIndex + 1)..]
            : profileImageBase64;

        byte[] bytes;

        try
        {
            bytes = Convert.FromBase64String(base64);
        }
        catch
        {
            return null;
        }

        using var stream = new MemoryStream(bytes);

        var image = new BitmapImage();
        image.BeginInit();
        image.CacheOption = BitmapCacheOption.OnLoad;
        image.StreamSource = stream;
        image.EndInit();
        image.Freeze();

        return image;
    }
}
