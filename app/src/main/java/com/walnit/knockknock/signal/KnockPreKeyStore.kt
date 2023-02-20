package com.walnit.knockknock.signal

import android.annotation.SuppressLint
import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.PreKeyStore
import org.whispersystems.libsignal.util.KeyHelper


class KnockPreKeyStore(context: Context)  : PreKeyStore {

    // Get PreKey store
    private val securePreKeyStore = EncryptedSharedPreferences.create(
        context,
        "secure_pkStore",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getNewPreKeyID() : Int {
        val currentPreKeyID = securePreKeyStore.getInt("currentPreKeyID", 0)
        val maxPreKeyID = securePreKeyStore.getInt("maxPreKeyID", -1)
        if (currentPreKeyID > maxPreKeyID) {
            KeyHelper.generatePreKeys(currentPreKeyID+1, maxPreKeyID+100).forEach { preKey ->
                storePreKey(preKey.id, preKey)
            }
        }

        securePreKeyStore.edit().putInt("currentPreKeyID", currentPreKeyID+1).apply()
        return currentPreKeyID+1
    }

    fun setMaxPreKeyID(maxPreKeyID: Int) {
        securePreKeyStore.edit().putInt("maxPreKeyID", maxPreKeyID).apply()
    }

    override fun loadPreKey(preKeyId: Int): PreKeyRecord {
        return PreKeyRecord(Base64.decode(
            securePreKeyStore.getString(preKeyId.toString(), null), Base64.NO_WRAP))
    }

    @SuppressLint("ApplySharedPref")
    override fun storePreKey(preKeyId: Int, record: PreKeyRecord?) {
        if (record != null) {
            if (preKeyId > securePreKeyStore.getInt("maxPreKeyID", -1)) {
                securePreKeyStore.edit().putInt("maxPreKeyID", preKeyId).commit() // Commit needed to make sure we don't skip pre keys
            }
            securePreKeyStore.edit()
                .putString(preKeyId.toString(), Base64.encodeToString(record.serialize(), Base64.NO_WRAP))
                .apply()
        } else {
            removePreKey(preKeyId)
        }
    }

    override fun containsPreKey(preKeyId: Int): Boolean {
        return securePreKeyStore.contains(preKeyId.toString())
    }

    override fun removePreKey(preKeyId: Int) {
        securePreKeyStore.edit().remove(preKeyId.toString()).apply()
    }
}