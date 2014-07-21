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

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.BeforeClass;
import org.junit.Test;

import com.codenvy.cli.command.builtin.Constants;

/**
 * @author Stéphane Daviet
 */
public class CachedFileCredentialsStoreFactoryTest {
    @BeforeClass
    public static void setup() {
        System.setProperty("karaf.home", ".");
    }

    @Test
    public void testGetDataStore() {
        new CachedFileCredentialsStoreFactory().getDataStore("credentialsStore");

        assertTrue(new File(Constants.CREDENTIAL_STORE_FILE).exists());
    }
}
