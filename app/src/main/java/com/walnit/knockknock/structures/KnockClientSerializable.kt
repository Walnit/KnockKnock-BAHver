package com.walnit.knockknock.structures

import org.whispersystems.libsignal.IdentityKey
import org.whispersystems.libsignal.ecc.ECPublicKey

@kotlinx.serialization.Serializable
data class KnockClientSerializable(
    val name : String,
    val registrationID : Int,
    val deviceID : Int,
    val preKeyID : Int,
    val preKeyPublic : ByteArray,
    val signedPreKeyId : Int,
    val signedPreKeyPublic : ByteArray,
    val signedPreKeySignature : ByteArray,
    val identityKey : ByteArray
) {


}