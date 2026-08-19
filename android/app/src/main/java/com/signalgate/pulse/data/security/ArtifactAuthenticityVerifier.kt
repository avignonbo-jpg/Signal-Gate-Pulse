package com.signalgate.pulse.data.security

import android.util.Base64
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.json.JSONObject

/**
 * Verifies a detached ECDSA P-256 signature over externally sourced security
 * data, per Architecture Contract §3.3. HTTPS is transport security only;
 * this establishes artifact authenticity for the first-party FTC mirror.
 *
 * FCC remains deliberately HTTPS-transport-only because it is third-party
 * infrastructure outside the project's signing pipeline.
 */
object ArtifactAuthenticityVerifier {
    private const val ALGORITHM = "SHA256withECDSA"
    private const val MAX_MANIFEST_AGE_DAYS = 7L
    private const val MAX_FUTURE_SKEW_SECONDS = 300L
    private val HASH_PATTERN = Regex("[0-9a-f]{64}")

    data class Manifest(
        val algorithm: String,
        val sha256Hex: String,
        val signatureB64: String,
        val signedAt: String
    )

    sealed interface Result {
        data object Verified : Result
        data class Failed(val reason: String) : Result
    }

    fun parseManifest(manifestText: String): ResultOrManifest {
        return try {
            val json = JSONObject(manifestText)
            ResultOrManifest.ManifestValue(
                Manifest(
                    algorithm = json.getString("algorithm"),
                    sha256Hex = json.getString("sha256Hex").lowercase(),
                    signatureB64 = json.getString("signatureB64"),
                    signedAt = json.getString("signedAt")
                )
            )
        } catch (_: Exception) {
            ResultOrManifest.Failure("Malformed authenticity manifest")
        }
    }

    fun verify(
        payloadBytes: ByteArray,
        manifest: Manifest,
        now: Instant = Instant.now()
    ): Result = verifyInternal(
        payloadBytes = payloadBytes,
        manifest = manifest,
        now = now,
        publicKeyDerBase64 = SourceAuthenticityTrustAnchor.FTC_DNC_PUBLIC_KEY_DER_BASE64
    )

    internal fun verifyForTest(
        payloadBytes: ByteArray,
        manifest: Manifest,
        publicKeyDerBase64: String,
        now: Instant = Instant.now()
    ): Result = verifyInternal(payloadBytes, manifest, now, publicKeyDerBase64)

    private fun verifyInternal(
        payloadBytes: ByteArray,
        manifest: Manifest,
        now: Instant,
        publicKeyDerBase64: String
    ): Result {
        if (manifest.algorithm != ALGORITHM) {
            return Result.Failed("Unsupported signature algorithm: ${manifest.algorithm}")
        }
        if (!manifest.sha256Hex.matches(HASH_PATTERN)) {
            return Result.Failed("Malformed payload hash")
        }

        val signedAt = try {
            Instant.parse(manifest.signedAt)
        } catch (_: Exception) {
            return Result.Failed("Malformed signedAt timestamp")
        }
        if (signedAt.isAfter(now.plusSeconds(MAX_FUTURE_SKEW_SECONDS))) {
            return Result.Failed("Manifest is dated in the future")
        }
        if (signedAt.isBefore(now.minus(MAX_MANIFEST_AGE_DAYS, ChronoUnit.DAYS))) {
            return Result.Failed("Manifest is stale")
        }

        val actualHash = MessageDigest.getInstance("SHA-256")
            .digest(payloadBytes)
            .joinToString("") { byte -> "%02x".format(byte) }
        if (actualHash != manifest.sha256Hex) {
            return Result.Failed("Payload hash mismatch")
        }

        return try {
            val publicKeyBytes = Base64.decode(
                publicKeyDerBase64,
                Base64.DEFAULT
            )
            val publicKey = KeyFactory.getInstance("EC")
                .generatePublic(X509EncodedKeySpec(publicKeyBytes))
            val signatureBytes = Base64.decode(manifest.signatureB64, Base64.DEFAULT)
            if (signatureBytes.isEmpty()) {
                return Result.Failed("Missing detached signature")
            }
            val verifier = Signature.getInstance(ALGORITHM)
            verifier.initVerify(publicKey)
            verifier.update(payloadBytes)
            if (verifier.verify(signatureBytes)) {
                Result.Verified
            } else {
                Result.Failed("Signature verification failed")
            }
        } catch (_: Exception) {
            Result.Failed("Detached signature verification failed")
        }
    }

    sealed interface ResultOrManifest {
        data class ManifestValue(val manifest: Manifest) : ResultOrManifest
        data class Failure(val reason: String) : ResultOrManifest
    }
}
