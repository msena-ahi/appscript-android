package br.com.devstudioia

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class ApiFailure(val status: Int) : IOException(when (status) {
    401 -> "Sessão expirada. Conecte sua conta novamente."
    403 -> "Acesso negado. Verifique as permissões, as APIs habilitadas e a ativação da Apps Script API na conta."
    404 -> "Projeto não encontrado ou indisponível para esta conta."
    429 -> "Limite de solicitações atingido. Aguarde antes de tentar novamente."
    else -> "O Google retornou HTTP $status. Tente novamente mais tarde."
})
class RemoteConflict : IOException("O projeto mudou no Google. Gravação cancelada; exporte seu rascunho e recarregue o projeto.")
class SessionChanged : IOException("A conta mudou. Operação cancelada.")

data class ScriptFile(val name: String, val type: String, val source: String) {
    fun json() = JSONObject().put("name", name).put("type", type).put("source", source)
}
data class ProjectDocument(val files: List<ScriptFile>) {
    fun json() = JSONObject().put("files", JSONArray().apply { files.forEach { put(it.json()) } })
    fun edited(index: Int, source: String): ProjectDocument {
        require(index in files.indices) { "Arquivo inválido" }
        return copy(files = files.mapIndexed { i, f -> if (i == index) f.copy(source = source) else f })
    }
    fun validateForSave() {
        require(files.isNotEmpty()) { "O projeto está vazio" }
        require(files.map { it.name }.distinct().size == files.size) { "Nomes de arquivo duplicados" }
        val manifest = files.singleOrNull { it.name == "appsscript" && it.type == "JSON" }
        requireNotNull(manifest) { "O manifesto appsscript.json deve ser preservado" }
        try { JSONObject(manifest.source) } catch (_: Exception) { throw IllegalArgumentException("O manifesto appsscript.json contém JSON inválido") }
    }
    fun sameContent(other: ProjectDocument) = files.sortedBy { it.name } == other.files.sortedBy { it.name }
    companion object {
        fun parse(json: JSONObject): ProjectDocument {
            val entries = json.getJSONArray("files")
            return ProjectDocument((0 until entries.length()).map { i ->
                val f = entries.getJSONObject(i)
                ScriptFile(f.getString("name"), f.getString("type"), f.optString("source", ""))
            })
        }
    }
}
data class ScriptProject(val id: String, val name: String)
data class ProjectPage(val projects: List<ScriptProject>, val nextPage: String?)

class ScriptRepository(
    private val client: OkHttpClient,
    private val driveBase: String = "https://www.googleapis.com/",
    private val scriptBase: String = "https://script.googleapis.com/"
) {
    private fun execute(request: Request): JSONObject = client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw ApiFailure(response.code)
        val text = response.body?.string() ?: throw IOException("Resposta vazia do Google")
        try { JSONObject(text) } catch (_: Exception) { throw IOException("Resposta inválida do Google") }
    }
    fun listProjects(token: String, nextPage: String? = null): ProjectPage {
        val url = driveBase.toHttpUrl().newBuilder().addPathSegments("drive/v3/files")
            .addQueryParameter("q", "mimeType='application/vnd.google-apps.script' and trashed=false")
            .addQueryParameter("fields", "nextPageToken,files(id,name)")
            .addQueryParameter("pageSize", "100").addQueryParameter("orderBy", "name")
            .addQueryParameter("supportsAllDrives", "true").addQueryParameter("includeItemsFromAllDrives", "true")
        if (!nextPage.isNullOrBlank()) url.addQueryParameter("pageToken", nextPage)
        val json = execute(Request.Builder().url(url.build()).header("Authorization", "Bearer $token").build())
        val items = json.getJSONArray("files")
        return ProjectPage((0 until items.length()).map { i ->
            val item = items.getJSONObject(i); ScriptProject(item.getString("id"), item.getString("name"))
        }, json.optString("nextPageToken").takeIf { it.isNotBlank() })
    }
    private fun contentUrl(id: String) = scriptBase.toHttpUrl().newBuilder()
        .addPathSegments("v1/projects").addPathSegment(id).addPathSegment("content").build()
    fun read(token: String, id: String): ProjectDocument = ProjectDocument.parse(execute(
        Request.Builder().url(contentUrl(id)).header("Authorization", "Bearer $token").build()))
    fun save(token: String, id: String, baseline: ProjectDocument, draft: ProjectDocument,
             active: () -> Boolean = { true }, backup: (ProjectDocument, ProjectDocument) -> Unit): ProjectDocument {
        draft.validateForSave()
        if (!active()) throw SessionChanged()
        val current = read(token, id)
        if (!current.sameContent(baseline)) throw RemoteConflict()
        if (!active()) throw SessionChanged()
        backup(current, draft) // Failure aborts the PUT; no remote write without a backup.
        if (!active()) throw SessionChanged()
        return ProjectDocument.parse(execute(Request.Builder().url(contentUrl(id))
            .header("Authorization", "Bearer $token")
            .put(draft.json().toString().toRequestBody("application/json; charset=utf-8".toMediaType())).build()))
    }
}
