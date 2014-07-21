/*
 * CODENVY CONFIDENTIAL
 * ________________
 *
 * [2012] - [2014] Codenvy, S.A.
 * All Rights Reserved.
 * NOTICE: All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any. The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.cli.security;

import com.codenvy.client.CodenvyClient;
import com.codenvy.client.auth.Credentials;

/**
 * @author Stéphane Daviet
 */
public final class CredentialsHelper {
    private static final CredentialsHelper INSTANCE = new CredentialsHelper();

    private CodenvyClient                  codenvyClient;

    private CredentialsHelper() {
    }

    public static CredentialsHelper getInstance() {
        return INSTANCE;
    }

    public StoredCredentials toStoreCredentials(Credentials credentials) {
        return new StoredCredentials(credentials.username(), credentials.token().value());
    }

    public Credentials fromStoreCredentials(StoredCredentials storedCredentials) {
        return storedCredentials != null ? codenvyClient.newCredentialsBuilder()
                                                        .withUsername(storedCredentials.username())
                                                        .withToken(codenvyClient.newTokenBuilder(storedCredentials.token())
                                                                                .build())
                                                        .build() : null;
    }

    /**
     * @param codenvyClient the codenvyClient to set
     */
    protected void setCodenvyClient(CodenvyClient codenvyClient) {
        this.codenvyClient = codenvyClient;
    }

    /**
     * @return the codenvyClient
     */
    protected CodenvyClient getCodenvyClient() {
        return codenvyClient;
    }
}
