# DevStudioIA Android 1.1 alfa — relatório de validação

## Resultado

O APK de teste foi compilado pelo GitHub Actions na execução [#4](https://github.com/msena-ahi/appscript-android/actions/runs/43128077034), commit `96682220a4ef1cac49b66a3bff7b804bf30d06bd`. Testes automatizados, análise Android, compilação e verificação da assinatura terminaram com sucesso.

| Verificação | Resultado |
|---|---|
| Testes unitários | 25 aprovados; 0 falhas, erros ou ignorados |
| Testes do repositório/API | 18 aprovados |
| Testes de interface Robolectric | 7 aprovados |
| Android Lint | 0 erros; 4 avisos |
| Build debug | Sucesso |
| Assinatura APK v2 | Verificada |
| Login Google real em aparelho | Pendente configuração OAuth |
| Leitura/gravação em Apps Script real | Pendente OAuth e homologação em projeto de teste |

Os quatro avisos de Lint são target SDK, versão de uma dependência, ausência de regras de extração de dados e ícone padrão. Não houve erro de Lint. O app mantém `allowBackup=false`; ainda assim o aviso de extração de dados fica registrado para revisão antes de produção.

## Mudança no login

A tela não pede mais Client ID. O botão de conexão abre o seletor de contas Google nativo do Android e solicita as permissões necessárias. Client ID não é uma senha de usuário; a configuração OAuth do aplicativo ainda precisa ser cadastrada pelo administrador no Google Cloud. A autorização nativa identifica o app por pacote e certificado. Não é necessário publicar na Play Store.

## APK

- Arquivo: `DevStudioIA-1.1-alpha-debug.apk`
- Tamanho: 3.598.378 bytes
- SHA-256: `8fbb71e8f776834ed0e8acedaac52ff71d2ae047132a8f5375fb7cf8aef22174`
- Pacote: `br.com.devstudioia`
- Certificado SHA-1: `D7:F4:7A:55:CD:F6:BB:17:FB:6F:53:DF:B3:A1:2C:A8:FC:EB:88:1C`
- A assinatura debug é efêmera por runner do Actions; o SHA-1 pode mudar em uma nova execução.

## O que foi exercitado automaticamente

- Conta e autorização canceladas sem falsa indicação de conexão.
- Chamadas à API com token no cabeçalho, paginação e leitura de arquivos.
- Edição em múltiplos arquivos com rascunhos preservados.
- Backup do original e rascunho antes do salvamento; falha do backup bloqueia a gravação.
- Confirmação e comparação do conteúdo remoto antes do PUT; conflito interrompe a gravação.
- Sessão trocada/expirada, permissões insuficientes, erros HTTP e respostas inválidas.
- Exportação e recuperação sem gravar token no backup.

## Ainda depende de configuração e teste manual

1. Cadastrar cliente OAuth do tipo Android no Google Cloud para pacote `br.com.devstudioia` e SHA-1 do certificado.
2. Habilitar Google Drive API e Google Apps Script API, revisar consentimento/audiência e, se aplicável, permitir o app no Workspace.
3. Ativar a Apps Script API nas configurações da conta Google de teste.
4. Instalar no Android com Google Play Services, conceder permissões e testar login, listagem e leitura.
5. Fazer edição e gravação apenas em projeto Apps Script descartável, confirmando o resultado remoto.
6. Preparar assinatura de teste estável em segredo do GitHub antes de cadastrar OAuth para versões sucessivas.

Até cumprir esses passos, login real e operações sobre projetos reais não estão validados. A comparação prévia de versão não é transacional; evite edição simultânea do projeto. Firebase, copiloto, execução/publicação de scripts e criação/exclusão de projetos não fazem parte desta alfa.

## Custo e publicação

A execução usou runner Ubuntu padrão do GitHub Actions e não ativou serviços pagos. O APK é de teste/debug. Não houve publicação na Play Store.
