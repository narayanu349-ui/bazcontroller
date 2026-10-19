using System.Windows;
using BazController.Windows.ViewModels;

namespace BazController.Windows
{
    public partial class MainWindow : Window
    {
        private readonly MainViewModel _viewModel;

        public MainWindow()
        {
            InitializeComponent();
            _viewModel = new MainViewModel();
            DataContext = _viewModel;

            Loaded += (s, e) => _viewModel.Start();
            Closing += (s, e) => _viewModel.Stop();
        }
    }
}
