# DevStudioIA Android 1.1.1 alfa — relatório de validação

## Resultado

O APK foi compilado na execução [#6 do GitHub Actions](https://github.com/msena-ahi/appscript-android/actions/runs/43149776484), commit `77314f0beff39d87d612d15493545de8c63a2e48`.

| Verificação | Resultado |
|---|---|
| Testes automatizados | 26 aprovados; 0 falhas, erros ou ignorados |
| Repositório/API | 18 aprovados |
| Interface Robolectric | 8 aprovados |
| Android Lint | 0 erros; 4 avisos |
| Compilação debug | Sucesso |
| Assinatura APK v2 | Verificada |
| OAuth Google real em aparelho | Pendente cadastro OAuth |
| Projetos reais Apps Script | Pendente homologação manual |

## Fluxo de conta

A tela agora orienta a conectar uma conta Google como primeiro passo. A lista de projetos fica desabilitada até concluir a autorização; depois, mostra os projetos Apps Script acessíveis ao e-mail conectado. O token é obtido pelo fluxo nativo Android e não há campo de Client ID, senha ou Client Secret para o usuário.

## APK

- Arquivo: `DevStudioIA-1.1.1-alpha-debug.apk`
- Tamanho: 3.598.478 bytes
- SHA-256: `bde41422a572361a9faadea0b017d698c834e9a18acc757a4b20c264d555544a`
- Pacote: `br.com.devstudioia`
- Certificado SHA-1: `5D:8B:BC:28:9D:87:46:E9:44:0F:4C:6E:81:29:33:BB:10:7E:AA:AF`
- A chave debug é gerada por runner e pode mudar a cada compilação.

## O que foi verificado automaticamente

Listagem depende do token da conta selecionada; testes também cobrem paginação, leitura de arquivos, edição com rascunhos preservados, backup antes de gravar, cancelamento, conflitos, erros de rede/API, falhas de sessão e exportação sem token. A compilação não verifica a autorização Google real nem operações em projeto real.

## Próximos passos para homologar o login real

1. Registrar no Google Cloud um cliente OAuth Android com pacote `br.com.devstudioia` e o SHA-1 desta assinatura, além de habilitar Drive API e Apps Script API.
2. Ativar Apps Script API na conta de teste e liberar o app no Workspace se necessário.
3. Instalar no Android com Google Play Services e testar login, listagem e leitura.
4. Testar edição e gravação somente em projeto Apps Script descartável.
5. Antes de próximas compilações, usar assinatura de teste estável guardada como segredo do GitHub.

Sem publicação na Play Store e sem ativação de serviços pagos.
