# DevStudioIA Android 1.1.1 alfa — validação concluída

- Repositório: `msena-ahi/appscript-android`, branch `main`.
- Commit compilado: `77314f0beff39d87d612d15493545de8c63a2e48`.
- GitHub Actions: execução #6, sucesso: https://github.com/msena-ahi/appscript-android/actions/runs/43149776484
- Testes unitários: 26 aprovados; 0 falhas, erros ou ignorados (18 de repositório/API e 8 de interface).
- Android Lint: 0 erros; 4 avisos. Build debug e assinatura APK v2 validados.
- APK: 3.598.478 bytes; SHA-256 `bde41422a572361a9faadea0b017d698c834e9a18acc757a4b20c264d555544a`.
- Certificado SHA-1: `5D:8B:BC:28:9D:87:46:E9:44:0F:4C:6E:81:29:33:BB:10:7E:AA:AF`.
- O primeiro passo é entrar com uma conta Google; só depois de autorizar fica habilitada a listagem dos projetos acessíveis por esse e-mail.
- **Pendente:** cadastro OAuth Android no Google Cloud, APIs Drive/Apps Script, autorização em aparelho e teste de leitura/gravação em projeto descartável. A integração Google real ainda não está homologada.
- A assinatura debug é efêmera por runner; configure uma chave de teste estável em segredo GitHub antes de homologar OAuth para versões sucessivas.
- Sem Play Store, serviços pagos ou publicação.

## Cobertura automatizada

As 18 verificações de repositório cobrem autenticação HTTP sem token na URL, paginação, leitura, conteúdo de atualização, edição de vários arquivos, backup, conflito, troca de sessão, manifesto, respostas inválidas e erros HTTP. As 8 de interface cobrem fluxo de login/listagem, seletor/cancelamento, diagnóstico, rascunhos, confirmação e backup sem token.

A compilação não valida consentimento OAuth real, instalação em aparelho, nem leitura ou gravação real no Apps Script. A comparação prévia ao PUT não é bloqueio atômico; evite edição simultânea.
