package br.com.devstudioia

import android.app.Activity
import android.os.Bundle
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationException

class AuthResultActivity: Activity() {
 override fun onCreate(savedInstanceState:Bundle?) {super.onCreate(savedInstanceState)
  val response=AuthorizationResponse.fromIntent(intent)
  val error=AuthorizationException.fromIntent(intent)
  AuthSession.response=response
  AuthSession.error=error
  startActivity(android.content.Intent(this,MainActivity::class.java).addFlags(android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP))
  finish()
 }
}
object AuthSession { var response:AuthorizationResponse?=null; var error:AuthorizationException?=null }
