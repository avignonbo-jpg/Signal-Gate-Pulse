// Add to repositoryModule and viewModelModule
single { BlocklistRepository(get()) }
viewModel { ContactsViewModel(get(), get()) } // Context, BlocklistRepository