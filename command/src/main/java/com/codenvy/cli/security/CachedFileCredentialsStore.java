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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.codenvy.client.CodenvyAPI;
import com.codenvy.client.CodenvyClient;
import com.codenvy.client.auth.Credentials;
import com.codenvy.client.store.DataStore;

/**
 * <p>
 * {@link DataStore} to store credentials in a file (persistent between JVM restarts). The file is formated in JSON.
 * </p>
 * <p>
 * The file is first loaded at class construction. A malformed file is overwrited by a blank new one. The content of the file is cached in
 * memory. Each get is made from this cache. Each put puts the value in the cache and then triggers a full write of the file (by serializing
 * the cache as JSON).
 * </p>
 *
 * @author Stéphane Daviet
 */
public class CachedFileCredentialsStore implements DataStore<String, Credentials> {
    private File                                     storeFile;
    private ConcurrentMap<String, StoredCredentials> cachedStore;
    private CodenvyClient                            codenvyClient;

    /**
     * <p>
     * Package visible constructor as this {@link DataStore} is only meant to be instantiated through
     * {@link CachedFileCredentialsStoreFactory}.
     * </p>
     * <p>
     * The file storing the {@link Credentials}s is loaded in a in-memory cache.
     * </p>
     *
     * @param storeFile the file where credentials whould be read from and writed in.
     */
    CachedFileCredentialsStore(File storeFile) {
        super();

        this.mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        setStoreFile(storeFile);
        loadFile();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Get the {@link Credentials} stored for a given key. {@link Credentials} is retrieved from the in-memory cache.
     * </p>
     */
    @Override
    public Credentials get(String key) {
        return CredentialsHelper.getInstance().fromStoreCredentials(cachedStore.get(key));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Store the {@link Credentials} first in the in-memory cache, then dump this cache to the file. This method is synchronized to prevent
     * multiple threads from writing the file simultaneously.
     * </p>
     */
    @Override
    public synchronized Credentials put(String key, Credentials credentials) {
        StoredCredentials previouslyStoredCredentials = cachedStore.put(key,
                                                                        CredentialsHelper.getInstance()
                                                                                         .toStoreCredentials(credentials));

        dumpFile();

        return CredentialsHelper.getInstance().fromStoreCredentials(previouslyStoredCredentials);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Same than {@link #put(String, Credentials)} for the mechanism: {@link Credentials} is deleted for the given key, then the file is
     * dumped.
     * </p>
     */
    @Override
    public Credentials delete(String key) {
        StoredCredentials previouslyStoredCredentials = cachedStore.remove(key);

        dumpFile();

        return CredentialsHelper.getInstance().fromStoreCredentials(previouslyStoredCredentials);
    }


    /**
     * Get the {@link CodenvyClient} used at runtime.
     *
     * @return the current {@link CodenvyClient}.
     */
    protected CodenvyClient getCodenvyClient() {
        if (codenvyClient == null) {
            codenvyClient = CodenvyAPI.getClient();
        }
        return codenvyClient;
    }

    /**
     * Defines the {@link CodenvyClient} to use.
     *
     * @param codenvyClient the {@link CodenvyClient} to use.
     */
    protected void setCodenvyClient(CodenvyClient codenvyClient) {
        this.codenvyClient = codenvyClient;
    }

    /**
     * Get the {@link File} where {@link Credentials} are stored.
     *
     * @return the {@link Credentials} store file.
     */
    protected File getStoreFile() {
        return storeFile;
    }

    /**
     * Set the {@link File} where {@link Credentials} are stored.
     *
     * @param storeFile the {@link Credentials} store file.
     */
    protected void setStoreFile(File storeFile) {
        this.storeFile = storeFile;
    }

    /**
     * Load the file where {@link Credentials} are stored.
     */
    protected void loadFile() {
        try {
            if (!storeFile.exists()) {
                storeFile.createNewFile();
                this.cachedStore = new ConcurrentHashMap<>();
                return;
            }
            TypeFactory typeFactory = mapper.getTypeFactory();
            MapType mapType = typeFactory.constructMapType(ConcurrentMap.class, String.class, StoredCredentials.class);

            this.cachedStore = mapper.readValue(storeFile, mapType);
        } catch (JsonMappingException e) {
            this.cachedStore = new ConcurrentHashMap<>();
            return;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Dump the in-memory cache into the file where {@link Credentials}.
     */
    protected void dumpFile() {
        synchronized (storeFile) {
            try {
                mapper.writeValue(storeFile, cachedStore);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
