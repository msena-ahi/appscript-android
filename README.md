# DevStudioIA Android 1.2 alfa

## Como funciona

1. Toque em **Entrar com Google** e escolha uma conta Google instalada no Android.
2. O DevStudioIA abre `script.google.com/home` no navegador e inclui o e-mail escolhido como indicação de conta.
3. Se o navegador ainda não estiver conectado a essa conta, o Google solicita o login. O próprio Apps Script mostra os projetos disponíveis e fornece o editor e seus recursos oficiais.

O APK não coleta senha, Client ID, Client Secret nem token Google. A autenticação e o editor são tratados pelo site oficial em Chrome ou outro navegador configurado no aparelho. O usuário pode trocar de conta no Google se o navegador não tiver uma sessão ativa para a conta selecionada.

Essa abordagem mantém os recursos e a interface oferecidos pelo Apps Script, sem tentar reproduzir o editor do Google dentro do APK. A tela nativa do DevStudioIA funciona como seleção de conta e acesso ao editor oficial; a listagem e as operações acontecem no Apps Script do navegador.

## Validação

- Execução #8 do GitHub Actions: https://github.com/msena-ahi/appscript-android/actions/runs/43199743849
- Build e verificação de assinatura do APK: sucesso.
- Testes automatizados: 25 aprovados, 0 falhas/erros/ignorados (18 do repositório/API e 7 da interface).
- Android Lint: 0 erros e 4 avisos.
- Os testes confirmam o seletor de conta, cancelamento e abertura do endereço oficial com o e-mail escolhido. Eles não substituem um teste manual de login no Chrome e carregamento dos projetos no aparelho.

## Custo e instalação

A compilação utiliza runner Ubuntu padrão do GitHub Actions. Não houve publicação na Play Store nem ativação de serviço pago. Este APK é debug/alfa. A chave de assinatura é gerada pelo runner; se o Android informar incompatibilidade de assinatura ao atualizar uma versão anterior, exporte quaisquer rascunhos e desinstale a versão anterior antes de instalar esta.
