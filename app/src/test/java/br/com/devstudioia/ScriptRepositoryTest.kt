package br.com.devstudioia

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

class ScriptRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repo: ScriptRepository
    private val original = ProjectDocument(listOf(ScriptFile("Code", "SERVER_JS", "old"), ScriptFile("appsscript", "JSON", "{\"timeZone\":\"America/Sao_Paulo\"}")))
    private val edited get() = original.edited(0, "novo código çã 😀")
    @Before fun setup() {
        server = MockWebServer(); server.start()
        val client = OkHttpClient.Builder().readTimeout(2, TimeUnit.SECONDS).retryOnConnectionFailure(false).build()
        repo = ScriptRepository(client, server.url("/").toString(), server.url("/").toString())
    }
    @After fun cleanup() { server.shutdown() }
    private fun document(d: ProjectDocument = original) = MockResponse().setBody(d.json().toString())
    @Test fun projectsAreFilteredAndTokenSentInHeader() {
        server.enqueue(MockResponse().setBody("{\"files\":[{\"id\":\"abc\",\"name\":\"Meu projeto\"}],\"nextPageToken\":\"a+b &c\"}"))
        val page = repo.listProjects("sensitive-token")
        assertEquals("Meu projeto", page.projects.single().name); assertEquals("a+b &c", page.nextPage)
        val request = server.takeRequest()
        assertEquals("Bearer sensitive-token", request.getHeader("Authorization"))
        assertEquals("mimeType='application/vnd.google-apps.script' and trashed=false", request.requestUrl!!.queryParameter("q"))
        assertEquals("100", request.requestUrl!!.queryParameter("pageSize"))
        assertFalse(request.path!!.contains("sensitive-token"))
    }
    @Test fun pageTokenIsSafelyEncoded() {
        server.enqueue(MockResponse().setBody("{\"files\":[]}"))
        val result = repo.listProjects("t", "a+b &c")
        assertTrue(result.projects.isEmpty()); assertNull(result.nextPage)
        assertEquals("a+b &c", server.takeRequest().requestUrl!!.queryParameter("pageToken"))
    }
    @Test fun readParsesFilesAndPreservesUnicode() {
        server.enqueue(document(edited))
        assertEquals(edited, repo.read("t", "project-id"))
        assertEquals("/v1/projects/project-id/content", server.takeRequest().path)
    }
    @Test fun outputOnlyFieldsAreNotForwardedInSavePayload() {
        val raw = original.json().put("scriptId", "abc")
        raw.getJSONArray("files").getJSONObject(0).put("functionSet", JSONObject()).put("createTime", "yesterday")
        val parsed = ProjectDocument.parse(raw)
        assertEquals(setOf("files"), parsed.json().keySet())
        assertEquals(setOf("name", "type", "source"), parsed.json().getJSONArray("files").getJSONObject(0).keySet())
    }
    @Test fun editDoesNotMutateBaselineOrOtherFiles() {
        assertEquals("old", original.files[0].source)
        assertEquals(original.files[1], edited.files[1])
        assertNotEquals(original.files[0], edited.files[0])
    }
    @Test fun saveChecksRemoteThenBacksUpThenWritesAllFiles() {
        server.enqueue(document()); server.enqueue(document(edited))
        var backedUp = false
        val saved = repo.save("t", "abc", original, edited) { before, after ->
            assertEquals(original, before); assertEquals(edited, after)
            assertEquals(1, server.requestCount); backedUp = true
        }
        assertTrue(backedUp); assertEquals(edited, saved)
        assertEquals("GET", server.takeRequest().method)
        val put = server.takeRequest()
        assertEquals("PUT", put.method)
        val body = JSONObject(put.body.readUtf8())
        assertEquals(2, body.getJSONArray("files").length())
        assertEquals("novo código çã 😀", body.getJSONArray("files").getJSONObject(0).getString("source"))
    }
    @Test fun conflictStopsBeforeBackupAndPut() {
        server.enqueue(document(original.edited(0, "mudou no Google")))
        assertThrows(RemoteConflict::class.java) { repo.save("t", "abc", original, edited) { _, _ -> fail("Backup must not run") } }
        assertEquals(1, server.requestCount)
    }
    @Test fun fileOrderDoesNotCreateFalseConflict() {
        server.enqueue(document(original.copy(files = original.files.reversed()))); server.enqueue(document(edited))
        assertEquals(edited, repo.save("t", "abc", original, edited) { _, _ -> })
    }
    @Test fun backupFailureStopsPut() {
        server.enqueue(document())
        assertThrows(IOException::class.java) { repo.save("t", "abc", original, edited) { _, _ -> throw IOException("disk full") } }
        assertEquals(1, server.requestCount)
    }
    @Test fun changedSessionStopsBeforeAnyRequest() {
        assertThrows(SessionChanged::class.java) { repo.save("t", "abc", original, edited, { false }) { _, _ -> fail() } }
        assertEquals(0, server.requestCount)
    }
    @Test fun changedSessionAfterBackupStopsPut() {
        server.enqueue(document()); var active = true
        assertThrows(SessionChanged::class.java) { repo.save("t", "abc", original, edited, { active }) { _, _ -> active = false } }
        assertEquals(1, server.requestCount)
    }
    @Test fun missingManifestStopsBeforeRequest() {
        val bad = ProjectDocument(listOf(original.files[0]))
        assertThrows(IllegalArgumentException::class.java) { repo.save("t", "abc", original, bad) { _, _ -> fail() } }
        assertEquals(0, server.requestCount)
    }
    @Test fun invalidManifestStopsBeforeRequest() {
        val bad = original.edited(1, "not JSON")
        assertThrows(IllegalArgumentException::class.java) { repo.save("t", "abc", original, bad) { _, _ -> fail() } }
        assertEquals(0, server.requestCount)
    }
    @Test fun duplicateNamesAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { ProjectDocument(original.files + original.files[0]).validateForSave() }
    }
    @Test fun httpErrorsExposeStatusWithoutLeakingResponseSecrets() {
        for (code in listOf(401, 403, 404, 429, 500)) {
            server.enqueue(MockResponse().setResponseCode(code).setBody("secret backend data"))
            val error = assertThrows(ApiFailure::class.java) { repo.listProjects("t") }
            assertEquals(code, error.status); assertFalse(error.message!!.contains("secret"))
        }
    }
    @Test fun malformedResponseFailsWithoutWriting() {
        server.enqueue(MockResponse().setBody("<html>outage</html>"))
        assertThrows(IOException::class.java) { repo.read("t", "abc") }
        assertEquals(1, server.requestCount)
    }
    @Test fun failedPutLeavesBaselineAndDraftUntouched() {
        server.enqueue(document()); server.enqueue(MockResponse().setResponseCode(403))
        assertThrows(ApiFailure::class.java) { repo.save("t", "abc", original, edited) { _, _ -> } }
        assertEquals("old", original.files[0].source); assertEquals("novo código çã 😀", edited.files[0].source)
        assertEquals(2, server.requestCount)
    }
    @Test fun ambiguousWriteIsNeverAutomaticallyRetried() {
        server.enqueue(document()); server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AFTER_REQUEST))
        assertThrows(IOException::class.java) { repo.save("t", "abc", original, edited) { _, _ -> } }
        assertEquals(2, server.requestCount)
    }
}
