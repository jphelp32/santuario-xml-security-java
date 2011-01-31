/*
 * Copyright 1999-2009 The Apache Software Foundation.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.apache.xml.security.test.interop;

import java.io.File;
import java.security.PublicKey;
import java.security.cert.X509Certificate;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.xml.security.keys.KeyInfo;
import org.apache.xml.security.signature.XMLSignature;
import org.apache.xml.security.test.DSNamespaceContext;
import org.apache.xml.security.utils.resolver.ResourceResolverSpi;
import org.w3c.dom.Element;

public class InteropTestBase extends org.junit.Assert {

    /** {@link org.apache.commons.logging} logging facility */
    static org.apache.commons.logging.Log log = 
        org.apache.commons.logging.LogFactory.getLog(InteropTestBase.class.getName());

    /** Field dbf */
    static javax.xml.parsers.DocumentBuilderFactory dbf = null;

    /**
     * Method setUp
     *
     */
    public InteropTestBase() {
        dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
    }


    /**
     * Method verifyHMAC
     *
     * @param filename
     * @param resolver
     * @param hmacKey
     *
     * @throws Exception
     */
    public boolean verifyHMAC(
        String filename, ResourceResolverSpi resolver, boolean followManifests, byte[] hmacKey
    ) throws Exception {
        File f = new File(filename);
        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
        org.w3c.dom.Document doc = db.parse(new java.io.FileInputStream(f));
        
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        xpath.setNamespaceContext(new DSNamespaceContext());

        String expression = "//ds:Signature[1]";
        Element sigElement = 
            (Element) xpath.evaluate(expression, doc, XPathConstants.NODE);
        XMLSignature signature = new XMLSignature(sigElement, f.toURI().toURL().toString());

        if (resolver != null) {
            signature.addResourceResolver(resolver);
        }
        signature.setFollowNestedManifests(followManifests);

        byte keybytes[] = hmacKey;
        javax.crypto.SecretKey sk = signature.createSecretKey(keybytes);

        return signature.checkSignatureValue(sk);
    }

    /**
     * Method verify
     *
     * @param filename
     * @param resolver
     *
     * @throws Exception
     */
    public boolean verify(String filename, ResourceResolverSpi resolver, boolean followManifests)
        throws Exception {
        File f = new File(filename);
        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
        org.w3c.dom.Document doc = db.parse(f);
        
        XPathFactory xpf = XPathFactory.newInstance();
        XPath xpath = xpf.newXPath();
        xpath.setNamespaceContext(new DSNamespaceContext());

        String expression = "//ds:Signature[1]";
        Element sigElement = 
            (Element) xpath.evaluate(expression, doc, XPathConstants.NODE);
        XMLSignature signature = new XMLSignature(sigElement, f.toURI().toURL().toString());

        if (resolver != null) {
            signature.addResourceResolver(resolver);
        }
        signature.setFollowNestedManifests(followManifests);


        KeyInfo ki = signature.getKeyInfo();
        boolean result = false;
        if (ki != null) {
            X509Certificate cert = ki.getX509Certificate();

            if (cert != null) {
                result = signature.checkSignatureValue(cert);
            } else {
                PublicKey pk = ki.getPublicKey();

                if (pk != null) {
                    result = signature.checkSignatureValue(pk);
                } else {
                    throw new RuntimeException(
                    "Did not find a public key, so I can't check the signature");
                }
            }
        } else {
            throw new RuntimeException("Did not find a KeyInfo");
        }
        if (!result) {
            for (int i = 0; i < signature.getSignedInfo().getLength(); i++) {
                boolean refVerify =
                    signature.getSignedInfo().getVerificationResult(i);             

                if (refVerify) {
                    log.debug("Reference " + i + " was OK");
                } else {
                    // JavaUtils.writeBytesToFilename(filename + i + ".apache.txt", signature.getSignedInfo().item(i).getContentsAfterTransformation().getBytes());                
                    log.debug("Reference " + i + " was not OK");
                }
            }
            //throw new RuntimeException("Falle:"+sb.toString());
        }

        return result;
    }
    
}