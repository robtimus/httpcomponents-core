/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.ssl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.annotation.NotThreadSafe;
import org.apache.http.util.Args;

/**
 * Builder for {@link javax.net.ssl.SSLContext} instances.
 *
 * @since 4.4
 */
@NotThreadSafe
public class SSLContextBuilder {

    static final String TLS   = "TLS";
    static final String SSL   = "SSL";

    private String protocol;
    private Set<KeyManager> keymanagers;
    private Set<TrustManager> trustmanagers;
    private SecureRandom secureRandom;

    public static SSLContextBuilder create() {
        return new SSLContextBuilder();
    }

    public SSLContextBuilder() {
        super();
        this.keymanagers = new HashSet<KeyManager>();
        this.trustmanagers = new HashSet<TrustManager>();
    }

    public SSLContextBuilder useTLS() {
        this.protocol = TLS;
        return this;
    }

    public SSLContextBuilder useSSL() {
        this.protocol = SSL;
        return this;
    }

    public SSLContextBuilder useProtocol(final String protocol) {
        this.protocol = protocol;
        return this;
    }

    public SSLContextBuilder setSecureRandom(final SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
        return this;
    }

    public SSLContextBuilder loadTrustMaterial(
            final KeyStore truststore,
            final TrustStrategy trustStrategy) throws NoSuchAlgorithmException, KeyStoreException {
        final TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(
                TrustManagerFactory.getDefaultAlgorithm());
        tmfactory.init(truststore);
        final TrustManager[] tms = tmfactory.getTrustManagers();
        if (tms != null) {
            if (trustStrategy != null) {
                for (int i = 0; i < tms.length; i++) {
                    final TrustManager tm = tms[i];
                    if (tm instanceof X509TrustManager) {
                        tms[i] = new TrustManagerDelegate(
                                (X509TrustManager) tm, trustStrategy);
                    }
                }
            }
            for (final TrustManager tm : tms) {
                this.trustmanagers.add(tm);
            }
        }
        return this;
    }

    public SSLContextBuilder loadTrustMaterial(
            final TrustStrategy trustStrategy) throws NoSuchAlgorithmException, KeyStoreException {
        return loadTrustMaterial(null, trustStrategy);
    }

    public SSLContextBuilder loadTrustMaterial(
            final File file,
            final char[] storePassword,
            final TrustStrategy trustStrategy) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        Args.notNull(file, "Truststore file");
        final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        final FileInputStream instream = new FileInputStream(file);
        try {
            trustStore.load(instream, storePassword);
        } finally {
            instream.close();
        }
        return loadTrustMaterial(trustStore, trustStrategy);
    }

    public SSLContextBuilder loadTrustMaterial(
            final File file,
            final char[] storePassword) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        return loadTrustMaterial(file, storePassword, null);
    }

    public SSLContextBuilder loadTrustMaterial(
            final File file) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        return loadTrustMaterial(file, null);
    }

    public SSLContextBuilder loadTrustMaterial(
            final URL url,
            final char[] storePassword,
            final TrustStrategy trustStrategy) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        Args.notNull(url, "Truststore URL");
        final KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
        final InputStream instream = url.openStream();
        try {
            trustStore.load(instream, storePassword);
        } finally {
            instream.close();
        }
        return loadTrustMaterial(trustStore, trustStrategy);
    }

    public SSLContextBuilder loadTrustMaterial(
            final URL url,
            final char[] storePassword) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        return loadTrustMaterial(url, storePassword, null);
    }

    public SSLContextBuilder loadKeyMaterial(
            final KeyStore keystore,
            final char[] keyPassword,
            final PrivateKeyStrategy aliasStrategy)
            throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        final KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(
                KeyManagerFactory.getDefaultAlgorithm());
        kmfactory.init(keystore, keyPassword);
        final KeyManager[] kms =  kmfactory.getKeyManagers();
        if (kms != null) {
            if (aliasStrategy != null) {
                for (int i = 0; i < kms.length; i++) {
                    final KeyManager km = kms[i];
                    if (km instanceof X509KeyManager) {
                        kms[i] = new KeyManagerDelegate(
                                (X509KeyManager) km, aliasStrategy);
                    }
                }
            }
            for (final KeyManager km : kms) {
                keymanagers.add(km);
            }
        }
        return this;
    }

    public SSLContextBuilder loadKeyMaterial(
            final KeyStore keystore,
            final char[] keyPassword) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        return loadKeyMaterial(keystore, keyPassword, null);
    }

    public SSLContextBuilder loadKeyMaterial(
            final File file,
            final char[] storePassword,
            final char[] keyPassword,
            final PrivateKeyStrategy aliasStrategy) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, CertificateException, IOException {
        Args.notNull(file, "Keystore file");
        final KeyStore identityStore = KeyStore.getInstance(KeyStore.getDefaultType());
        final FileInputStream instream = new FileInputStream(file);
        try {
            identityStore.load(instream, storePassword);
        } finally {
            instream.close();
        }
        return loadKeyMaterial(identityStore, keyPassword, aliasStrategy);
    }

    public SSLContextBuilder loadKeyMaterial(
            final File file,
            final char[] storePassword,
            final char[] keyPassword) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, CertificateException, IOException {
        return loadKeyMaterial(file, storePassword, keyPassword, null);
    }

    public SSLContextBuilder loadKeyMaterial(
            final URL url,
            final char[] storePassword,
            final char[] keyPassword,
            final PrivateKeyStrategy aliasStrategy) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, CertificateException, IOException {
        Args.notNull(url, "Keystore URL");
        final KeyStore identityStore = KeyStore.getInstance(KeyStore.getDefaultType());
        final InputStream instream = url.openStream();
        try {
            identityStore.load(instream, storePassword);
        } finally {
            instream.close();
        }
        return loadKeyMaterial(identityStore, keyPassword, aliasStrategy);
    }

    public SSLContextBuilder loadKeyMaterial(
            final URL url,
            final char[] storePassword,
            final char[] keyPassword) throws NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException, CertificateException, IOException {
        return loadKeyMaterial(url, storePassword, keyPassword, null);
    }

    public SSLContext build() throws NoSuchAlgorithmException, KeyManagementException {
        final SSLContext sslcontext = SSLContext.getInstance(
                this.protocol != null ? this.protocol : TLS);
        sslcontext.init(
                !keymanagers.isEmpty() ? keymanagers.toArray(new KeyManager[keymanagers.size()]) : null,
                !trustmanagers.isEmpty() ? trustmanagers.toArray(new TrustManager[trustmanagers.size()]) : null,
                secureRandom);
        return sslcontext;
    }

    static class TrustManagerDelegate implements X509TrustManager {

        private final X509TrustManager trustManager;
        private final TrustStrategy trustStrategy;

        TrustManagerDelegate(final X509TrustManager trustManager, final TrustStrategy trustStrategy) {
            super();
            this.trustManager = trustManager;
            this.trustStrategy = trustStrategy;
        }

        @Override
        public void checkClientTrusted(
                final X509Certificate[] chain, final String authType) throws CertificateException {
            this.trustManager.checkClientTrusted(chain, authType);
        }

        @Override
        public void checkServerTrusted(
                final X509Certificate[] chain, final String authType) throws CertificateException {
            if (!this.trustStrategy.isTrusted(chain, authType)) {
                this.trustManager.checkServerTrusted(chain, authType);
            }
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return this.trustManager.getAcceptedIssuers();
        }

    }

    static class KeyManagerDelegate implements X509KeyManager {

        private final X509KeyManager keyManager;
        private final PrivateKeyStrategy aliasStrategy;

        KeyManagerDelegate(final X509KeyManager keyManager, final PrivateKeyStrategy aliasStrategy) {
            super();
            this.keyManager = keyManager;
            this.aliasStrategy = aliasStrategy;
        }

        @Override
        public String[] getClientAliases(
                final String keyType, final Principal[] issuers) {
            return this.keyManager.getClientAliases(keyType, issuers);
        }

        @Override
        public String chooseClientAlias(
                final String[] keyTypes, final Principal[] issuers, final Socket socket) {
            final Map<String, PrivateKeyDetails> validAliases = new HashMap<String, PrivateKeyDetails>();
            for (final String keyType: keyTypes) {
                final String[] aliases = this.keyManager.getClientAliases(keyType, issuers);
                if (aliases != null) {
                    for (final String alias: aliases) {
                        validAliases.put(alias,
                                new PrivateKeyDetails(keyType, this.keyManager.getCertificateChain(alias)));
                    }
                }
            }
            return this.aliasStrategy.chooseAlias(validAliases, socket);
        }

        @Override
        public String[] getServerAliases(
                final String keyType, final Principal[] issuers) {
            return this.keyManager.getServerAliases(keyType, issuers);
        }

        @Override
        public String chooseServerAlias(
                final String keyType, final Principal[] issuers, final Socket socket) {
            final Map<String, PrivateKeyDetails> validAliases = new HashMap<String, PrivateKeyDetails>();
            final String[] aliases = this.keyManager.getServerAliases(keyType, issuers);
            if (aliases != null) {
                for (final String alias: aliases) {
                    validAliases.put(alias,
                            new PrivateKeyDetails(keyType, this.keyManager.getCertificateChain(alias)));
                }
            }
            return this.aliasStrategy.chooseAlias(validAliases, socket);
        }

        @Override
        public X509Certificate[] getCertificateChain(final String alias) {
            return this.keyManager.getCertificateChain(alias);
        }

        @Override
        public PrivateKey getPrivateKey(final String alias) {
            return this.keyManager.getPrivateKey(alias);
        }

    }

}