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

import com.codenvy.cli.command.builtin.Constants;
import com.codenvy.client.auth.Credentials;
import com.codenvy.client.auth.CredentialsProvider;

/**
 * @author Stéphane Daviet
 */
public class FileBasedCredentialsProvider implements CredentialsProvider {
    private final String environmentAlias;

    public FileBasedCredentialsProvider(String environmentAlias) {
        this.environmentAlias = environmentAlias;
    }

    @Override
    public Credentials getCredentials(String username) {
        return CachedFileCredentialsStoreFactory.getInstance()
                                                .getDataStore(Constants.CREDENTIALS_STORE_KEY)
                                                .get(environmentAlias);
    }

}
