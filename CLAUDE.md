# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Projeto

Casa em Dia é um app Android para controlar o estoque da casa (alimentos, limpeza, cuidados pessoais), datas de validade e uma lista de compras. Tem um único módulo (`:app`) e usa Kotlin, Jetpack Compose + Material 3, Room, Koin e Coroutines/Flow. Requisitos: JDK 17, compileSdk/targetSdk 35, minSdk 26. As versões das dependências ficam em `gradle/libs.versions.toml`.

O idioma da interface é português do Brasil. Tanto `res/values/strings.xml` quanto `res/values-pt-rBR/strings.xml` têm os textos em português, então toda string nova precisa ser adicionada **nos dois** arquivos. Nunca coloque texto visível ao usuário direto nos composables.

## Comandos

```bash
./gradlew assembleDebug                 # gera o APK de debug
./gradlew installDebug                  # instala em dispositivo/emulador
./gradlew testDebugUnitTest             # todos os testes unitários
./gradlew testDebugUnitTest --tests "br.com.casaemdia.ui.home.HomeViewModelTest"            # uma classe
./gradlew testDebugUnitTest --tests "br.com.casaemdia.ui.home.HomeViewModelTest.updatesWhenInventoryChanges"  # um teste
```

Só existem testes unitários na JVM (JUnit 4, MockK, kotlinx-coroutines-test). Não há testes instrumentados nem ferramenta de lint/formatação configurada.

## Arquitetura

MVVM com camadas de Clean Architecture em `app/src/main/java/br/com/casaemdia`:

- **`domain/`** é Kotlin puro, sem imports de Android. Contém:
  - `model/`: os modelos de domínio.
  - `repository/Repositories.kt`: as interfaces `InventoryRepository` e `ShoppingRepository`.
  - `usecase/`: as regras de negócio. A maioria são funções de extensão de nível superior, como `expirationStatus`, `nearingExpiration`, `lowStock`, `suggestShoppingItems`, `filterInventory` e `parseQuantity`. As operações que usam repositórios são pequenas classes com `operator fun invoke`: `AddSuggestionToShoppingList` e `SetItemPurchased`. Esta última reabastece o item de estoque vinculado quando você marca a compra e desfaz isso quando desmarca. Coloque lógica de negócio nova aqui, não nos ViewModels.
- **`data/`** contém:
  - `local/`: as entidades, os DAOs e o `AppDatabase` do Room (em `Daos.kt`). As entidades guardam enums como o nome em `String` e datas como epoch day. Ao ler um nome desconhecido, os mapeadores `toDomain()`/`toEntity()` em `Entities.kt` usam um valor padrão do enum.
  - `repository/RepositoryImpls.kt`: as implementações das interfaces de domínio.
- **`di/AppModule.kt`** é o único módulo Koin. Ele liga o banco, os DAOs, os repositórios, os casos de uso, o `java.time.Clock` e todos os ViewModels. Registre aqui qualquer ViewModel ou caso de uso novo. Os ViewModels que recebem um id de item o recebem via `parametersOf` do Koin. O `EditItemViewModel` converte id negativo em `null`, que significa "item novo".
- **`ui/`** tem um pacote por tela (`home`, `inventory`, `details`, `edit`, `shopping`), cada um com um composable `*Screen.kt` e um `*ViewModel.kt`. Os componentes compartilhados ficam em `ui/components/`.
  - `ui/navigation/CasaEmDiaNavHost.kt` define as `Routes` e envolve os três destinos da barra inferior em `TabScaffold`. Detalhes e Edição são rotas de tela cheia. `Routes.NEW_ITEM_ID = -1` abre a Edição no modo "criar".

### Convenções a manter

- Cada ViewModel expõe um único `StateFlow<XUiState>`, criado com `stateIn(viewModelScope, WhileSubscribed(5_000), initial)`. As telas o coletam com `collectAsStateWithLifecycle`. O UiState tem as flags `isLoading`/`hasError`. Para tentar de novo, o ViewModel incrementa um gatilho `MutableStateFlow`, que passa por `flatMapLatest` até o flow do repositório.
- A data de "hoje" sempre vem do `Clock` injetado (`LocalDate.now(clock)`), nunca de `LocalDate.now()`, para que os testes possam fixar a data.
- **Schema do Room:** o `AppDatabase` está na versão 1, e o KSP exporta o schema para `app/schemas/`. Alterar uma entidade exige subir a versão, adicionar uma migration e commitar o JSON de schema gerado.

## Testes

`app/src/test/java/br/com/casaemdia/testing/Fakes.kt` reúne os utilitários de teste compartilhados:

- `FakeInventoryRepository`/`FakeShoppingRepository`, que guardam os dados em memória
- o builder `inventoryItem(...)`
- `TODAY` (2025-01-10) e `FIXED_CLOCK`
- `MainDispatcherRule`, que substitui o `Dispatchers.Main`

Os testes de ViewModel seguem um padrão comum. Aplique `@get:Rule val mainDispatcherRule = MainDispatcherRule()` e crie o VM com os fakes e o `FIXED_CLOCK`. Depois comece a coletar `uiState` em `backgroundScope.launch(UnconfinedTestDispatcher(testScheduler))`, porque sem isso o `WhileSubscribed` não emite nada.
