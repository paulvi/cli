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

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Stéphane Daviet
 */
@JsonAutoDetect(getterVisibility = JsonAutoDetect.Visibility.NONE, isGetterVisibility = JsonAutoDetect.Visibility.NONE)
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

    @JsonProperty
    public String username() {
        return username;
    }

    @JsonProperty
    public String token() {
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
