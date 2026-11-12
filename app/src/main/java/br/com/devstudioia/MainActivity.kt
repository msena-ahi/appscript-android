package br.com.devstudioia

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.ClearTokenRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.auth.api.identity.RevokeAccessRequest
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    private val client = OkHttpClient.Builder().callTimeout(30, TimeUnit.SECONDS).retryOnConnectionFailure(false).build()
    private val repository = ScriptRepository(client)
    private val worker = Executors.newSingleThreadExecutor()
    private val auth by lazy { Identity.getAuthorizationClient(this) }
    private val scopes = listOf(Scope("https://www.googleapis.com/auth/drive.metadata.readonly"), Scope("https://www.googleapis.com/auth/script.projects"))
    private var account: Account? = null
    private var token: String? = null
    @Volatile private var generation = 0
    private var pendingGeneration = 0
    private var busy = false
    private var project: ScriptProject? = null
    private var baseline: ProjectDocument? = null
    private var draft: ProjectDocument? = null
    private var selected = -1
    private var page: ProjectPage? = null
    private var pageToken: String? = null
    private var exportText: String? = null
    private lateinit var root: LinearLayout
    private lateinit var status: TextView
    private var editor: EditText? = null

    private val chooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) { busy = false; home("Seleção de conta cancelada."); return@registerForActivityResult }
        val email = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        val type = result.data?.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE)
        if (email.isNullOrBlank() || type != "com.google") { busy = false; home("Selecione uma conta Google válida."); return@registerForActivityResult }
        account = Account(email, "com.google")
        pendingGeneration = generation
        authorize()
    }
    private val consent = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (pendingGeneration != generation) return@registerForActivityResult
        if (result.resultCode != Activity.RESULT_OK) { busy = false; token = null; home("Permissão cancelada. Nenhum dado foi alterado."); return@registerForActivityResult }
        try { authorized(auth.getAuthorizationResultFromIntent(result.data ?: throw IOException("Autorização vazia"))) }
        catch (e: Exception) { authFailed(e) }
    }
    private val export = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val text = exportText; exportText = null
        if (uri == null || text == null) { message("Exportação cancelada."); return@registerForActivityResult }
        try {
            val stream = contentResolver.openOutputStream(uri, "wt") ?: throw IOException("Não foi possível abrir o destino")
            stream.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            message("Backup exportado com sucesso.")
        } catch (_: Exception) { message("Não foi possível exportar. O rascunho continua no aplicativo.") }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (busy) { message("Aguarde a operação terminar."); return }
                if (editor != null) showFiles() else if (draft != null) leaveProject { home() } else finish()
            }
        })
        home()
        val recovery = File(filesDir, "editor-recovery.json")
        if (recovery.exists()) AlertDialog.Builder(this).setTitle("Recuperar rascunho local?")
            .setMessage("Há um projeto aberto anteriormente. Você pode recuperar e exportar as alterações sem gravar no Google.")
            .setNegativeButton("Descartar") { _, _ -> recovery.delete() }
            .setPositiveButton("Recuperar") { _, _ ->
                try {
                    val json = JSONObject(recovery.readText())
                    project = ScriptProject(json.getString("id"), json.getString("name"))
                    baseline = ProjectDocument.parse(json.getJSONObject("original"))
                    draft = ProjectDocument.parse(json.getJSONObject("draft"))
                    showFiles(); message("Rascunho recuperado. Exporte antes de conectar outra sessão.")
                } catch (_: Exception) { message("Rascunho local inválido. Nenhuma alteração remota foi feita.") }
            }.show()
    }
    private fun text(value: String, size: Float = 16f) = TextView(this).apply {
        text = value; textSize = size; setTextColor(Color.rgb(30, 41, 59)); setPadding(12, 12, 12, 12)
    }
    private fun button(label: String, enabled: Boolean = !busy, action: () -> Unit) {
        root.addView(Button(this).apply { text = label; isEnabled = enabled; isAllCaps = false; setOnClickListener { action() } })
    }
    private fun screen(title: String) {
        editor = null
        root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 24, 24, 24); setBackgroundColor(Color.WHITE) }
        val scroll = ScrollView(this)
        scroll.setOnApplyWindowInsetsListener { v, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.ime())
                v.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            } else {
                @Suppress("DEPRECATION")
                v.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            }
            insets
        }
        scroll.addView(root); setContentView(scroll); scroll.requestApplyInsets()
        root.addView(text("DevStudioIA • $title", 24f).apply { setTypeface(null, Typeface.BOLD) })
        status = text(if (token != null) "Conectado: ${account?.name}" else "Conta não conectada", 14f)
        status.accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        root.addView(status)
    }
    private fun home(note: String? = null) {
        screen("Mobile IDE")
        root.addView(text("Use sua conta Google pessoal ou corporativa. A senha é informada somente ao Google.", 15f))
        button(if (token == null) "Entrar com Google" else "Trocar conta Google") { leaveProject { chooseAccount() } }
        button("Meus projetos Apps Script", token != null && !busy) { listProjects() }
        button("Sair da conta", account != null && !busy) { leaveProject { disconnect() } }
        button("Diagnóstico e configuração", !busy) { diagnostics() }
        root.addView(text("Versão 1.1 alfa • Firebase e copiloto ainda não disponíveis.", 12f))
        note?.let { message(it) }
    }
    private fun message(value: String) { if (!isDestroyed) status.text = value }
    private fun controls(enabled: Boolean) {
        for (i in 0 until root.childCount) { val v = root.getChildAt(i); if (v is Button || v is EditText) v.isEnabled = enabled }
    }
    private fun chooseAccount() {
        clearSession()
        home()
        busy = true; controls(false)
        try {
            chooser.launch(AccountManager.newChooseAccountIntent(null, null, arrayOf("com.google"), "Escolha a conta para o DevStudioIA", null, null, null))
        } catch (_: Exception) { busy = false; home("Não foi possível abrir as contas. Adicione uma conta Google nas configurações do Android.") }
    }
    private fun authorize() {
        val available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)
        if (available != ConnectionResult.SUCCESS) {
            busy = false; home("Atualize ou habilite o Google Play Services para conectar sua conta.")
            GoogleApiAvailability.getInstance().getErrorDialog(this, available, 100)?.show(); return
        }
        val selectedAccount = account ?: return
        val expected = generation
        val request = AuthorizationRequest.builder().setAccount(selectedAccount).setRequestedScopes(scopes).build()
        auth.authorize(request).addOnSuccessListener { result ->
            if (expected != generation || isDestroyed) return@addOnSuccessListener
            if (result.hasResolution()) {
                val pending = result.pendingIntent
                if (pending == null) authFailed(IOException("Não foi possível abrir a autorização"))
                else try { consent.launch(IntentSenderRequest.Builder(pending.intentSender).build()) } catch (e: Exception) { authFailed(e) }
            } else authorized(result)
        }.addOnFailureListener { e -> if (expected == generation && !isDestroyed) authFailed(e) }
    }
    private fun authorized(result: AuthorizationResult) {
        val granted = result.grantedScopes.toSet()
        if (!scopes.all { it.scopeUri in granted } || result.accessToken.isNullOrBlank()) {
            busy = false; token = null; home("Acesso aos projetos não autorizado. Conecte novamente e revise as permissões."); return
        }
        token = result.accessToken; busy = false
        home("Conectado: ${account?.name}. Você já pode abrir seus projetos.")
    }
    private fun authFailed(e: Exception) {
        token = null; busy = false
        val code = (e as? ApiException)?.statusCode
        home(when (code) {
            10 -> "Cadastro do aplicativo no Google pendente. Use Diagnóstico para obter o pacote e a assinatura necessários ao administrador."
            7 -> "Sem conexão. Verifique a internet e tente novamente."
            16, 12501 -> "Entrada cancelada. Nenhuma alteração foi feita."
            else -> "Não foi possível autorizar a conta (código ${code ?: "indisponível"}). Consulte Diagnóstico e tente novamente."
        })
    }
    private fun clearSession() {
        generation++; token = null; account = null; busy = false
        clearProject()
    }
    private fun clearProject() { project = null; baseline = null; draft = null; selected = -1; page = null; pageToken = null; File(filesDir, "editor-recovery.json").delete() }
    private fun disconnect() {
        val selectedAccount = account
        val oldToken = token
        clearSession(); home("Sessão local encerrada.")
        if (oldToken != null) auth.clearToken(ClearTokenRequest.builder().setToken(oldToken).build())
        if (selectedAccount != null) auth.revokeAccess(RevokeAccessRequest.builder().setAccount(selectedAccount).setScopes(scopes).build())
            .addOnFailureListener { if (!isDestroyed && account == null) message("Sessão local encerrada. A revogação no Google falhou; remova o acesso em sua Conta Google se necessário.") }
    }
    private fun <T> network(task: (String, Int) -> T, done: (T) -> Unit) {
        val accessToken = token ?: run { message("Entre com Google para continuar."); return }
        if (busy) return
        val expected = generation; busy = true; controls(false); message("Processando…")
        worker.execute {
            val result = runCatching { task(accessToken, expected) }
            runOnUiThread {
                if (expected != generation || isDestroyed) return@runOnUiThread
                busy = false; controls(true)
                result.onSuccess(done).onFailure { error ->
                    if (error is ApiFailure && error.status == 401) {
                        token = null
                        auth.clearToken(ClearTokenRequest.builder().setToken(accessToken).build())
                        controls(false)
                        // Keep editor text available to copy/export before starting a new session.
                        root.addView(text("Sessão expirada. Exporte o rascunho antes de entrar novamente.", 14f))
                        button("Exportar rascunho", draft != null) { exportBackup() }
                        button("Voltar ao início", true) { leaveProject { home() } }
                    }
                    message(when (error) { is IOException, is IllegalArgumentException -> error.message ?: "Não foi possível concluir a operação"; else -> "Resposta inesperada. Tente recarregar o projeto." })
                }
            }
        }
    }
    private fun listProjects(next: String? = null) {
        network({ t, _ -> repository.listProjects(t, next) }) { result -> page = result; pageToken = next; showProjects() }
    }
    private fun showProjects() {
        screen("Projetos")
        val items = page?.projects.orEmpty()
        root.addView(text(if (items.isEmpty()) "Nenhum projeto encontrado nesta página." else "${items.size} projetos nesta página", 14f))
        items.forEach { item -> button(item.name) { openProject(item) } }
        page?.nextPage?.let { next -> button("Próxima página") { listProjects(next) } }
        if (pageToken != null) button("Primeira página") { listProjects() }
        button("Atualizar") { listProjects(pageToken) }
        button("Voltar") { home() }
    }
    private fun openProject(item: ScriptProject) {
        network({ t, _ -> repository.read(t, item.id) }) { document -> project = item; baseline = document; draft = document; selected = -1; showFiles() }
    }
    private fun dirty(): Boolean = draft != null && baseline?.sameContent(draft!!) == false
    private fun leaveProject(action: () -> Unit) {
        if (busy) { message("Aguarde a operação terminar."); return }
        if (dirty()) AlertDialog.Builder(this).setTitle("Alterações não salvas")
            .setMessage("Há um rascunho local. Você pode exportá-lo antes de sair.")
            .setNegativeButton("Continuar editando", null)
            .setNeutralButton("Exportar") { _, _ -> exportBackup() }
            .setPositiveButton("Descartar e sair") { _, _ -> clearProject(); action() }.show()
        else { clearProject(); action() }
    }
    private fun showFiles() {
        screen(project?.name ?: "Arquivos")
        val document = draft ?: return
        document.files.forEachIndexed { i, file -> button("${file.name} (${file.type})") { showEditor(i) } }
        button("Exportar backup", !busy) { exportBackup() }
        button("Recarregar do Google", token != null && !busy) {
            val item = project ?: return@button
            if (dirty()) AlertDialog.Builder(this).setTitle("Descartar rascunho?").setMessage("Recarregar substitui as alterações locais. Exporte antes de continuar.")
                .setNegativeButton("Cancelar", null).setPositiveButton("Recarregar") { _, _ -> openProject(item) }.show() else openProject(item)
        }
        button("Voltar aos projetos") { leaveProject { listProjects() } }
        if (dirty()) message("Alterações locais ainda não salvas.")
    }
    private fun showEditor(index: Int) {
        val file = draft?.files?.getOrNull(index) ?: return
        selected = index; screen(file.name)
        val input = EditText(this).apply {
            setText(file.source); minLines = 14; textSize = 14f; typeface = Typeface.MONOSPACE
            contentDescription = "Código de ${file.name}"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            setHorizontallyScrolling(true)
            addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { draft = draft?.edited(index, s.toString()) }
                override fun afterTextChanged(s: Editable?) {}
            })
        }
        editor = input; root.addView(input)
        button("Salvar no Google", token != null && !busy) { confirmSave() }
        button("Exportar backup") { exportBackup() }
        button("Voltar aos arquivos") { showFiles() }
        root.addView(text("O rascunho permanece ao trocar de arquivo. Salvar exige confirmação e backup prévio.", 12f))
    }
    private fun backupJson(original: ProjectDocument, edited: ProjectDocument) = JSONObject()
        .put("id", project?.id).put("name", project?.name).put("original", original.json()).put("draft", edited.json()).toString(2)
    private fun backup(original: ProjectDocument, edited: ProjectDocument): String {
        val directory = File(filesDir, "backups").apply { if (!exists() && !mkdirs()) throw IOException("Não foi possível criar o backup") }
        val target = File(directory, "backup_${System.currentTimeMillis()}_${System.nanoTime()}.json")
        target.writeText(backupJson(original, edited)); return target.name
    }
    private fun exportBackup() {
        val original = baseline ?: return; val edited = draft ?: return
        try {
            backup(original, edited)
            exportText = backupJson(original, edited)
            export.launch("DevStudioIA_backup_${System.currentTimeMillis()}.json")
        } catch (_: Exception) { exportText = null; message("Não foi possível criar o backup. Nenhuma gravação remota foi feita.") }
    }
    private fun confirmSave() {
        val original = baseline ?: return; val edited = draft ?: return; val item = project ?: return
        if (!dirty()) { message("Nenhuma alteração para salvar."); return }
        try { edited.validateForSave() } catch (e: Exception) { message(e.message ?: "Conteúdo inválido"); return }
        val changes = edited.files.count { f -> original.files.find { it.name == f.name } != f }
        AlertDialog.Builder(this).setTitle("Salvar alterações no Google?")
            .setMessage("Conta: ${account?.name}\nProjeto: ${item.name}\nArquivos alterados: $changes\n\nSerá salvo um backup local. O conteúdo remoto será conferido antes da gravação. Evite editar simultaneamente em outro dispositivo.")
            .setNegativeButton("Cancelar", null).setPositiveButton("Salvar") { _, _ ->
                var backupName = ""
                network({ t, g -> repository.save(t, item.id, original, edited, { g == generation }) { before, after -> backupName = backup(before, after) } }) { saved ->
                    baseline = saved; draft = saved
                    File(filesDir, "editor-recovery.json").delete()
                    if (selected in saved.files.indices) showEditor(selected) else showFiles()
                    message("Salvo no Google. Backup local: $backupName")
                }
            }.show()
    }
    @Suppress("DEPRECATION") private fun signingFingerprint(): String = try {
        val bytes = if (Build.VERSION.SDK_INT >= 28) packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES).signingInfo!!.apkContentsSigners.first().toByteArray()
            else packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures!!.first().toByteArray()
        MessageDigest.getInstance("SHA-1").digest(bytes).joinToString(":") { "%02X".format(it) }
    } catch (_: Exception) { "Não foi possível obter a assinatura" }
    private fun diagnostics() {
        screen("Diagnóstico")
        root.addView(text("Pacote: $packageName\nSHA-1: ${signingFingerprint()}", 14f))
        root.addView(text("O administrador deve cadastrar este pacote e SHA-1 como cliente Android no Google Cloud, configurar a tela de consentimento e habilitar Drive API e Apps Script API. O usuário entra escolhendo seu e-mail, sem digitar identificadores técnicos.", 14f))
        root.addView(text("A conta precisa permitir o uso da Apps Script API nas configurações do Apps Script. Contas corporativas podem depender da liberação do administrador.", 14f))
        button("Compartilhar diagnóstico") {
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, "DevStudioIA 1.1-alpha\nPacote: $packageName\nSHA-1: ${signingFingerprint()}"), "Compartilhar diagnóstico"))
        }
        button("Voltar") { home() }
    }
    override fun onStop() {
        super.onStop()
        val original = baseline; val edited = draft
        if (dirty() && original != null && edited != null) try { File(filesDir, "editor-recovery.json").writeText(backupJson(original, edited)) } catch (_: Exception) { /* explicit export stays available */ }
    }
    override fun onDestroy() { generation++; worker.shutdownNow(); super.onDestroy() }
}
