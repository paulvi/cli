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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.codenvy.client.CodenvyClient;
import com.codenvy.client.auth.Credentials;
import com.codenvy.client.auth.CredentialsBuilder;
import com.codenvy.client.auth.Token;
import com.codenvy.client.auth.TokenBuilder;

/**
 * @author Stéphane Daviet
 */
@RunWith(MockitoJUnitRunner.class)
public class CachedFileCredentialsStoreTest {
    @Mock
    private CodenvyClient              codenvyClient;

    @Mock
    private CredentialsBuilder         credentialsBuilder;

    @Mock
    private TokenBuilder               tokenBuilder;

    @Mock
    private Credentials                credentials;

    @Mock
    private Token                      token;

    private CachedFileCredentialsStore cachedFileCredentialsStore;

    @Before
    public void setup() throws IOException, URISyntaxException {
        cachedFileCredentialsStore = new CachedFileCredentialsStore(getTempFile(false));
        cachedFileCredentialsStore.setCodenvyClient(codenvyClient);

        CredentialsHelper.getInstance().setCodenvyClient(codenvyClient);

        when(codenvyClient.newCredentialsBuilder()).thenReturn(credentialsBuilder);
        when(codenvyClient.newTokenBuilder(anyString())).thenReturn(tokenBuilder);

        when(credentialsBuilder.withUsername(anyString())).thenReturn(credentialsBuilder);
        when(credentialsBuilder.withPassword(anyString())).thenReturn(credentialsBuilder);
        when(credentialsBuilder.withToken(any(Token.class))).thenReturn(credentialsBuilder);
        when(credentialsBuilder.build()).thenReturn(credentials);

        when(credentials.username()).thenReturn("jonhdo");
        when(credentials.password()).thenReturn("secret");
        when(credentials.token()).thenReturn(token);

        when(tokenBuilder.build()).thenReturn(token);

        when(token.value()).thenReturn("token#123");
    }

    @Test
    public void testPutAbsent() throws IOException, URISyntaxException {
        File tempCredentialsStoreFile = createAndLoadTempFile(false);

        Credentials credentials = codenvyClient.newCredentialsBuilder()
                                               .withUsername("josh")
                                               .withToken(codenvyClient.newTokenBuilder("token#123")
                                                                       .build())
                                               .build();
        cachedFileCredentialsStore.put("fake", credentials);

        assertEquals(credentials, cachedFileCredentialsStore.get("fake"));

        File expectedFile = new File(Thread.currentThread()
                                           .getContextClassLoader()
                                           .getResource("credentialStore.json")
                                           .toURI());
        assertThat(tempCredentialsStoreFile).hasContentEqualTo(expectedFile);
    }

    @Test
    public void testAlreadyInFile() throws IOException, URISyntaxException {
        createAndLoadTempFile(true);

        assertEquals(credentials, cachedFileCredentialsStore.get("fake"));
    }

    @Test
    public void testDelete() throws IOException, URISyntaxException {
        File tempCredentialsStoreFile = createAndLoadTempFile(true);

        cachedFileCredentialsStore.delete("fake");

        assertNull(cachedFileCredentialsStore.get("fake"));
        assertThat(tempCredentialsStoreFile).hasContent("{ }");
    }

    private File createAndLoadTempFile(boolean withSampleContent) throws IOException, URISyntaxException {
        File tempFile = getTempFile(withSampleContent);
        cachedFileCredentialsStore.setStoreFile(tempFile);
        cachedFileCredentialsStore.loadFile();
        return tempFile;
    }

    private File getTempFile(boolean withSampleContent) throws IOException, URISyntaxException {
        File tempCredentialsStoreFile = File.createTempFile("credentialStore", ".json");
        tempCredentialsStoreFile.deleteOnExit();
        if (withSampleContent) {
            Files.copy(new File(Thread.currentThread()
                                      .getContextClassLoader()
                                      .getResource("credentialStore.json")
                                      .toURI()).toPath(),
                       new FileOutputStream(tempCredentialsStoreFile));
        }
        return tempCredentialsStoreFile;
    }
}
