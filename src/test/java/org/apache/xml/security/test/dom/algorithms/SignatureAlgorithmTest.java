/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.xml.security.test.dom.algorithms;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.lang.reflect.Field;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import java.util.Arrays;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

import org.apache.xml.security.algorithms.SignatureAlgorithm;
import org.apache.xml.security.algorithms.implementations.SignatureBaseRSA;
import org.apache.xml.security.algorithms.implementations.SignatureECDSA;
import org.apache.xml.security.exceptions.AlgorithmAlreadyRegisteredException;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.signature.XMLSignatureException;
import org.apache.xml.security.test.dom.TestUtils;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.w3c.dom.Document;

import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SignatureAlgorithmTest {

    private static final Logger LOG = System.getLogger(SignatureAlgorithmTest.class.getName());

    static {
        org.apache.xml.security.Init.init();
    }

    private final SecretKey secretKey;
    private final KeyPair keyPair;

    public SignatureAlgorithmTest() throws NoSuchAlgorithmException {
        KeyGenerator keygen = KeyGenerator.getInstance("AES");
        keygen.init(256);
        secretKey = keygen.generateKey();

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPair = keyPairGenerator.generateKeyPair();
    }

    @Test
    void testSameKeySeveralAlgorithmSigning() throws Exception {
        Document doc = TestUtils.newDocument();
        SignatureAlgorithm signatureAlgorithm =
            new SignatureAlgorithm(doc, XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1);
        PrivateKey pk = keyPair.getPrivate();
        signatureAlgorithm.initSign(pk);
        signatureAlgorithm.update((byte)2);
        signatureAlgorithm.sign();
        SignatureAlgorithm otherSignatureAlgorithm =
            new SignatureAlgorithm(doc, XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256);

        try {
            otherSignatureAlgorithm.initSign(pk);
        } catch (XMLSecurityException ex) {
            LOG.log(Level.WARNING,
                "Test testSameKeySeveralAlgorithmSigning skipped as necessary algorithms "
                + "not available"
            );
            return;
        }

        otherSignatureAlgorithm.update((byte)2);
        otherSignatureAlgorithm.sign();
    }

    @Test
    void testConstructionWithProvider() throws Exception {
        Field algorithmHashField = SignatureAlgorithm.class.getDeclaredField("algorithmHash");
        algorithmHashField.setAccessible(true); //NOPMD
        @SuppressWarnings("unchecked")
        Map<String, Class<?>> algorithmHash = (Map<String, Class<?>>)algorithmHashField.get(null);
        assertFalse(algorithmHash.isEmpty());

        Document doc = TestUtils.newDocument();
        Provider provider = null;
        try {
            Class<?> bouncyCastleProviderClass = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider");
            provider = (Provider)bouncyCastleProviderClass.getConstructor().newInstance();

            for (String algorithmURI : algorithmHash.keySet()) {
                try {
                    AlgorithmParameterSpec spec = algorithmURI.equals(XMLSignature.ALGO_ID_SIGNATURE_RSA_PSS)
                            ? new PSSParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, 32, 1)
                            : null;
                    SignatureAlgorithm signatureAlgorithm = new SignatureAlgorithm(doc, algorithmURI, provider, spec);
                    assertEquals(provider.getName(), signatureAlgorithm.getJCEProviderName());
                } catch (XMLSecurityException e) {
                    assertEquals("", Arrays.asList(e.getStackTrace()).toString());
                }
            }
        } catch (ReflectiveOperationException e) {
            // BouncyCastle not installed, skip
            assumeFalse(provider == null);
        }
    }

    @Test
    void testRSASigningKeyIsPrivateKey() throws Exception {
        Document doc = TestUtils.newDocument();
        SignatureAlgorithm signatureAlgorithm =
                new SignatureAlgorithm(doc, XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1);

        assertThrows(XMLSignatureException.class, () ->
            signatureAlgorithm.initSign(secretKey));
    }

    @Test
    void testDSASigningKeyIsPrivateKey() throws Exception {
        Document doc = TestUtils.newDocument();
        SignatureAlgorithm signatureAlgorithm =
                new SignatureAlgorithm(doc, XMLSignature.ALGO_ID_SIGNATURE_DSA);

        assertThrows(XMLSignatureException.class, () ->
                signatureAlgorithm.initSign(secretKey));
    }

    @Test
    void testECDSASigningKeyIsPrivateKey() throws Exception {
        Document doc = TestUtils.newDocument();
        SignatureAlgorithm signatureAlgorithm =
                new SignatureAlgorithm(doc, XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA1);

        assertThrows(XMLSignatureException.class, () ->
                signatureAlgorithm.initSign(secretKey));
    }

    @Test
    void testRSAVerifyingKeyIsPublicKey() throws Exception {
        Document doc = TestUtils.newDocument();
        SignatureAlgorithm signatureAlgorithm =
                new SignatureAlgorithm(doc, XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1);

        assertThrows(XMLSignatureException.class, () ->
                signatureAlgorithm.initVerify(secretKey));
    }

    @Test
    void testDSAVerifyingKeyIsPublicKey() throws Exception {
        Document doc = TestUtils.newDocument();
        SignatureAlgorithm signatureAlgorithm =
                new SignatureAlgorithm(doc, XMLSignature.ALGO_ID_SIGNATURE_DSA);

        assertThrows(XMLSignatureException.class, () ->
                signatureAlgorithm.initVerify(secretKey));
    }

    @Test
    void testECDSAVerifyingKeyIsPublicKey() throws Exception {
        Document doc = TestUtils.newDocument();
        SignatureAlgorithm signatureAlgorithm =
                new SignatureAlgorithm(doc, XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA1);

        assertThrows(XMLSignatureException.class, () ->
                signatureAlgorithm.initVerify(secretKey));
    }

    @Test
    void testHMACSigningKeyIsSecretKey() throws Exception {
        Document doc = TestUtils.newDocument();
        SignatureAlgorithm signatureAlgorithm =
                new SignatureAlgorithm(doc, XMLSignature.ALGO_ID_MAC_HMAC_SHA1);

        assertThrows(XMLSignatureException.class, () ->
                signatureAlgorithm.initSign(keyPair.getPrivate()));
    }

    @Test
    void testHMACVerifyingKeyIsSecretKey() throws Exception {
        Document doc = TestUtils.newDocument();
        SignatureAlgorithm signatureAlgorithm =
                new SignatureAlgorithm(doc, XMLSignature.ALGO_ID_MAC_HMAC_SHA1);

        assertThrows(XMLSignatureException.class, () ->
                signatureAlgorithm.initVerify(keyPair.getPublic()));
    }

    @Test
    void testAlreadyRegisteredException() throws Exception {
        assertThrows(AlgorithmAlreadyRegisteredException.class, () ->
            SignatureAlgorithm.register(XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256,
                    SignatureBaseRSA.SignatureRSASHA256.class)
        );
    }

    @Test
    void testAlreadyRegisteredExceptionFromString() throws Exception {
        assertThrows(AlgorithmAlreadyRegisteredException.class, () ->
                SignatureAlgorithm.register(XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA256,
                        SignatureBaseRSA.SignatureRSASHA256.class.getName())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            XMLSignature.ALGO_ID_SIGNATURE_EDDSA_ED25519,
            XMLSignature.ALGO_ID_SIGNATURE_EDDSA_ED448,
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA1,
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA224,
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA256,
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA384,
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA512,
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA3_224,
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA3_256,
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA3_384,
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_SHA3_512,
            XMLSignature.ALGO_ID_SIGNATURE_ECDSA_RIPEMD160,
    })
    void testAlreadyRegisteredExceptionFromString(String algorithmURI) {
        String algorithmMockName = SignatureECDSA.SignatureECDSASHA3_224.class.getName();

        AlgorithmAlreadyRegisteredException exception =
            assertThrows(AlgorithmAlreadyRegisteredException.class, () ->
                SignatureAlgorithm.register(algorithmURI,
                        algorithmMockName)
        );

        MatcherAssert.assertThat(exception.getMessage(), org.hamcrest.Matchers.containsString(algorithmURI));
    }
}
