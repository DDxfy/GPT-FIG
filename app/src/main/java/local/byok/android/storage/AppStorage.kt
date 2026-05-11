package local.byok.android.storage

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import local.byok.android.model.AppState
import java.io.File

class AppStorage(
    private val context: Context,
    private val json: Json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    private val file = File(context.filesDir, "state.json")

    fun load(): AppState = runCatching {
        if (!file.exists()) AppState() else json.decodeFromString(file.readText())
    }.getOrElse { AppState() }

    fun save(state: AppState) {
        file.writeText(json.encodeToString(state))
    }

    fun clear() {
        if (file.exists()) file.delete()
    }
}

interface KeyStore {
    fun readApiKey(): String?
    fun saveApiKey(value: String)
    fun deleteApiKey()
}

class SecureKeyStore(context: Context) : KeyStore {
    private val prefs: SharedPreferences

    init {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        prefs = EncryptedSharedPreferences.create(
            context,
            "byok_secure",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun readApiKey(): String? = prefs.getString("api_key", null)?.takeIf { it.isNotBlank() }
    override fun saveApiKey(value: String) = prefs.edit().putString("api_key", value.trim()).apply()
    override fun deleteApiKey() = prefs.edit().remove("api_key").apply()
}
