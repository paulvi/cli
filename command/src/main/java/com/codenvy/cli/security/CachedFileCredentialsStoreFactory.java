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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.codenvy.cli.command.builtin.Constants;
import com.codenvy.client.auth.Credentials;
import com.codenvy.client.store.DataStore;
import com.codenvy.client.store.DataStoreFactory;

/**
 * Factory that gives {@link CachedFileCredentialsStore} instances relative to a given key thanks to {@link #getDataStore(String)}.
 *
 * @author Stéphane Daviet
 */
public final class CachedFileCredentialsStoreFactory implements DataStoreFactory<String, Credentials> {
    private final static CachedFileCredentialsStoreFactory              INSTANCE = new CachedFileCredentialsStoreFactory();

    private final ConcurrentMap<String, DataStore<String, Credentials>> dataStores;

    /**
     * Basic constructor that initializes an empty {@link DataStore} cache.
     */
    private CachedFileCredentialsStoreFactory() {
        this.dataStores = new ConcurrentHashMap<>();
    }

    public static CachedFileCredentialsStoreFactory getInstance() {
        return INSTANCE;
    }

    /**
     * {@inheritDoc}
     * <p>
     * If no store already exists for the given key, a new one is created, associated with the passed {@code id} and returned.
     * </p>
     */
    @Override
    public DataStore<String, Credentials> getDataStore(String id) {
        DataStore<String, Credentials> store = dataStores.get(id);
        if (store == null) {
            final DataStore<String, Credentials> dataStore = new CachedFileCredentialsStore(new File(Constants.CREDENTIAL_STORE_FILE));
            store = dataStores.putIfAbsent(id, dataStore);
            if (store == null) {
                store = dataStore;
            }
        }
        return store;
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Cannot clone singleton.");
    }
}
