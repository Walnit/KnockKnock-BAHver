package com.walnit.knockknock.signal

import android.content.Context
import android.util.Base64
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.state.SessionRecord
import org.whispersystems.libsignal.state.SessionState
import org.whispersystems.libsignal.state.SessionStore
import org.whispersystems.libsignal.state.StorageProtos

class KnockSessionStore(context: Context) : SessionStore {

    // Get Session store
    private val secureSessionStore = EncryptedSharedPreferences.create(
        context,
        "secure_sessionStore",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    override fun loadSession(address: SignalProtocolAddress?): SessionRecord {
        var sessionRecord: SessionRecord? = null
        if (address != null) {
            if (containsSession(address)) {
                sessionRecord = SessionRecord(Base64.decode(
                    secureSessionStore.getString(address.name+","+address.deviceId.toString(), null), Base64.NO_WRAP))
            }
        }

        return sessionRecord ?: SessionRecord(SessionState(StorageProtos.SessionStructure.getDefaultInstance()))

    }

    override fun getSubDeviceSessions(name: String?): MutableList<Int> {
        val subDeviceSessions = mutableListOf<Int>()
        secureSessionStore.all.keys.forEach { identifier ->
            if (identifier.split(",")[0] == name) {
                subDeviceSessions.add(identifier.split(",")[1].toInt())
            }
        }
        return subDeviceSessions
    }

    override fun storeSession(address: SignalProtocolAddress?, record: SessionRecord?) {
        if (address != null) {
            if (record != null) {
                secureSessionStore.edit()
                    .putString(address.name+","+address.deviceId.toString(), Base64.encodeToString(record.serialize(), Base64.NO_WRAP))
                    .apply()
            } else {
                deleteSession(address)
            }
        }
    }

    override fun containsSession(address: SignalProtocolAddress?): Boolean {
        return if (address != null) {
            secureSessionStore.contains(address.name+","+address.deviceId.toString())
        } else false
    }

    override fun deleteSession(address: SignalProtocolAddress?) {
        if (address != null) {
            secureSessionStore.edit().remove(address.name+","+address.deviceId.toString()).apply()
        }
    }

    override fun deleteAllSessions(name: String?) {
        val storeEditor = secureSessionStore.edit()
        secureSessionStore.all.keys.forEach { identifier ->
            if (identifier.split(",")[0] == name) {
                storeEditor.remove(identifier)
            }
        }
        storeEditor.apply()
    }
}