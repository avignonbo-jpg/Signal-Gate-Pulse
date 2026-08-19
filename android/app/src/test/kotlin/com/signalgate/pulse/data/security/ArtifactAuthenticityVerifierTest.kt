package com.signalgate.pulse.data.security

import android.util.Base64
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.security.MessageDigest
import java.security.Signature
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtifactAuthenticityVerifierTest {
    private val now = Instant.parse("2026-08-19T20:00:00Z")
    private val keyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()
    private val publicKeyB64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)

    @Test
    fun matchingManifestAndSignature_areVerified() {
        val payload = "snapshot".toByteArray()
        val manifest = manifestFor(payload)

        assertEquals(
            ArtifactAuthenticityVerifier.Result.Verified,
            ArtifactAuthenticityVerifier.verifyForTest(payload, manifest, publicKeyB64, now)
        )
    }

    @Test
    fun tamperedPayload_isRejectedBeforeActivation() {
        val payload = "snapshot".toByteArray()
        val manifest = manifestFor(payload)

        assertEquals(
            ArtifactAuthenticityVerifier.Result.Failed("Payload hash mismatch"),
            ArtifactAuthenticityVerifier.verifyForTest("tampered".toByteArray(), manifest, publicKeyB64, now)
        )
    }

    @Test
    fun unsupportedAlgorithm_isRejected() {
        val payload = "snapshot".toByteArray()
        val manifest = manifestFor(payload).copy(algorithm = "SHA256withRSA")

        assertEquals(
            ArtifactAuthenticityVerifier.Result.Failed("Unsupported signature algorithm: SHA256withRSA"),
            ArtifactAuthenticityVerifier.verifyForTest(payload, manifest, publicKeyB64, now)
        )
    }

    @Test
    fun staleAndFutureManifests_areRejected() {
        val payload = "snapshot".toByteArray()
        val stale = manifestFor(payload, now.minus(8, ChronoUnit.DAYS))
        val future = manifestFor(payload, now.plus(6, ChronoUnit.MINUTES))

        assertEquals(
            ArtifactAuthenticityVerifier.Result.Failed("Manifest is stale"),
            ArtifactAuthenticityVerifier.verifyForTest(payload, stale, publicKeyB64, now)
        )
        assertEquals(
            ArtifactAuthenticityVerifier.Result.Failed("Manifest is dated in the future"),
            ArtifactAuthenticityVerifier.verifyForTest(payload, future, publicKeyB64, now)
        )
    }

    @Test
    fun malformedManifest_isRejected() {
        assertEquals(
            ArtifactAuthenticityVerifier.ResultOrManifest.Failure("Malformed authenticity manifest"),
            ArtifactAuthenticityVerifier.parseManifest("not-json")
        )
    }

    private fun manifestFor(
        payload: ByteArray,
        signedAt: Instant = now
    ): ArtifactAuthenticityVerifier.Manifest {
        val signature = Signature.getInstance("SHA256withECDSA")
        signature.initSign(keyPair.private)
        signature.update(payload)
        return ArtifactAuthenticityVerifier.Manifest(
            algorithm = "SHA256withECDSA",
            sha256Hex = MessageDigest.getInstance("SHA-256")
                .digest(payload)
                .joinToString("") { byte -> "%02x".format(byte) },
            signatureB64 = Base64.encodeToString(signature.sign(), Base64.NO_WRAP),
            signedAt = signedAt.toString()
        )
    }
}
