# DevStudioIA Android V1 alpha — código-fonte

**Status:** projeto Android para compilação; **APK ainda não compilado/testado**. Sem publicação, sem custos contratados.

## Compilar APK grátis

1. Código hospedado em `msena-ahi/appscript-android`. O workflow usa um runner Ubuntu padrão em repositório público.
2. Abra **Actions → Build DevStudioIA APK (manual) → Run workflow**.
3. Aguarde o resultado. Se a compilação concluir, baixe o artefato `DevStudioIA-debug-apk`, extraia o `app-debug.apk` e instale no Android. O GitHub Actions tem limites gratuitos e pode depender do plano da conta.
4. Não coloque Client ID, token, chave privada ou senha no repositório. Informe o OAuth Client ID apenas no aplicativo.

## Configuração Google OAuth

- No Google Cloud Console, configure tela de consentimento OAuth e escopos necessários, e habilite **Drive API** e **Apps Script API**.
- Crie um cliente OAuth apropriado para aplicativo nativo e configure redirecionamento **`br.com.devstudioia://oauth2redirect`**. **Atenção:** o Google pode não aceitar esse esquema de redirecionamento para um cliente Android. Se não for aceito, o fluxo OAuth precisa ser adaptado a uma opção suportada pelo Google antes de testar login. Não contorne restrições de segurança.
- A Google Apps Script API pode exigir ativação em `script.google.com/home/usersettings`.
- O projeto não inclui backend ou cliente secreto. O token OAuth é mantido apenas em memória.

## Escopo realmente implementado

- Interface Android nativa simples.
- Login via navegador externo/AppAuth e seleção de conta (ainda não homologado).
- Listagem de até 100 projetos Apps Script da conta autorizada via Drive API.
- Leitura e edição de arquivos de projeto via Apps Script API.
- Backup local privado antes de salvar e confirmação explícita.
- Troca de conta por desconexão e novo login; **não** há múltiplas sessões simultâneas.

## Limitações / segurança

- **Firebase Manager e DevCopilot não estão implementados no APK**; permanecem previstos para próximas versões.
- Não há diff visual, controle de conflitos, histórico de versões remoto ou paginação completa.
- A gravação via `projects.updateContent` substitui todo o conteúdo do projeto. Use somente com projetos de teste. Não edite projetos críticos nesta versão.
- O app não instala nem publica projetos Apps Script e não altera Firebase.
- Não existe APK validado nesta entrega. A compilação pode exigir ajustes de dependências ou configuração OAuth.
- O APK debug é para homologação local, não para distribuição pública.
