# DevStudioIA Android 1.1 alfa

A tela de login usa **Entrar com Google** e abre o seletor de contas do Android. O usuário não informa Client ID, Client Secret ou senha no DevStudioIA. A senha e a autorização são tratadas pelo sistema/Google.

## Configuração do aplicativo pelo administrador

Este cadastro é obrigatório para acesso real às APIs; remover o campo técnico da tela não elimina a configuração OAuth do aplicativo.

1. Defina um projeto Google Cloud de homologação e configure consentimento/audiência, incluindo usuários de teste se estiver em modo de teste. Não é necessário publicar na Play Store.
2. Cadastre um cliente OAuth **Android** para o pacote `br.com.devstudioia` e o SHA-1 do certificado deste APK. O SHA-1 está em **Diagnóstico e configuração** e no artefato `APK_CERTIFICATE.txt` da compilação.
3. Habilite Google Drive API e Google Apps Script API no projeto.
4. Na conta que será usada, ative a Apps Script API em `https://script.google.com/home/usersettings`.
5. Em contas Workspace, o administrador pode precisar liberar o aplicativo e seus escopos.
6. Escolha a conta no Android e conceda `drive.metadata.readonly` e `script.projects`.

Não há redirecionamento customizado AppAuth nem cliente web/backend neste fluxo. O SDK Google usa a identidade do aplicativo Android. Tokens permanecem em memória e não são incluídos em backups.

**Assinatura de homologação:** enquanto não houver chave estável configurada, cada runner novo gera uma chave debug diferente. Uma versão posterior pode exigir novo cadastro de SHA-1 e desinstalação da anterior. Antes de homologar OAuth, configure uma chave de teste estável como segredo do repositório; nunca publique a chave no código, cache público ou logs. A compilação atual ainda usa a assinatura debug efêmera.

## Fluxos implementados

- Seleção de conta Google pessoal ou corporativa; consentimento e cancelamento tratados.
- Entrada sem Client ID visível, saída com limpeza local e tentativa de revogação de acesso.
- Listagem paginada e atualização de projetos Apps Script acessíveis pela Drive API.
- Leitura de arquivos do projeto; edição de código e manifesto.
- Rascunho mantido ao mudar de arquivo e proteção contra descarte acidental.
- Recuperação local de rascunho após interrupção do app; exportação por seletor de arquivos Android.
- Backup com original **e** rascunho antes de salvar; falha de backup impede gravação remota.
- Comparação com conteúdo remoto antes do PUT; conflito cancela gravação.
- Após salvar, a referência local é atualizada para permitir novas alterações.
- Tratamento de sessão expirada, falta de permissão, projeto ausente, limite de API e resposta inválida.
- Erro em gravação não provoca repetição automática do PUT.
- Área de diagnóstico com pacote e certificado para configuração pelo administrador.

A comparação antes de gravar **não é um bloqueio atômico**: a Apps Script API substitui o conjunto de arquivos e não oferece aqui uma transação de edição colaborativa. Evite edição simultânea. Firebase Manager, copiloto, execução/publicação de scripts, criação e exclusão de projetos continuam fora do escopo desta versão.

## Testes e compilação

O workflow `.github/workflows/android-apk.yml` executa automaticamente em mudanças Android e pode ser iniciado manualmente em Actions. Usa runner Ubuntu padrão em repositório público, sem serviços pagos ou publicação na Play Store.

Com Gradle 8.9, JDK 17 e SDK 35:

```sh
gradle --no-daemon :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

- Testes de repositório: HTTP com MockWebServer; dados de teste, sem acesso a projetos reais.
- Testes de interface: Robolectric Android 9; tela inicial, seletor cancelado, diagnóstico, editor, confirmação e backup.
- Análise Android Lint e verificação de assinatura/integridade do APK.
- Artefatos: APK, certificado, SHA-256 e relatórios de testes/lint, retidos por um dia.

Testes automatizados não substituem a homologação do seletor/consentimento Google em aparelho com Play Services. O login, leitura e salvamento em projetos reais só podem ser declarados homologados depois de configurar OAuth e executar o roteiro abaixo.

## Homologação em aparelho

Use uma conta de teste e um projeto Apps Script descartável. Nunca valide gravação inicialmente em projetos de produção.

1. Abrir app, verificar que não há campo Client ID e cancelar a seleção da conta.
2. Conectar uma conta autorizada; conferir o e-mail exibido e as permissões solicitadas.
3. Listar, paginar, atualizar e abrir projeto. Testar conta sem projetos e sem permissão.
4. Alterar dois arquivos, alternar entre eles e verificar preservação de rascunho.
5. Exportar backup e conferir original/rascunho. Não há tokens no arquivo.
6. Cancelar o diálogo de gravação; confirmar que nada mudou no Google.
7. Confirmar a gravação; conferir resultado remotamente e executar uma segunda gravação.
8. Alterar o mesmo projeto no navegador antes de salvar pelo app: a gravação deve ser bloqueada por conflito.
9. Testar sem internet e acesso revogado; manter e exportar rascunho em caso de erro.
10. Sair e trocar para outra conta: não devem aparecer dados da conta anterior.

Fontes: https://developer.android.com/identity/authorization e https://developers.google.com/apps-script/api/reference/rest/v1/projects/updateContent
