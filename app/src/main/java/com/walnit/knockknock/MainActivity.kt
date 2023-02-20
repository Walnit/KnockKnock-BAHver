package com.walnit.knockknock

import android.content.SharedPreferences
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.navigation.ui.AppBarConfiguration
import androidx.preference.PreferenceManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.protobuf.ByteString
import com.walnit.knockknock.databinding.ActivityMainBinding
import com.walnit.knockknock.networking.GetBundle
import com.walnit.knockknock.networking.GetMessagesForClient
import com.walnit.knockknock.networking.SendBundle
import com.walnit.knockknock.networking.SendMessagesToClient
import com.walnit.knockknock.signal.KnockIdentityKeyStore
import com.walnit.knockknock.signal.KnockPreKeyStore
import com.walnit.knockknock.signal.KnockSignedPreKeyStore
import com.walnit.knockknock.structures.KnockClient
import com.walnit.knockknock.structures.KnockClientSerializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.ResponseBody
import org.json.JSONArray
import org.whispersystems.libsignal.SessionBuilder
import org.whispersystems.libsignal.SessionCipher
import org.whispersystems.libsignal.SignalProtocolAddress
import org.whispersystems.libsignal.protocol.PreKeySignalMessage
import org.whispersystems.libsignal.protocol.SignalMessage
import org.whispersystems.libsignal.state.*
import org.whispersystems.libsignal.state.impl.InMemorySessionStore
import org.whispersystems.libsignal.util.KeyHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var securePreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {

        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

        securePreferences = EncryptedSharedPreferences.create(
            this,
            "secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)

        if (sharedPreferences.getBoolean("firstTime", true)) {

            // Generate required information

            val identityKeyPair = KeyHelper.generateIdentityKeyPair()
            val registrationId = KeyHelper.generateRegistrationId(false)
            val preKeys = KeyHelper.generatePreKeys(1, 100)
            val signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, 1)

            // Save required information to secure preferences

            securePreferences.edit()
                .putString("IKP", Base64.encodeToString(identityKeyPair.serialize(), Base64.NO_WRAP))
                .putInt("RID", registrationId)
                .apply() // Store IdentityKeyPair and RegistrationID

            val preKeyStore = KnockPreKeyStore(this)
            preKeyStore.setMaxPreKeyID(100)
            preKeys.forEach {preKey ->
                preKeyStore.storePreKey(preKey.id, preKey)
            } // Store PreKeys

            KnockSignedPreKeyStore(this).storeSignedPreKey(signedPreKey.id, signedPreKey)


        }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val sessionStore = InMemorySessionStore()
        val preKeyStore = KnockPreKeyStore(this)
        val signedPreKeyStore = KnockSignedPreKeyStore(this)
        val identityStore = KnockIdentityKeyStore(this)

//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        appBarConfiguration = AppBarConfiguration(navController.graph)
//        setupActionBarWithNavController(navController, appBarConfiguration)

        val usernameInput : TextInputEditText = findViewById(R.id.username_input)
        val targetInput : TextInputEditText = findViewById(R.id.target_input)
        val messageInput : TextInputEditText = findViewById(R.id.message_input)

        findViewById<MaterialButton>(R.id.sync_button).setOnClickListener {
            if (usernameInput.text.isNullOrBlank()) {
                usernameInput.error = "Username must not be blank!"
            } else if (targetInput.text.isNullOrBlank()) {
                targetInput.error = "Who are you even going to message?"
            } else {
                Executors.newSingleThreadExecutor().execute {

                    val localUser : KnockClient = KnockClient.newClient(this, usernameInput.text.toString(),
                        identityStore.localRegistrationId, 1, signedPreKeyStore.loadSignedPreKeys()[0],
                        identityStore.identityKeyPair.publicKey)

                    val retrofit = Retrofit.Builder().baseUrl("http://192.168.1.126:8907").build()

                    val sendBundle : SendBundle = retrofit.create(SendBundle::class.java)
                    val callFirst : Call<ResponseBody> = sendBundle.sendBundle(localUser.name, Base64.encodeToString(Json.encodeToString(localUser.toSerializableClient()).toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP))
                    callFirst.execute()


                    val getMessages : GetMessagesForClient = retrofit.create(GetMessagesForClient::class.java)
                    val call : Call<ResponseBody> = getMessages.getMessageList(usernameInput.text.toString(), targetInput.text.toString())
                    call.enqueue(object : Callback<ResponseBody?> {
                        override fun onResponse(
                            call: Call<ResponseBody?>?,
                            response: Response<ResponseBody?>
                        ) {
                            if (response.code() == 200) {
                                val messagesJson = JSONArray(response.body()?.string())

                                val getBundle: GetBundle = retrofit.create(GetBundle::class.java)
                                val call: Call<ResponseBody> =
                                    getBundle.getBundle(targetInput.text.toString())
                                Executors.newSingleThreadExecutor().execute {
                                    val response = call.execute()
                                    response.body()?.string()
                                        ?.let { it1 ->
                                            run {
                                                val targetClient = KnockClient.fromSerialized(
                                                    Json.decodeFromString<KnockClientSerializable>(
                                                        (String(Base64.decode(it1, Base64.NO_WRAP or Base64.URL_SAFE)))
                                                    )
                                                )

                                                val localUser: KnockClient = KnockClient.newClient(
                                                    applicationContext,
                                                    usernameInput.text.toString(),
                                                    identityStore.localRegistrationId,
                                                    1,
                                                    signedPreKeyStore.loadSignedPreKeys()[0],
                                                    identityStore.identityKeyPair.publicKey
                                                )

                                                val sessionBuilder = SessionBuilder(
                                                    sessionStore,
                                                    preKeyStore,
                                                    signedPreKeyStore,
                                                    identityStore,
                                                    SignalProtocolAddress(
                                                        targetClient.name,
                                                        targetClient.deviceID
                                                    )
                                                )

                                                sessionBuilder.process(targetClient.getPreKeyBundle())
                                                val sessionCipher = SessionCipher(
                                                    sessionStore,
                                                    preKeyStore,
                                                    signedPreKeyStore,
                                                    identityStore,
                                                    SignalProtocolAddress(
                                                        targetClient.name,
                                                        targetClient.deviceID
                                                    )
                                                )

                                                for (i in 0 until messagesJson.length()) {
                                                    runOnUiThread {
                                                        Toast.makeText(applicationContext,
                                                            String(sessionCipher.decrypt(
                                                                PreKeySignalMessage(
                                                                    Base64.decode(
                                                                        messagesJson.getString(i),
                                                                        Base64.NO_WRAP or Base64.URL_SAFE
                                                                    )
                                                                )
                                                            )), Toast.LENGTH_LONG
                                                        ).show()
                                                    }
                                                }
                                            }
                                        }
                                }
                            }
                        }


                        override fun onFailure(call: Call<ResponseBody?>, t: Throwable) {
                            Toast.makeText(applicationContext, "Failure!", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
        }

        binding.fab.setOnClickListener {

            val retrofit = Retrofit.Builder().baseUrl("http://192.168.1.126:8907")
                .build()
            val getBundle : GetBundle = retrofit.create(GetBundle::class.java)
            val call : Call<ResponseBody> = getBundle.getBundle(targetInput.text.toString())
            Executors.newSingleThreadExecutor().execute {
                val response = call.execute()
                val body = response.body()
                if (body != null) {
                    val targetClient = KnockClient.fromSerialized(
                        Json.decodeFromString<KnockClientSerializable>(String(Base64.decode(body.string(), Base64.NO_WRAP or Base64.URL_SAFE)))
                    )

                    val localUser : KnockClient = KnockClient.newClient(this, usernameInput.text.toString(),
                        identityStore.localRegistrationId, 1, signedPreKeyStore.loadSignedPreKeys()[0],
                        identityStore.identityKeyPair.publicKey)

                    val sessionBuilder = SessionBuilder(
                        sessionStore, preKeyStore, signedPreKeyStore,
                        identityStore, SignalProtocolAddress(targetClient.name, targetClient.deviceID)
                    )

                    sessionBuilder.process(targetClient.getPreKeyBundle())
                    val sessionCipher = SessionCipher(sessionStore, preKeyStore, signedPreKeyStore, identityStore, SignalProtocolAddress(targetClient.name, targetClient.deviceID))
                    val cipherMessage = PreKeySignalMessage(sessionCipher.encrypt(messageInput.text.toString().toByteArray()).serialize())
                    val sendMessage: SendMessagesToClient = retrofit.create(SendMessagesToClient::class.java)
                    val callMsg = sendMessage.sendMessage(targetClient.name, localUser.name, Base64.encodeToString(
                        cipherMessage.serialize(), Base64.NO_WRAP or Base64.URL_SAFE)
                    )
                    println(callMsg.execute().message())

                }
            }

//

//
//
//            sessionBuilder.process()


        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

//    override fun onSupportNavigateUp(): Boolean {
//        val navController = findNavController(R.id.nav_host_fragment_content_main)
//        return navController.navigateUp(appBarConfiguration)
//                || super.onSupportNavigateUp()
//    }
}