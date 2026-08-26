package org.multipaz.samples.wallet.cmp

import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.io.bytestring.ByteString
import org.multipaz.asn1.ASN1Integer
import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.RawCbor
import org.multipaz.cbor.Tagged
import org.multipaz.cbor.buildCborMap
import org.multipaz.cbor.toDataItem
import org.multipaz.cose.Cose
import org.multipaz.cose.CoseLabel
import org.multipaz.cose.CoseNumberLabel
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.AsymmetricKey
import org.multipaz.crypto.Crypto
import org.multipaz.crypto.EcCurve
import org.multipaz.crypto.X500Name
import org.multipaz.crypto.X509CertChain
import org.multipaz.document.Document
import org.multipaz.document.DocumentStore
import org.multipaz.documenttype.knowntypes.DrivingLicense
import org.multipaz.mdoc.credential.MdocCredential
import org.multipaz.mdoc.issuersigned.buildIssuerNamespaces
import org.multipaz.mdoc.mso.MobileSecurityObject
import org.multipaz.mdoc.util.MdocUtil
import org.multipaz.securearea.CreateKeySettings
import org.multipaz.securearea.SecureArea
import org.multipaz.util.Logger
import org.multipaz.util.truncateToWholeSeconds

private const val TAG = "LocalTestIssuer"

/** Display name marking locally issued test documents. */
const val LOCAL_TEST_MDL_DISPLAY_NAME = "Test mDL (local)"

/**
 * Issues an ISO 18013-5 mDL locally with a self-generated IACA/DS chain.
 *
 * Rationale: the hosted issuer (issuer.multipaz.org) includes `keyAuthorizations`
 * in the MSO's `deviceKeyInfo`, which the Longfellow ZK circuits (v6 and v7)
 * cannot parse — every ZKP presentment with such a credential fails with
 * MDOC_PROVER_GENERAL_FAILURE (error code 6). This local issuance produces an
 * MSO without `keyAuthorizations` so ZKP presentment works.
 *
 * The IACA certificate PEM is logged so it can be added to a verifier's
 * trusted-issuers list. ZK proof verification itself does not require issuer
 * trust — it verifies against the DS certificate carried in the response.
 *
 * @return the created [Document].
 */
suspend fun issueLocalTestMdl(
    documentStore: DocumentStore,
    secureArea: SecureArea,
    cardArt: ByteString?,
): Document {
    val now = Clock.System.now().truncateToWholeSeconds()

    // IACA + DS, both P-256/SHA-256 as required by the Longfellow circuits.
    val iacaKey = Crypto.createEcPrivateKey(EcCurve.P256)
    val iacaCert = MdocUtil.generateIacaCertificate(
        iacaKey = AsymmetricKey.AnonymousExplicit(iacaKey),
        subject = X500Name.fromName("CN=Local Test IACA,C=DE"),
        serial = ASN1Integer.fromRandom(numBits = 128),
        validFrom = now - 1.hours,
        validUntil = now + 365.days,
        issuerAltNameUrl = "https://example.local/iaca",
        crlUrl = "https://example.local/crl",
    )
    val iacaAsymmetricKey = AsymmetricKey.X509CertifiedExplicit(
        certChain = X509CertChain(listOf(iacaCert)),
        privateKey = iacaKey,
    )
    val dsKey = Crypto.createEcPrivateKey(EcCurve.P256)
    val dsCert = MdocUtil.generateDsCertificate(
        iacaKey = iacaAsymmetricKey,
        dsKey = dsKey.publicKey,
        subject = X500Name.fromName("CN=Local Test DS,C=DE"),
        serial = ASN1Integer.fromRandom(numBits = 128),
        validFrom = now - 1.hours,
        validUntil = now + 90.days,
    )
    val dsAsymmetricKey = AsymmetricKey.X509CertifiedExplicit(
        certChain = X509CertChain(listOf(dsCert, iacaCert)),
        privateKey = dsKey,
    )
    Logger.i(TAG, "IACA certificate for verifier trust list:\n${iacaCert.toPem()}")

    val documentType = DrivingLicense.getDocumentType()
    val mdocType = documentType.mdocDocumentType!!

    // Restrict to a compact element set: each element adds a digest to the MSO,
    // and the Longfellow v7 circuits only support MSOs up to 2551 bytes
    // (the full DrivingLicense sample set yields a 2583-byte MSO).
    val includedElements = setOf(
        "family_name", "given_name", "birth_date", "issue_date", "expiry_date",
        "issuing_country", "issuing_authority", "document_number", "portrait",
        "driving_privileges", "un_distinguishing_sign", "sex", "nationality",
        "age_over_18", "age_over_21",
    )
    val issuerNamespaces = buildIssuerNamespaces {
        for ((nsName, ns) in mdocType.namespaces) {
            addNamespace(nsName) {
                for ((deName, de) in ns.dataElements) {
                    if (deName !in includedElements) continue
                    val sampleValue = de.attribute.sampleValueMdoc
                    if (sampleValue != null) {
                        addDataElement(deName, sampleValue)
                    }
                }
            }
        }
    }

    val document = documentStore.createDocument(
        displayName = LOCAL_TEST_MDL_DISPLAY_NAME,
        typeDisplayName = documentType.displayName,
        cardArt = cardArt,
    )

    val signedAt = now - 1.hours
    val validFrom = now - 1.hours
    val validUntil = now + 30.days

    val mdocCredential = MdocCredential.create(
        document = document,
        asReplacementForIdentifier = null,
        domain = "mdoc_user_auth",
        secureArea = secureArea,
        docType = mdocType.docType,
        createKeySettings = CreateKeySettings(
            algorithm = Algorithm.ESP256,
            userAuthenticationRequired = false,
            validFrom = validFrom,
            validUntil = validUntil,
        ),
    )

    // MSO deliberately without keyAuthorizations (and without status).
    val mso = MobileSecurityObject(
        version = "1.0",
        docType = mdocType.docType,
        signedAt = signedAt,
        validFrom = validFrom,
        validUntil = validUntil,
        expectedUpdate = null,
        digestAlgorithm = Algorithm.SHA256,
        valueDigests = issuerNamespaces.getValueDigests(Algorithm.SHA256),
        deviceKey = mdocCredential.getAttestation().publicKey,
    )
    val taggedEncodedMso = Cbor.encode(
        Tagged(Tagged.ENCODED_CBOR, Bstr(Cbor.encode(mso.toDataItem())))
    )
    val protectedHeaders = mapOf<CoseLabel, DataItem>(
        CoseNumberLabel(Cose.COSE_LABEL_ALG) to
            Algorithm.ES256.coseAlgorithmIdentifier!!.toDataItem()
    )
    val unprotectedHeaders = mapOf<CoseLabel, DataItem>(
        CoseNumberLabel(Cose.COSE_LABEL_X5CHAIN) to
            dsAsymmetricKey.certChain.toDataItem()
    )
    val encodedIssuerAuth = Cbor.encode(
        Cose.coseSign1Sign(
            dsAsymmetricKey,
            taggedEncodedMso,
            true,
            protectedHeaders,
            unprotectedHeaders,
        ).toDataItem()
    )
    val issuerProvidedAuthenticationData = Cbor.encode(
        buildCborMap {
            put("nameSpaces", issuerNamespaces.toDataItem())
            put("issuerAuth", RawCbor(encodedIssuerAuth))
        }
    )
    mdocCredential.certify(ByteString(issuerProvidedAuthenticationData))

    Logger.i(TAG, "Issued local test mDL, document ${document.identifier}, MSO ${taggedEncodedMso.size} bytes")
    return document
}
