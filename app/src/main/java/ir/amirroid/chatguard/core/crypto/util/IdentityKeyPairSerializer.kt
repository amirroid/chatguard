package ir.amirroid.chatguard.core.crypto.util

import ir.amirroid.chatguard.core.crypto.models.IdentityKeyPair
import ir.amirroid.chatguard.core.crypto.models.PrivateKey
import ir.amirroid.chatguard.core.crypto.models.PublicKey
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream

object IdentityKeyPairSerializer {

    fun serialize(pair: IdentityKeyPair): ByteArray {
        val output = ByteArrayOutputStream()
        val data = DataOutputStream(output)

        writeString(data, pair.privateKey.algorithm)
        writeByteArray(data, pair.privateKey.encoded)

        writeString(data, pair.publicKey.algorithm)
        writeByteArray(data, pair.publicKey.encoded)

        return output.toByteArray()
    }

    fun deserialize(bytes: ByteArray): IdentityKeyPair {
        val input = ByteArrayInputStream(bytes)
        val data = DataInputStream(input)

        val privateAlg = readString(data)
        val privateEncoded = readByteArray(data)

        val publicAlg = readString(data)
        val publicEncoded = readByteArray(data)

        return IdentityKeyPair(
            privateKey = PrivateKey(
                encoded = privateEncoded,
                algorithm = privateAlg
            ),
            publicKey = PublicKey(
                encoded = publicEncoded,
                algorithm = publicAlg
            )
        )
    }


    private fun writeString(out: DataOutputStream, value: String) {
        val bytes = value.toByteArray(Charsets.UTF_8)
        out.writeInt(bytes.size)
        out.write(bytes)
    }

    private fun readString(input: DataInputStream): String {
        val size = input.readInt()
        val bytes = ByteArray(size)
        input.readFully(bytes)
        return String(bytes, Charsets.UTF_8)
    }

    private fun writeByteArray(out: DataOutputStream, value: ByteArray) {
        out.writeInt(value.size)
        out.write(value)
    }

    private fun readByteArray(input: DataInputStream): ByteArray {
        val size = input.readInt()
        val bytes = ByteArray(size)
        input.readFully(bytes)
        return bytes
    }
}
