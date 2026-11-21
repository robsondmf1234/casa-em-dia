# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projeto

Casa em Dia é um app Android para controlar o estoque da casa (alimentos, limpeza, cuidados pessoais), datas de validade e uma lista de compras. Também há login com Google (Credential Manager). Tem um único módulo (`:app`) e usa Kotlin, Jetpack Compose + Material 3, Room, Koin e Coroutines/Flow. Requisitos: JDK 17, compileSdk/targetSdk 35, minSdk 26. As versões das dependências ficam em `gradle/libs.versions.toml`.

O idioma da interface é português do Brasil. Tanto `res/values/strings.xml` quanto `res/values-pt-rBR/strings.xml` têm os textos em português, então toda string nova precisa ser adicionada **nos dois** arquivos. Nunca coloque texto visível ao usuário direto nos composables.

## Antes de implementar

Se houver qualquer dúvida sobre o que implementar (escopo, comportamento, textos, opções de menu, onde algo deve aparecer etc.), **pergunte ao usuário antes de começar**, em vez de presumir. Só siga com uma escolha própria quando não houver ambiguidade real.

## Comandos

```bash
./gradlew assembleDebug                 # gera o APK de debug
./gradlew installDebug                  # instala em dispositivo/emulador
./gradlew testDebugUnitTest             # todos os testes unitários
./gradlew testDebugUnitTest --tests "br.com.casaemdia.ui.viewmodel.HomeViewModelTest"            # uma classe
./gradlew testDebugUnitTest --tests "br.com.casaemdia.ui.viewmodel.HomeViewModelTest.updatesWhenInventoryChanges"  # um teste
```

Só existem testes unitários na JVM (JUnit 4, MockK, kotlinx-coroutines-test). Não há testes instrumentados nem ferramenta de lint/formatação configurada.

## Arquitetura

MVVM com camadas de Clean Architecture em `app/src/main/java/br/com/casaemdia`:

- **`domain/`** é Kotlin puro, sem imports de Android. Contém:
  - `model/`: os modelos de domínio.
  - `repository/`: as interfaces `InventoryRepository` e `ShoppingRepository` (`Repositories.kt`), `AuthRepository` + `GoogleCredentialProvider` (`AuthRepository.kt`) `TransactionRunner`, que executa várias operações de repositório de forma atômica, e `SettingsRepository` (preferências do app, como o `ThemeMode`).
  - `usecase/`: as regras de negócio. A maioria são funções de extensão de nível superior, como `expirationStatus`, `nearingExpiration`, `lowStock`, `suggestShoppingItems`, `filterInventory` e `parseQuantity`. As operações que usam repositórios são pequenas classes com `operator fun invoke`: `AddSuggestionToShoppingList` e `SetItemPurchased`. Esta última recebe o id do item, relê o estado atual dentro de um `TransactionRunner` e reabastece o item de estoque vinculado quando você marca a compra (desfaz quando desmarca). Chamá-la de novo com o mesmo valor não faz nada, o que evita reabastecer em dobro com toque duplo ou estado desatualizado da tela. Coloque lógica de negócio nova aqui, não nos ViewModels.
- **`data/`** contém:
  - `local/`: as entidades, os DAOs e o `AppDatabase` do Room (em `Daos.kt`). As entidades guardam enums como o nome em `String` e datas como epoch day. Ao ler um nome desconhecido, os mapeadores `toDomain()`/`toEntity()` em `Entities.kt` usam um valor padrão do enum.
  - `repository/`: as implementações das interfaces de domínio. `RepositoryImpls.kt` (estoque e compras), `RoomTransactionRunner` (usa `withTransaction` do Room), `AuthRepositoryImpl` (guarda a sessão só em memória) `CredentialManagerGoogleProvider`, que é a única classe que conhece `Context`/Credential Manager, e `SettingsRepositoryImpl`, que guarda as preferências em `SharedPreferences`.
- **`di/AppModule.kt`** é o único módulo Koin. Ele liga o banco, os DAOs, os repositórios, os casos de uso, o `java.time.Clock` e todos os ViewModels. Registre aqui qualquer ViewModel ou caso de uso novo. Os ViewModels que recebem um id de item o recebem via `parametersOf` do Koin. O `EditItemViewModel` converte id negativo em `null`, que significa "item novo". O `GoogleCredentialProvider` é uma `factory` que recebe o `Context` da Activity via `parametersOf` (a `LoginScreen` o obtém com `koinInject`).
- **`ui/`** tem um pacote por tela (`home`, `inventory`, `details`, `edit`, `shopping`, `login`) com o composable `*Screen.kt`. `ui/settings/SettingsMenu.kt` é o menu da engrenagem da Home (Tema e Sobre o app). Todos os ViewModels (e seus `*UiState`) ficam juntos em `ui/viewmodel/`. Os componentes compartilhados ficam em `ui/components/` e o tema em `ui/theme/`.
  - `ui/navigation/CasaEmDiaNavHost.kt` define as `Routes` e envolve os três destinos da barra inferior em `TabScaffold`. Detalhes e Edição são rotas de tela cheia. `Routes.NEW_ITEM_ID = -1` abre a Edição no modo "criar". O Estoque aceita `inventory?filterLowStock=true`. A rota `LOGIN` existe, mas o `startDestination` é `HOME`, então a tela de login ainda não é exibida. O `TabScaffold` recebe as ações da barra superior pelo slot `actions`.
  - **Tema:** o `MainActivity` lê o `SettingsViewModel` e resolve `ThemeMode` (`LIGHT`, `DARK` ou `SYSTEM`) para o `darkTheme` do `CasaEmDiaTheme`, e também ajusta os ícones das barras do sistema com `enableEdgeToEdge`. Previews não usam Koin: passe a versão sem estado (`SettingsMenu`) em vez de `SettingsMenuButton`.

### Convenções a manter

- Cada ViewModel expõe um único `StateFlow<XUiState>`, criado com `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`. As telas o coletam com `collectAsStateWithLifecycle`. O UiState tem as flags `isLoading`/`hasError`. Para tentar de novo, o ViewModel incrementa um gatilho `MutableStateFlow`, que passa por `flatMapLatest` até o flow do repositório.
- A data de "hoje" sempre vem do `Clock` injetado (`LocalDate.now(clock)`), nunca de `LocalDate.now()`, para que os testes possam fixar a data.
- **Schema do Room:** o `AppDatabase` está na versão 2 (a `MIGRATION_1_2`, em `Daos.kt`, só cria índices), e o KSP exporta o schema para `app/schemas/`. Alterar uma entidade exige subir a versão, adicionar uma migration, registrá-la em `AppModule.kt` com `addMigrations` e commitar o JSON de schema gerado.
- Operações que gravam em mais de um repositório devem rodar dentro do `TransactionRunner` e reler o estado atual no repositório em vez de confiar no objeto vindo da tela.

## Testes

`app/src/test/java/br/com/casaemdia/testing/Fakes.kt` reúne os utilitários de teste compartilhados:

- `FakeInventoryRepository`/`FakeShoppingRepository`, que guardam os dados em memória
- o builder `inventoryItem(...)`
- `TODAY` (2025-01-10) e `FIXED_CLOCK`
- `ImmediateTransactionRunner`, que executa o bloco direto, `FakeAuthRepository` e `FakeSettingsRepository`
- `MainDispatcherRule`, que substitui o `Dispatchers.Main`

Os testes de ViewModel seguem um padrão comum. Aplique `@get:Rule val mainDispatcherRule = MainDispatcherRule()` e crie o VM com os fakes e o `FIXED_CLOCK`. Depois comece a coletar `uiState` em `backgroundScope.launch(UnconfinedTestDispatcher(testScheduler))`, porque sem isso o `WhileSubscribed` não emite nada.
