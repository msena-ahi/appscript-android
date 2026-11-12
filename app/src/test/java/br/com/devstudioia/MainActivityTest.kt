package br.com.devstudioia

import android.app.Activity
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
        assertTrue(all.filterIsInstance<Button>().any { it.text == "Entrar com Google" && it.isEnabled })
        assertFalse(all.filterIsInstance<Button>().single { it.text == "Meus projetos Apps Script" }.isEnabled)
        assertFalse(all.filterIsInstance<Button>().single { it.text == "Sair da conta" }.isEnabled)
        controller.pause().stop().destroy()
    }
    @Test fun connectOpensNativeChooserAndCancelRestoresUi() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        val activity = controller.get()
        views(activity.window.decorView).filterIsInstance<Button>().single { it.text == "Entrar com Google" }.performClick()
        val started = shadowOf(activity).nextStartedActivityForResult
        assertNotNull(started)
        assertNull(started.intent.data) // No browser redirect or client ID is used.
        activity.activityResultRegistry.dispatchResult(started.requestCode, Activity.RESULT_CANCELED, null)
        val all = views(activity.window.decorView)
        assertTrue(all.filterIsInstance<TextView>().any { it.text.toString().contains("Seleção de conta cancelada") })
        assertTrue(all.filterIsInstance<Button>().single { it.text == "Entrar com Google" }.isEnabled)
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
}
