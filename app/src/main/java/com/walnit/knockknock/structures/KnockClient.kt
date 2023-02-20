package com.walnit.knockknock.structures

import android.content.Context
import com.walnit.knockknock.signal.KnockPreKeyStore
import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.ecc.Curve
import org.whispersystems.libsignal.ecc.ECPublicKey
import org.whispersystems.libsignal.state.PreKeyBundle
import org.whispersystems.libsignal.state.PreKeyRecord
import org.whispersystems.libsignal.state.SignedPreKeyRecord

class KnockClient(
    val name : String,
    val registrationID : Int,
    val deviceID : Int,
    val preKeyID : Int,
    val preKeyPublic : ECPublicKey,
    val signedPreKeyId : Int,
    val signedPreKeyPublic : ECPublicKey,
    val signedPreKeySignature : ByteArray,
    val identityKey : IdentityKey
) {
    companion object {
        fun newClient(context: Context, name: String, registrationID: Int, deviceID: Int, signedPreKeyRecord: SignedPreKeyRecord, identityKey: IdentityKey, preKeyRecord: PreKeyRecord? = null) : KnockClient {
            if (preKeyRecord == null) {
                val preKeyStore = KnockPreKeyStore(context)
                val preKeyID = preKeyStore.getNewPreKeyID()
                return KnockClient(name, registrationID, deviceID, preKeyID, preKeyStore.loadPreKey(preKeyID).keyPair.publicKey,
                signedPreKeyRecord.id, signedPreKeyRecord.keyPair.publicKey, signedPreKeyRecord.signature, identityKey)
            } else {
                return KnockClient(name, registrationID, deviceID, preKeyRecord.id, preKeyRecord.keyPair.publicKey,
                    signedPreKeyRecord.id, signedPreKeyRecord.keyPair.publicKey, signedPreKeyRecord.signature, identityKey)
            }
        }
        fun fromSerialized(serializedClient: KnockClientSerializable) : KnockClient {
            return KnockClient(serializedClient.name, serializedClient.registrationID, serializedClient.deviceID,
                serializedClient.preKeyID, Curve.decodePoint(serializedClient.preKeyPublic, 0),
                serializedClient.signedPreKeyId, Curve.decodePoint(serializedClient.signedPreKeyPublic, 0),
                serializedClient.signedPreKeySignature,
                IdentityKey(serializedClient.identityKey, 0))
        }
    }

    fun getPreKeyBundle() : PreKeyBundle {
        return PreKeyBundle(registrationID, deviceID, preKeyID, preKeyPublic, signedPreKeyId, signedPreKeyPublic, signedPreKeySignature, identityKey)
    }

    fun toSerializableClient() : KnockClientSerializable {
        return KnockClientSerializable(name, registrationID, deviceID, preKeyID, preKeyPublic.serialize(), signedPreKeyId, signedPreKeyPublic.serialize(), signedPreKeySignature, identityKey.serialize())
    }
}