package ir.sysfail.chatguard.core.preferences.implementation

import android.util.Base64
import ir.sysfail.chatguard.core.preferences.abstraction.PreferenceSerializer

object Base64ByteArraySerializer : PreferenceSerializer<ByteArray> {
    override fun serialize(value: ByteArray): String =
        Base64.encodeToString(value, Base64.DEFAULT)

    override fun deserialize(data: String): ByteArray =
        Base64.decode(data, Base64.DEFAULT)
}