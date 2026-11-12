package br.com.devstudioia

import android.app.Activity
import android.app.AlertDialog
import android.accounts.Account
import org.robolectric.shadows.ShadowAlertDialog
import org.json.JSONObject
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class MainActivityTest {
    private fun views(v: View): List<View> = listOf(v) + if (v is ViewGroup) (0 until v.childCount).flatMap { views(v.getChildAt(it)) } else emptyList()
    @Test fun loginHasNoTechnicalIdFieldAndProjectsRequireAuthorization() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        val all = views(activity.window.decorView)
        assertTrue(all.filterIsInstance<EditText>().isEmpty())
        assertTrue(all.filterIsInstance<Button>().any { it.text == "1. Entrar com Google" && it.isEnabled })
        assertFalse(all.filterIsInstance<Button>().single { it.text == "2. Listar projetos desta conta" }.isEnabled)
        assertFalse(all.filterIsInstance<Button>().single { it.text == "Sair da conta" }.isEnabled)
        controller.pause().stop().destroy()
    }
    @Test fun connectOpensNativeChooserAndCancelRestoresUi() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        views(activity.window.decorView).filterIsInstance<Button>().single { it.text == "1. Entrar com Google" }.performClick()
        val started = shadowOf(activity).nextStartedActivityForResult
        assertNotNull(started)
        assertNull(started.intent.data) // No browser redirect or client ID is used.
        activity.activityResultRegistry.dispatchResult(started.requestCode, Activity.RESULT_CANCELED, null)
        val all = views(activity.window.decorView)
        assertTrue(all.filterIsInstance<TextView>().any { it.text.toString().contains("Seleção de conta cancelada") })
        assertTrue(all.filterIsInstance<Button>().single { it.text == "1. Entrar com Google" }.isEnabled)
        controller.pause().stop().destroy()
    }
    @Test fun projectListingIsEnabledOnlyAfterGoogleAccountAuthorization() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup(); val activity = controller.get()
        assertFalse(views(activity.window.decorView).filterIsInstance<Button>().single { it.text == "2. Listar projetos desta conta" }.isEnabled)
        MainActivity::class.java.getDeclaredField("account").apply { isAccessible = true }.set(activity, Account("marcelo@example.com", "com.google"))
        MainActivity::class.java.getDeclaredField("token").apply { isAccessible = true }.set(activity, "token-for-test")
        MainActivity::class.java.getDeclaredMethod("home").apply { isAccessible = true }.invoke(activity)
        val connected = views(activity.window.decorView)
        assertTrue(connected.filterIsInstance<TextView>().any { it.text.toString().contains("marcelo@example.com") })
        assertTrue(connected.filterIsInstance<Button>().single { it.text == "2. Listar projetos desta conta" }.isEnabled)
        controller.pause().stop().destroy()
    }
    @Test fun diagnosticsContainsPackageAndNoLoginField() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        views(activity.window.decorView).filterIsInstance<Button>().single { it.text == "Diagnóstico e configuração" }.performClick()
        val all = views(activity.window.decorView)
        assertTrue(all.filterIsInstance<TextView>().any { it.text.toString().contains("br.com.devstudioia") })
        assertTrue(all.filterIsInstance<EditText>().isEmpty())
        controller.pause().stop().destroy()
    }
    private fun seed(activity: MainActivity) {
        fun field(name: String, value: Any) { MainActivity::class.java.getDeclaredField(name).apply { isAccessible = true }.set(activity, value) }
        val doc = ProjectDocument(listOf(ScriptFile("Code", "SERVER_JS", "original"), ScriptFile("appsscript", "JSON", "{}")))
        field("project", ScriptProject("test-id", "Projeto de teste")); field("baseline", doc); field("draft", doc)
        field("account", Account("test@example.com", "com.google")); field("token", "test-only-token")
        MainActivity::class.java.getDeclaredMethod("showFiles").apply { isAccessible = true }.invoke(activity)
    }
    private fun click(activity: MainActivity, text: String) = views(activity.window.decorView).filterIsInstance<Button>().single { it.text == text }.performClick()
    @Test fun editsSurviveFileNavigation() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup(); val activity = controller.get(); seed(activity)
        click(activity, "Code (SERVER_JS)")
        views(activity.window.decorView).filterIsInstance<EditText>().single().setText("alteração local")
        click(activity, "Voltar aos arquivos"); click(activity, "appsscript (JSON)"); click(activity, "Voltar aos arquivos"); click(activity, "Code (SERVER_JS)")
        assertEquals("alteração local", views(activity.window.decorView).filterIsInstance<EditText>().single().text.toString())
        controller.pause().stop().destroy()
        java.io.File(activity.filesDir, "editor-recovery.json").delete()
    }
    @Test fun saveRequiresConfirmationAndCancelKeepsDraft() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup(); val activity = controller.get(); seed(activity)
        click(activity, "Code (SERVER_JS)"); views(activity.window.decorView).filterIsInstance<EditText>().single().setText("rascunho")
        click(activity, "Salvar no Google")
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(dialog.isShowing)
        assertTrue(shadowOf(dialog).message.toString().contains("test@example.com"))
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        assertEquals("rascunho", views(activity.window.decorView).filterIsInstance<EditText>().single().text.toString())
        controller.pause().stop().destroy(); java.io.File(activity.filesDir, "editor-recovery.json").delete()
    }
    @Test fun exportIncludesDraftAndOriginalButNoAccessToken() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup(); val activity = controller.get(); seed(activity)
        click(activity, "Code (SERVER_JS)"); views(activity.window.decorView).filterIsInstance<EditText>().single().setText("código novo")
        click(activity, "Exportar backup")
        val backup = java.io.File(activity.filesDir, "backups").listFiles()!!.maxBy { it.lastModified() }
        val json = JSONObject(backup.readText())
        assertEquals("original", json.getJSONObject("original").getJSONArray("files").getJSONObject(0).getString("source"))
        assertEquals("código novo", json.getJSONObject("draft").getJSONArray("files").getJSONObject(0).getString("source"))
        assertFalse(backup.readText().contains("test-only-token"))
        val started = shadowOf(activity).nextStartedActivityForResult
        assertEquals(android.content.Intent.ACTION_CREATE_DOCUMENT, started.intent.action)
        activity.activityResultRegistry.dispatchResult(started.requestCode, Activity.RESULT_CANCELED, null)
        assertTrue(views(activity.window.decorView).filterIsInstance<TextView>().any { it.text == "Exportação cancelada." })
        controller.pause().stop().destroy(); java.io.File(activity.filesDir, "editor-recovery.json").delete()
    }
    @Test fun leavingDirtyProjectPromptsBeforeDiscarding() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup(); val activity = controller.get(); seed(activity)
        click(activity, "Code (SERVER_JS)"); views(activity.window.decorView).filterIsInstance<EditText>().single().setText("não perder")
        click(activity, "Voltar aos arquivos"); click(activity, "Voltar aos projetos")
        val dialog = ShadowAlertDialog.getLatestAlertDialog()
        assertTrue(dialog.isShowing)
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).performClick()
        click(activity, "Code (SERVER_JS)")
        assertEquals("não perder", views(activity.window.decorView).filterIsInstance<EditText>().single().text.toString())
        controller.pause().stop().destroy(); java.io.File(activity.filesDir, "editor-recovery.json").delete()
    }
}
