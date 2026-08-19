package com.signalgate.pulse.data.security

/**
 * Public trust anchor for the FTC-via-owned-mirror source only.
 *
 * The corresponding private key remains exclusively in the mirror repository's
 * GitHub Actions secret and must never be present in the Android project.
 */
internal object SourceAuthenticityTrustAnchor {
    const val FTC_DNC_PUBLIC_KEY_DER_BASE64 =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEm8p1EIMaaQv23v89evaic5Kf4Bh7nYRYodgss7iVVg5XcdGHbODvL9xNvXsVhjOIJdnqCyuzvZzr0+X28f8HNg=="
}
