package com.voca.mobile.ui

import android.content.Context
import android.speech.tts.TextToSpeech
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voca.mobile.data.SessionManager
import com.voca.mobile.data.TokenStore
import com.voca.mobile.data.VocaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class AppViewModel(
    private val appContext: Context,
    val repo: VocaRepository,
) : ViewModel() {

    private val tokenStore = TokenStore(appContext)

    private val _authed = MutableStateFlow<Boolean?>(null)
    /** null = still loading persisted token, true/false = resolved. */
    val authed: StateFlow<Boolean?> = _authed.asStateFlow()

    private var tts: TextToSpeech? = null

    init {
        SessionManager.onUnauthorized = { logoutLocal() }
        viewModelScope.launch {
            val saved = tokenStore.token.first()
            SessionManager.token = saved
            _authed.value = !saved.isNullOrEmpty()
        }
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                tts?.setSpeechRate(0.92f)
            }
        }
    }

    fun onAuthSuccess(token: String) {
        SessionManager.token = token
        viewModelScope.launch { tokenStore.save(token) }
        _authed.value = true
    }

    fun logout() {
        SessionManager.token = null
        viewModelScope.launch { tokenStore.clear() }
        _authed.value = false
    }

    private fun logoutLocal() {
        SessionManager.token = null
        viewModelScope.launch { tokenStore.clear() }
        _authed.value = false
    }

    fun speak(text: String?) {
        val value = text?.trim().orEmpty()
        if (value.isEmpty()) return
        tts?.speak(value, TextToSpeech.QUEUE_FLUSH, null, "voca-word")
    }

    override fun onCleared() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        SessionManager.onUnauthorized = null
        super.onCleared()
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    AppViewModel(context, VocaRepository()) as T
            }
    }
}
