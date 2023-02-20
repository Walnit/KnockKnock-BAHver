package com.walnit.knockknock.signal

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.whispersystems.libsignal.state.SignedPreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyStore

class KnockSignedPreKeyStore(context: Context) : SignedPreKeyStore {

    // Get SignedPreKey store
    private val secureSignedPreKeyStore = EncryptedSharedPreferences.create(
        context,
        "secure_signedPkStore",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun loadSignedPreKey(signedPreKeyId: Int): SignedPreKeyRecord {
        return SignedPreKeyRecord(Base64.decode(
            secureSignedPreKeyStore.getString(signedPreKeyId.toString(), null), Base64.NO_WRAP))
    }

    override fun loadSignedPreKeys(): MutableList<SignedPreKeyRecord> {
        val signedPreKeyRecords = mutableListOf<SignedPreKeyRecord>()
        secureSignedPreKeyStore.all.values.forEach { b64SignedPreKeyRecord ->
            signedPreKeyRecords.add(SignedPreKeyRecord(Base64.decode(
                b64SignedPreKeyRecord as String, Base64.NO_WRAP)))
        }
        return signedPreKeyRecords
    }

    override fun storeSignedPreKey(signedPreKeyId: Int, record: SignedPreKeyRecord?) {
        if (record != null) {
            secureSignedPreKeyStore.edit()
                .putString(signedPreKeyId.toString(), Base64.encodeToString(record.serialize(), Base64.NO_WRAP))
                .apply()
        } else {
            removeSignedPreKey(signedPreKeyId)
        }
    }

    override fun containsSignedPreKey(signedPreKeyId: Int): Boolean {
         return secureSignedPreKeyStore.contains(signedPreKeyId.toString())
    }

    override fun removeSignedPreKey(signedPreKeyId: Int) {
        secureSignedPreKeyStore.edit().remove(signedPreKeyId.toString()).apply()
    }
}