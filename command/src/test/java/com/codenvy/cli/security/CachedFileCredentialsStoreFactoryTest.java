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
        CachedFileCredentialsStoreFactory.getInstance().getDataStore("credentialsStore");

        assertTrue(new File(Constants.CREDENTIAL_STORE_FILE).exists());
    }
}