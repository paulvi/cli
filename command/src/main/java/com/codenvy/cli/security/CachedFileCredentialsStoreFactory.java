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
public class CachedFileCredentialsStoreFactory implements DataStoreFactory<String, Credentials> {
    private final ConcurrentMap<String, DataStore<String, Credentials>> dataStores;

    /**
     * Basic constructor that initializes an empty {@link DataStore} cache.
     */
    public CachedFileCredentialsStoreFactory() {
        this.dataStores = new ConcurrentHashMap<>();
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

}
