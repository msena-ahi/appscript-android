# DevStudioIA Android 1.2 alfa — validação

## Resultado da compilação

A execução [#8 do GitHub Actions](https://github.com/msena-ahi/appscript-android/actions/runs/43199743849) terminou com sucesso no commit `ca343f1abe76a413aa78208806ec15a4e0bfb0e7`.

| Verificação | Resultado |
|---|---|
| Testes automatizados | 25 aprovados; 0 falhas, erros ou ignorados |
| Testes de repositório/API | 18 aprovados |
| Testes de interface | 7 aprovados |
| Android Lint | 0 erros; 4 avisos |
| Compilação debug e assinatura APK v2 | Sucesso |
| Seletor de conta e URL oficial com e-mail selecionado | Verificados em teste automatizado |
| Login Google real no Chrome e carregamento de projetos | Ainda precisa de teste manual no aparelho |

## Fluxo de uso

O usuário escolhe sua conta Google no app. O DevStudioIA abre o site oficial `https://script.google.com/home` no navegador e acrescenta o e-mail escolhido como indicação de conta. O Google autentica a sessão do navegador e o Apps Script apresenta a lista de projetos e o editor oficial. Se o Chrome não tiver essa conta conectada, o Google solicitará o login; a conta também pode ser trocada no perfil do site.

O APK não recebe senha nem token, não pede Client ID na tela e não replica o editor web. A interface de projetos e as funcionalidades são fornecidas pelo Apps Script no navegador. Essa decisão preserva a interface oficial em vez de expor no celular apenas um subconjunto de funções por API.

## Artefato

- APK: `DevStudioIA-1.2-alpha-debug.apk`
- Tamanho: 3.598.490 bytes
- SHA-256: `6172e6e75dac06434199ba287d669e5cacf44d4bbd360ac4350aafe5f8b74ec8`
- Pacote: `br.com.devstudioia`
- Certificado SHA-1: `91:DC:35:DD:C0:5F:24:6F:08:A0:E1:B1:CF:1F:31:A8:4F:0E:D7:DF`
- Assinatura debug efêmera do runner GitHub Actions.

## Limites da validação

Os testes automatizados verificam a tela, o seletor e o endereço aberto com a indicação do e-mail. Não houve execução no aparelho do usuário, login no Chrome nem confirmação de que a sessão consegue exibir os projetos da conta. Para concluir a homologação prática, instale o APK, escolha a conta desejada e confirme no navegador se o Apps Script mostra seus projetos.

## Custo e publicação

Foi usado runner Ubuntu padrão do GitHub Actions. Nenhum serviço pago foi ativado e não houve publicação na Play Store.
