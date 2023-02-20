package com.walnit.knockknock.signal

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.IdentityKeyPair
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.state.IdentityKeyStore

class KnockIdentityKeyStore(context: Context) : IdentityKeyStore {

    // Get Identity Key store
    private val secureIdentityKeyStore = EncryptedSharedPreferences.create(
        context,
        "secure_identityKeyStore",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    // Get secure prefs for Registration ID and IdentityKeyPair
    private val securePreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    override fun getIdentityKeyPair(): IdentityKeyPair {
        return IdentityKeyPair(Base64.decode(securePreferences.getString("IKP", null), Base64.NO_WRAP))
    }

    override fun getLocalRegistrationId(): Int {
        return securePreferences.getInt("RID", -1)
    }

    override fun saveIdentity(address: SignalProtocolAddress?, identityKey: IdentityKey?): Boolean {
        if (address != null) {
            val isReplacingPreviousKey = secureIdentityKeyStore.contains(address.name+","+address.deviceId.toString())
            if (identityKey != null) {
                secureIdentityKeyStore.edit()
                    .putString(address.name+","+address.deviceId.toString(), Base64.encodeToString(identityKey.serialize(), Base64.NO_WRAP))
                    .apply()
            } else {
                secureIdentityKeyStore.edit().remove(address.name+","+address.deviceId.toString()).apply()
            }
            return isReplacingPreviousKey
        }
        return false
    }

    override fun isTrustedIdentity(
        address: SignalProtocolAddress?,
        identityKey: IdentityKey?,
        direction: IdentityKeyStore.Direction?
    ): Boolean {
        if (address != null) {
            if (!secureIdentityKeyStore.contains(address.name+","+address.deviceId.toString())) return true
            else {
                if (identityKey != null) {
                    if (getIdentity(address)?.fingerprint == identityKey.fingerprint) {
                        return true
                    }
                }
            }
        }
        return false
    }

    override fun getIdentity(address: SignalProtocolAddress?): IdentityKey? {
        if (address != null) {
            return IdentityKey(Base64.decode(
                secureIdentityKeyStore.getString(address.name+","+address.deviceId.toString(), null), Base64.NO_WRAP),0)
        }
        return null
    }
}