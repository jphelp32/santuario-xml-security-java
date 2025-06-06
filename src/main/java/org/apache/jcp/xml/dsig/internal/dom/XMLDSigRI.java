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
/*
 * ===========================================================================
 *
 * (C) Copyright IBM Corp. 2003 All Rights Reserved.
 *
 * ===========================================================================
 */
/*
 * Portions copyright 2005 Sun Microsystems, Inc. All rights reserved.
 */
package org.apache.jcp.xml.dsig.internal.dom;

import java.security.AccessController;
import java.security.InvalidParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivilegedAction;
import java.security.Provider;
import java.security.ProviderException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.crypto.dsig.Transform;

/**
 * The XMLDSig RI Provider.
 *
 */

/**
 * Defines the XMLDSigRI provider.
 */

public final class XMLDSigRI extends Provider {

    static final long serialVersionUID = -5049765099299494554L;

    private static final String INFO = "Apache Santuario XMLDSig " +
        "(DOM XMLSignatureFactory; DOM KeyInfoFactory; " +
        "C14N 1.0, C14N 1.1, Exclusive C14N, Base64, Enveloped, XPath, " +
        "XPath2, XSLT TransformServices)";

    private static final class ProviderService extends Provider.Service {

        ProviderService(Provider p, String type, String algo, String cn) {
            super(p, type, algo, cn, null, null);
        }

        ProviderService(Provider p, String type, String algo, String cn,
            String[] aliases) {
            super(p, type, algo, cn,
                aliases == null ? null : Arrays.asList(aliases), null);
        }

        ProviderService(Provider p, String type, String algo, String cn,
            String[] aliases, Map<String, String> attrs) {
            super(p, type, algo, cn,
                  aliases == null ? null : Arrays.asList(aliases), attrs);
        }

        @Override
        public Object newInstance(Object ctrParamObj)
            throws NoSuchAlgorithmException {
            String type = getType();
            if (ctrParamObj != null) {
                throw new InvalidParameterException
                    ("constructorParameter not used with " + type + " engines");
            }

            String algo = getAlgorithm();
            try {
                if ("XMLSignatureFactory".equals(type)) {
                    if ("DOM".equals(algo)) {
                        return new DOMXMLSignatureFactory();
                    }
                } else if ("KeyInfoFactory".equals(type)) {
                    if ("DOM".equals(algo)) {
                        return new DOMKeyInfoFactory();
                    }
                } else if ("TransformService".equals(type)) {
                    if (algo.equals(CanonicalizationMethod.INCLUSIVE) ||
                        algo.equals(CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS)) {
                        return new DOMCanonicalXMLC14NMethod();
                    } else if ("http://www.w3.org/2006/12/xml-c14n11".equals(algo) ||
                        "http://www.w3.org/2006/12/xml-c14n11#WithComments".equals(algo)) {
                        return new DOMCanonicalXMLC14N11Method();
                    } else if (algo.equals(CanonicalizationMethod.EXCLUSIVE) ||
                        algo.equals(CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS)) {
                        return new DOMExcC14NMethod();
                    } else if (algo.equals(Transform.BASE64)) {
                        return new DOMBase64Transform();
                    } else if (algo.equals(Transform.ENVELOPED)) {
                        return new DOMEnvelopedTransform();
                    } else if (algo.equals(Transform.XPATH2)) {
                        return new DOMXPathFilter2Transform();
                    } else if (algo.equals(Transform.XPATH)) {
                        return new DOMXPathTransform();
                    } else if (algo.equals(Transform.XSLT)) {
                        return new DOMXSLTTransform();
                    }
                }
            } catch (Exception ex) {
                throw new NoSuchAlgorithmException("Error constructing " +
                    type + " for " + algo + " using XMLDSig", ex);
            }
            throw new ProviderException("No impl for " + algo +
                " " + type);
        }
    }

    public XMLDSigRI() {
        /* We are the ApacheXMLDSig provider */
        super("ApacheXMLDSig", "4.0.5", INFO);

        final Provider p = this;
        AccessController.doPrivileged(new PrivilegedAction<Void>() {
            @Override
            public Void run() {
                Map<String, String> MECH_TYPE = new HashMap<>();
                MECH_TYPE.put("MechanismType", "DOM");

                putService(new ProviderService(p, "XMLSignatureFactory",
                    "DOM", "org.apache.jcp.xml.dsig.internal.dom.DOMXMLSignatureFactory"));

                putService(new ProviderService(p, "KeyInfoFactory",
                    "DOM", "org.apache.jcp.xml.dsig.internal.dom.DOMKeyInfoFactory"));


                // Inclusive C14N
                putService(new ProviderService(p, "TransformService",
                    CanonicalizationMethod.INCLUSIVE,
                    "org.apache.jcp.xml.dsig.internal.dom.DOMCanonicalXMLC14NMethod",
                    new String[] {"INCLUSIVE"}, MECH_TYPE));

                // InclusiveWithComments C14N
                putService(new ProviderService(p, "TransformService",
                    CanonicalizationMethod.INCLUSIVE_WITH_COMMENTS,
                    "org.apache.jcp.xml.dsig.internal.dom.DOMCanonicalXMLC14NMethod",
                    new String[] {"INCLUSIVE_WITH_COMMENTS"}, MECH_TYPE));

                // Inclusive C14N 1.1
                putService(new ProviderService(p, "TransformService",
                    "http://www.w3.org/2006/12/xml-c14n11",
                    "org.apache.jcp.xml.dsig.internal.dom.DOMCanonicalXMLC14N11Method",
                    null, MECH_TYPE));

                // InclusiveWithComments C14N 1.1
                putService(new ProviderService(p, "TransformService",
                    "http://www.w3.org/2006/12/xml-c14n11#WithComments",
                    "org.apache.jcp.xml.dsig.internal.dom.DOMCanonicalXMLC14N11Method",
                    null, MECH_TYPE));

                // Exclusive C14N
                putService(new ProviderService(p, "TransformService",
                    CanonicalizationMethod.EXCLUSIVE,
                    "org.apache.jcp.xml.dsig.internal.dom.DOMExcC14NMethod",
                    new String[] {"EXCLUSIVE"}, MECH_TYPE));

                // ExclusiveWithComments C14N
                putService(new ProviderService(p, "TransformService",
                    CanonicalizationMethod.EXCLUSIVE_WITH_COMMENTS,
                    "org.apache.jcp.xml.dsig.internal.dom.DOMExcC14NMethod",
                    new String[] {"EXCLUSIVE_WITH_COMMENTS"}, MECH_TYPE));

                // Base64 Transform
                putService(new ProviderService(p, "TransformService",
                    Transform.BASE64,
                    "org.apache.jcp.xml.dsig.internal.dom.DOMBase64Transform",
                    new String[] {"BASE64"}, MECH_TYPE));

                // Enveloped Transform
                putService(new ProviderService(p, "TransformService",
                    Transform.ENVELOPED,
                    "org.apache.jcp.xml.dsig.internal.dom.DOMEnvelopedTransform",
                    new String[] {"ENVELOPED"}, MECH_TYPE));

                // XPath2 Transform
                putService(new ProviderService(p, "TransformService",
                    Transform.XPATH2,
                    "org.apache.jcp.xml.dsig.internal.dom.DOMXPathFilter2Transform",
                    new String[] {"XPATH2"}, MECH_TYPE));

                // XPath Transform
                putService(new ProviderService(p, "TransformService",
                    Transform.XPATH,
                    "org.apache.jcp.xml.dsig.internal.dom.DOMXPathTransform",
                    new String[] {"XPATH"}, MECH_TYPE));

                // XSLT Transform
                putService(new ProviderService(p, "TransformService",
                    Transform.XSLT,
                    "org.apache.jcp.xml.dsig.internal.dom.DOMXSLTTransform",
                    new String[] {"XSLT"}, MECH_TYPE));
                return null;
            }
        });
    }
}
