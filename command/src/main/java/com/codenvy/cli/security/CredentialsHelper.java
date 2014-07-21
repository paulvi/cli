/*******************************************************************************
 * Copyright (c) 2012-2014 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
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
                                                        .withUsername(storedCredentials.getUsername())
                                                        .withToken(codenvyClient.newTokenBuilder(storedCredentials.getToken())
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

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Cannot clone singleton.");
    }
}
