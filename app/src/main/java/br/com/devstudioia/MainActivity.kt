package br.com.devstudioia

import android.app.Activity
import android.os.Bundle
import android.graphics.Typeface
import android.view.View
import android.widget.*
import android.text.InputType
import android.net.Uri
import net.openid.appauth.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
 private val client = OkHttpClient.Builder().callTimeout(25, TimeUnit.SECONDS).build()
 private lateinit var authService: AuthorizationService
 private var authState: AuthState? = null
 private var token: String? = null
 private var projectId: String? = null
 private var files: JSONObject? = null
 private var selectedFile = 0
 private var clientId = ""
 private lateinit var root: LinearLayout
 private lateinit var status: TextView
 private lateinit var editor: EditText
 private var projectFiles = org.json.JSONArray()
 private val redirect = Uri.parse("br.com.devstudioia://oauth2redirect")
 private val scopes = "openid email profile https://www.googleapis.com/auth/drive.metadata.readonly https://www.googleapis.com/auth/script.projects"
 override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); authService = AuthorizationService(this); home(); consumeAuth() }
 override fun onNewIntent(intent:android.content.Intent) { super.onNewIntent(intent); consumeAuth() }
 private fun consumeAuth() { val response=AuthSession.response; val error=AuthSession.error; if(response!=null || error!=null){AuthSession.response=null;AuthSession.error=null; if(response!=null) exchangeAuth(response,error) else status.text="OAuth falhou: ${error?.errorDescription}"} }
 private fun label(s: String, size: Float = 16f): TextView = TextView(this).apply { text=s; textSize=size; setPadding(12,12,12,12) }
 private fun button(s:String, action:()->Unit) { root.addView(Button(this).apply { text=s; setOnClickListener { action() } }) }
 private fun screen(title:String) {
  root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(18,20,18,12) }
  val scroll=ScrollView(this); scroll.addView(root); setContentView(scroll)
  root.addView(label("DevStudioIA  •  $title",22f).apply { setTypeface(null,Typeface.BOLD) })
  status=label("Conta não conectada",13f); root.addView(status)
 }
 private fun home() {
  screen("Mobile IDE")
  val idInput=EditText(this).apply { hint="OAuth Client ID (Android)"; setSingleLine(true); setText(clientId) }; root.addView(idInput)
  button("Conectar / adicionar conta Google") { clientId=idInput.text.toString().trim(); if(clientId.isBlank()) { status.text="Informe o Client ID"; return@button }; signIn() }
  button("Listar meus projetos Apps Script") { listProjects() }
  button("Desconectar / alternar conta") { authState=null; token=null; projectId=null; status.text="Sessão local encerrada. Conecte outra conta." }
  root.addView(label("V1 alfa: projetos Apps Script. Firebase e copiloto nativo serão integrados nas próximas etapas.",13f))
 }
 private fun signIn() {
  val config=AuthorizationServiceConfiguration(Uri.parse("https://accounts.google.com/o/oauth2/v2/auth"),Uri.parse("https://oauth2.googleapis.com/token"))
  val req=AuthorizationRequest.Builder(config,clientId,"code",redirect).setScope(scopes).setAdditionalParameters(mapOf("prompt" to "select_account")).build()
  authService.performAuthorizationRequest(req,android.app.PendingIntent.getActivity(this,123,android.content.Intent(this,AuthResultActivity::class.java),android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_MUTABLE))
 }
 private fun exchangeAuth(response:AuthorizationResponse,error:AuthorizationException?) {
  authState=AuthState(response,error)
  authService.performTokenRequest(response.createTokenExchangeRequest()) { tokenResponse, exception ->
   runOnUiThread {
    if(tokenResponse!=null) { authState?.update(tokenResponse,exception); token=tokenResponse.accessToken; status.text="Conta conectada (token temporário em memória)" }
    else status.text="Falha ao trocar código OAuth: ${exception?.errorDescription}"
   }
  }
 }
 private fun get(url:String, done:(JSONObject)->Unit) {
  val t=token ?: run {status.text="Conecte uma conta primeiro";return}
  val req=Request.Builder().url(url).header("Authorization","Bearer $t").build()
  client.newCall(req).enqueue(object:Callback {
   override fun onFailure(call:Call,e:IOException) {runOnUiThread{status.text="Rede: ${e.message}"}}
   override fun onResponse(call:Call,response:Response) { response.use {
    val body=it.body?.string().orEmpty()
    runOnUiThread { if(!it.isSuccessful) status.text="HTTP ${it.code}: ${body.take(300)}" else try { done(JSONObject(body)) } catch(e:Exception) { status.text="Resposta inválida: ${e.message}" } }
   } }
  })
 }
 private fun listProjects() {
  get("https://www.googleapis.com/drive/v3/files?q=mimeType%3D%27application%2Fvnd.google-apps.script%27%20and%20trashed%3Dfalse&fields=nextPageToken%2Cfiles(id%2Cname%2CmodifiedTime)&pageSize=100") { obj ->
   screen("Projetos Apps Script")
   val arr=obj.optJSONArray("files") ?: org.json.JSONArray()
   root.addView(label("${arr.length()} projetos nesta página"))
   for(i in 0 until arr.length()) { val item=arr.getJSONObject(i); button(item.optString("name","Projeto")) {projectId=item.getString("id"); openProject()} }
   if(obj.has("nextPageToken")) root.addView(label("Mais páginas disponíveis: paginação pendente nesta versão."))
   button("Voltar") {home()}
  }
 }
 private fun openProject() {
  val id=projectId ?:return
  get("https://script.googleapis.com/v1/projects/$id/content") { obj ->
   files=obj; projectFiles=obj.optJSONArray("files") ?: org.json.JSONArray(); screen("Editor")
   root.addView(label("Projeto: $id",12f))
   for(i in 0 until projectFiles.length()) { val index=i; val f=projectFiles.getJSONObject(i); button(f.optString("name","arquivo")+" ("+f.optString("type")+")") {showEditor(index)} }
   button("Voltar aos projetos") {listProjects()}
  }
 }
 private fun showEditor(index:Int) {
  selectedFile=index; val f=projectFiles.getJSONObject(index)
  screen("${f.optString("name")}")
  root.addView(label("Editor de texto • alterações locais até salvar"))
  editor=EditText(this).apply {setText(f.optString("source")); minLines=16; textSize=13f; typeface=Typeface.MONOSPACE; inputType=InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS; setHorizontallyScrolling(true)}
  root.addView(editor)
  button("Salvar cópia local (backup)") {backup()}
  button("Revisar e salvar no Google") { confirmSave() }
  button("Voltar") {openProject()}
 }
 private fun backup() {
  val id=projectId ?:return
  val backupText=files?.toString(2) ?:return
  val name="devstudioia_${id}_${System.currentTimeMillis()}.json"
  try {openFileOutput(name,MODE_PRIVATE).use {it.write(backupText.toByteArray(Charsets.UTF_8))};status.text="Backup local privado: $name"}
  catch(e:Exception){status.text="Falha no backup: ${e.message}"}
 }
 private fun confirmSave() {
  val id=projectId ?:return
  val old=projectFiles.getJSONObject(selectedFile).optString("source")
  val changed=editor.text.toString()
  if(old==changed) {status.text="Nenhuma alteração";return}
  android.app.AlertDialog.Builder(this).setTitle("Substituir código remoto?")
   .setMessage("Arquivo: ${projectFiles.getJSONObject(selectedFile).optString("name")}\nAntes: ${old.length} caracteres\nDepois: ${changed.length} caracteres\n\nO Apps Script API substitui o conjunto de arquivos. Evite edição simultânea. Backup será salvo antes da alteração.")
   .setNegativeButton("Cancelar",null).setPositiveButton("Confirmar") { _,_ ->
    val backupText=files?.toString(2) ?:return@setPositiveButton
    try {openFileOutput("backup_${id}_${System.currentTimeMillis()}.json",MODE_PRIVATE).use {it.write(backupText.toByteArray())}}catch(e:Exception){status.text="Backup falhou; gravação cancelada";return@setPositiveButton}
    val t=token ?:return@setPositiveButton
    val newFiles=org.json.JSONArray(projectFiles.toString());newFiles.getJSONObject(selectedFile).put("source",changed)
    val payload=JSONObject().put("files",newFiles).toString()
    val req=Request.Builder().url("https://script.googleapis.com/v1/projects/$id/content").header("Authorization","Bearer $t").put(payload.toRequestBody("application/json; charset=utf-8".toMediaType())).build()
    client.newCall(req).enqueue(object:Callback {
     override fun onFailure(call:Call,e:IOException){runOnUiThread{status.text="Falha: ${e.message}"}}
     override fun onResponse(call:Call,response:Response){response.use{val result=it.body?.string().orEmpty();runOnUiThread{if(it.isSuccessful){status.text="Código salvo; recarregue o projeto"}else{status.text="HTTP ${it.code}: ${result.take(300)}"}}}}
    })
   }.show()
 }
 override fun onDestroy(){authService.dispose();super.onDestroy()}
}
