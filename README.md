# Casa em Dia

Aplicativo Android para gerenciar o estoque da casa: alimentos (macarrão, arroz, feijão…), produtos de limpeza, higiene e outros itens essenciais. Construído com **Kotlin**, **Jetpack Compose** e **Material 3**.

## Funcionalidades

- **Início (dashboard):** total de itens, alertas de estoque baixo e produtos perto de vencer.
- **Estoque:** cadastro com nome, categoria (Alimentos, Produtos de limpeza, Cuidados pessoais, Outros), quantidade, unidade, estoque mínimo, validade e local de armazenamento; adicionar, editar, excluir, buscar e filtrar (categoria / estoque baixo).
- **Detalhes do item:** todas as informações, edição, exclusão com confirmação e atalho para a lista de compras.
- **Lista de compras:** sugestões automáticas para itens com estoque baixo, itens manuais, marcar como comprado (reabastece o estoque do item vinculado) e limpar comprados.
- **Validade:** indicadores visuais — válido, vence em breve (≤ 7 dias) e vencido.
- **Persistência local** com Room: os dados permanecem após reiniciar o app.
- Estados de carregamento, vazio, sucesso e erro; tema claro/escuro; textos em português (pt-BR).

## Arquitetura

MVVM + princípios de Clean Architecture, em `app/src/main/java/br/com/casaemdia`:

```
domain/   modelos, interfaces de repositório e regras de negócio (sem dependência de Android)
data/     Room (entidades, DAOs, banco) e implementações dos repositórios
di/       módulo Koin
ui/       tema, componentes reutilizáveis, navegação e telas (Composable + ViewModel/StateFlow)
```

- **Estado de UI:** `ViewModel` expõe `StateFlow` de `UiState`, coletado com `collectAsStateWithLifecycle`.
- **Repository pattern:** `InventoryRepository` e `ShoppingRepository` (domínio) → `*RepositoryImpl` (dados).
- **Navegação:** Navigation Compose (abas Início/Estoque/Compras + Detalhes e Adicionar/Editar).
- **Injeção de dependência:** Koin. **Assíncrono:** Kotlin Coroutines / Flow.
- A estrutura permite evoluir para leitura de código de barras e sincronização em nuvem adicionando novas fontes de dados atrás das interfaces de repositório.

## Como executar

Requisitos: Android Studio Ladybug+ (ou JDK 17 e Android SDK 35).

```bash
./gradlew assembleDebug      # gera o APK de debug
./gradlew installDebug       # instala em dispositivo/emulador
./gradlew testDebugUnitTest  # testes unitários (JUnit + MockK)
```

Abra a pasta no Android Studio e execute o módulo `app`. `minSdk` 26.

## Testes

Testes unitários (JUnit 4 + MockK + kotlinx-coroutines-test) cobrem as regras de domínio: status de validade, estoque baixo, sugestões de compra, filtros e reabastecimento ao marcar item como comprado.
