using System.Threading;
using System.Windows;

namespace Concord___Definitive_Edition
{
    public partial class App : System.Windows.Application
    {
        private const string MutexName = "ConcordDefinitiveEdition_SingleInstance";
        private const string EventName = "ConcordDefinitiveEdition_BringToFront";

        private Mutex? _mutex;
        private EventWaitHandle? _event;

        protected override void OnStartup(StartupEventArgs e)
        {
            // Tenta criar o Mutex. Se já existir, outra instância está rodando.
            _mutex = new Mutex(initiallyOwned: true, MutexName, out bool isNewInstance);

            if (!isNewInstance)
            {
                // Sinaliza a instância já aberta para se mostrar
                using var ev = new EventWaitHandle(false, EventResetMode.AutoReset, EventName);
                ev.Set();
                Shutdown();
                return;
            }

            // Cria o evento que escuta sinais das próximas tentativas de abertura
            _event = new EventWaitHandle(false, EventResetMode.AutoReset, EventName);

            // Thread em segundo plano esperando o sinal
            var thread = new Thread(() =>
            {
                while (_event.WaitOne())
                {
                    Dispatcher.Invoke(() =>
                    {
                        var window = MainWindow;
                        if (window == null) return;

                        window.Show();
                        window.ShowInTaskbar = true;
                        window.WindowState = WindowState.Normal;
                        window.Activate();
                    });
                }
            })
            {
                IsBackground = true
            };
            thread.Start();

            base.OnStartup(e);
        }

        protected override void OnExit(ExitEventArgs e)
        {
            _event?.Set();   // acorda a thread para que ela possa terminar
            _event?.Dispose();
            _mutex?.ReleaseMutex();
            _mutex?.Dispose();
            base.OnExit(e);
        }
    }
}