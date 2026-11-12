# DevStudioIA Android 1.1 alfa — validação concluída

- Repositório: `msena-ahi/appscript-android`, branch `main`.
- Commit compilado: `96682220a4ef1cac49b66a3bff7b804bf30d06bd`.
- GitHub Actions: execução #4, sucesso: https://github.com/msena-ahi/appscript-android/actions/runs/43128077034
- Etapas: testes unitários, Android Lint, compilação debug e verificação criptográfica do APK concluídas.
- Testes: 25 aprovados; 0 falhas, erros ou ignorados (18 de repositório/API e 7 de interface).
- Lint: 0 erros e 4 avisos (target SDK, versão de dependência, regras de extração de dados e ícone padrão).
- APK: 3.598.378 bytes; SHA-256 `8fbb71e8f776834ed0e8acedaac52ff71d2ae047132a8f5375fb7cf8aef22174`.
- Certificado: SHA-1 `D7:F4:7A:55:CD:F6:BB:17:FB:6F:53:DF:B3:A1:2C:A8:FC:EB:88:1C`; assinatura APK v2 validada.
- O login agora usa o seletor de contas Google do Android e não pede Client ID na tela.
- **Pendente:** cadastro OAuth Android no Google Cloud, APIs Drive/Apps Script, autorização no aparelho e teste de leitura/gravação com projeto descartável. Logo, a integração real Google ainda não está homologada.
- Assinatura debug é efêmera por runner; antes de homologar OAuth, configurar chave estável de teste como segredo do GitHub e cadastrar seu SHA-1.
- Sem publicação na Play Store, sem serviços pagos e sem ativação de recursos pagos.

## Cobertura automatizada

As 18 verificações do repositório cobrem autenticação HTTP sem token na URL, paginação, leitura, preservação de campos de atualização, edição em múltiplos arquivos, backup antes de gravação, conflito, falhas de backup, troca de sessão/conta, validação de manifesto, respostas inválidas e erros HTTP. As 7 verificações da interface cobrem tela sem Client ID, seletor/cancelamento, diagnóstico, rascunhos, confirmação de gravação e backup/SAF sem token.

A compilação não valida consentimento OAuth real, conta Workspace, instalação em aparelho, nem leitura ou gravação em Apps Script real. A comparação de conteúdo antes do PUT não é um bloqueio atômico; evite editar o mesmo projeto simultaneamente.
