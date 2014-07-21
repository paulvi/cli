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

/**
 * @author Stéphane Daviet
 */
public class StoredCredentials {
    private String username;

    private String token;

    public StoredCredentials() {
        super();
    }

    public StoredCredentials(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }

    public StoredCredentials withUsername(String username) {
        this.username = username;
        return this;
    }

    public StoredCredentials withToken(String token) {
        this.token = token;
        return this;
    }
}
