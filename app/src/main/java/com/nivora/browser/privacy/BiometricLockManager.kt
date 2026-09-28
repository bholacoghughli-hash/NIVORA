package com.nivora.browser.privacy

import android.app.KeyguardManager
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

class BiometricLockManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("nivora_security_prefs", Context.MODE_PRIVATE)

    private val keyguardManager =
        context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    companion object {
        private const val KEY_ALIAS = "NivoraMasterKey"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val PREF_LOCK_ENABLED = "pref_app_lock_enabled"
        private const val PREF_LOCK_ON_STARTUP = "pref_lock_on_startup"
        private const val PREF_LOCK_TIMEOUT_MINS = "pref_lock_timeout_mins"
        private const val PREF_LAST_UNLOCKED = "pref_last_unlocked_ts"
    }

    init {
        ensureKeystoreKey()
        checkInitialLockState()
    }

    var isAppLockEnabled: Boolean
        get() = prefs.getBoolean(PREF_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(PREF_LOCK_ENABLED, value).apply()

    var isLockOnStartupEnabled: Boolean
        get() = prefs.getBoolean(PREF_LOCK_ON_STARTUP, true)
        set(value) = prefs.edit().putBoolean(PREF_LOCK_ON_STARTUP, value).apply()

    var lockTimeoutMinutes: Int
        get() = prefs.getInt(PREF_LOCK_TIMEOUT_MINS, 5)
        set(value) = prefs.edit().putInt(PREF_LOCK_TIMEOUT_MINS, value).apply()

    fun isDeviceSecurityAvailable(): Boolean {
        return keyguardManager?.isDeviceSecure ?: false
    }

    private fun checkInitialLockState() {
        if (isAppLockEnabled) {
            val lastUnlocked = prefs.getLong(PREF_LAST_UNLOCKED, 0L)
            val now = System.currentTimeMillis()
            val timeoutMillis = lockTimeoutMinutes * 60 * 1000L
            if (isLockOnStartupEnabled || (now - lastUnlocked > timeoutMillis)) {
                _isLocked.value = true
            }
        }
    }

    fun unlockApp() {
        prefs.edit().putLong(PREF_LAST_UNLOCKED, System.currentTimeMillis()).apply()
        _isLocked.value = false
    }

    fun lockApp() {
        if (isAppLockEnabled) {
            _isLocked.value = true
        }
    }

    /**
     * Initializes hardware-backed Master Key in Android Keystore
     */
    private fun ensureKeystoreKey() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val builder = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                ).apply {
                    setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    setRandomizedEncryptionRequired(true)
                }
                keyGenerator.init(builder.build())
                keyGenerator.generateKey()
            }
        } catch (_: Exception) {
            // Handled gracefully on systems where Keystore is restricted
        }
    }

    /**
     * Returns an initialized cipher for encrypting sensitive vault credentials
     */
    fun getVaultCipher(): Cipher? {
        return try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            val key = keyStore.getKey(KEY_ALIAS, null) as? SecretKey ?: return null
            Cipher.getInstance("AES/GCM/NoPadding").apply {
                init(Cipher.ENCRYPT_MODE, key)
            }
        } catch (_: Exception) {
            null
        }
    }
}
