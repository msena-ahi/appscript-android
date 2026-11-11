# DevStudioIA Android — estado verificado

- Conta GitHub: msena-ahi. Repositório: msena-ahi/appscript-android.
- Código enviado; compilação debug concluída com sucesso em 11/11/2026.
- Execução #1: falha no pacote obsoleto Android SDK `tools`; corrigido para `platform-tools`.
- Execução #2: sucesso em todas as etapas, incluindo upload do APK.
- Build: https://github.com/msena-ahi/appscript-android/actions/runs/43115147714
- Commit compilado: 425f429cd2b3e66a5d006534fd3f9cc6ae6b15c7.
- APK: 3.823.984 bytes; SHA-256 `53c1911b60fb6d56e37e36822d9f6d1dcd5e9651c3dc2c4af86267666169dd68`.
- Integridade do ZIP e do APK verificada contra o digest do artefato e CRC.
- AndroidX e placeholder AppAuth configurados; Gradle 8.9, Java 17, SDK 35.
- Workflow manual em runner Ubuntu padrão, cache desabilitado, artefato retido por 1 dia.
- Nenhuma publicação na Play Store nem serviço pago ativado.
- Instalação e execução em aparelho ainda não verificadas.
- OAuth Google Android ainda precisa ser configurado e homologado: redirect customizado pode ser rejeitado pelo provedor.
- Firebase Manager e IA nativa não implementados nesta versão alfa.
- Antes de usar projetos reais, implementar controle de conflitos e testar gravação em projetos de teste.
