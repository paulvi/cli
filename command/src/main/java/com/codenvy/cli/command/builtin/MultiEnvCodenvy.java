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
package com.codenvy.cli.command.builtin;

import com.codenvy.cli.command.builtin.model.DefaultUserBuilderStatus;
import com.codenvy.cli.command.builtin.model.DefaultUserProject;
import com.codenvy.cli.command.builtin.model.DefaultUserRunnerStatus;
import com.codenvy.cli.command.builtin.model.DefaultUserWorkspace;
import com.codenvy.cli.command.builtin.model.UserBuilderStatus;
import com.codenvy.cli.command.builtin.model.UserProject;
import com.codenvy.cli.command.builtin.model.UserRunnerStatus;
import com.codenvy.cli.preferences.Preferences;
import com.codenvy.cli.security.EnvironmentCredentials;
import com.codenvy.cli.security.PreferencesDataStore;
import com.codenvy.cli.security.TokenRetrieverDatastore;
import com.codenvy.client.Codenvy;
import com.codenvy.client.CodenvyClient;
import com.codenvy.client.CodenvyException;
import com.codenvy.client.Request;
import com.codenvy.client.WorkspaceClient;
import com.codenvy.client.auth.CodenvyAuthenticationException;
import com.codenvy.client.auth.Credentials;
import com.codenvy.client.auth.Token;
import com.codenvy.client.model.BuilderStatus;
import com.codenvy.client.model.Project;
import com.codenvy.client.model.RunnerStatus;
import com.codenvy.client.model.User;
import com.codenvy.client.model.Workspace;
import com.codenvy.client.model.WorkspaceReference;

import org.fusesource.jansi.Ansi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.fusesource.jansi.Ansi.Attribute.INTENSITY_BOLD;
import static org.fusesource.jansi.Ansi.Attribute.INTENSITY_BOLD_OFF;
import static org.fusesource.jansi.Ansi.Color.RED;

/**
 * @author Florent Benoit
 */
public class MultiEnvCodenvy {

    private CodenvyClient codenvyClient;

    private ConcurrentMap<String, Codenvy> readyEnvironments;
    private ConcurrentMap<String, Environment> availableEnvironments;

    private Preferences globalPreferences;

    public MultiEnvCodenvy(CodenvyClient codenvyClient, Preferences globalPreferences) {
        this.codenvyClient = codenvyClient;
        this.globalPreferences = globalPreferences;
        this.readyEnvironments = new ConcurrentHashMap<>();
        this.availableEnvironments = new ConcurrentHashMap<>();
        init();
    }

    protected void init() {
        readyEnvironments.clear();
        availableEnvironments.clear();
        // now read envionments and add a new datastore for each env
        Map preferencesEnvironments = globalPreferences.get("environments", Map.class);
        if (preferencesEnvironments != null) {
            Iterator<String> environmentIterator = preferencesEnvironments.keySet().iterator();
            Preferences environmentsPreferences = globalPreferences.path("environments");
            while (environmentIterator.hasNext()) {
                String environment = environmentIterator.next();
                // create store
                PreferencesDataStore preferencesDataStore = new PreferencesDataStore(environmentsPreferences, environment, codenvyClient);

                // read environment
                Environment environmentData = environmentsPreferences.get(environment, Environment.class);
                EnvironmentCredentials environmentCredentials = environmentsPreferences.get(environment, EnvironmentCredentials.class);

                // If token is available, add it
                if (!environmentCredentials.getToken().isEmpty()) {
                    // add remote env
                    // Manage credentials
                    Codenvy codenvy = codenvyClient.newCodenvyBuilder(environmentData.getUrl(), environmentCredentials.getUsername())
                                                   .withCredentialsProvider(preferencesDataStore)
                                                   .withCredentialsStoreFactory(preferencesDataStore)
                                                   .build();
                    readyEnvironments.put(environment, codenvy);
                }

                availableEnvironments.put(environment, environmentData);

            }
        }
    }


    protected List<UserProject> getProjects() {
        List<UserProject> projects = new ArrayList<>();

        Set<Map.Entry<String, Codenvy>> entries = readyEnvironments.entrySet();
        Iterator<Map.Entry<String, Codenvy>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Codenvy> entry = iterator.next();
            try {
                List<UserProject> foundProjects = getProjects(entry.getValue());
                if (foundProjects.size() > 0) {
                    projects.addAll(foundProjects);
                }
            } catch (CodenvyAuthenticationException e) {
                System.err.println("Authentication problem on environment '" + entry.getKey() + "'");
            }
        }
        return projects;
    }


    /**
     * Gets list of all projects for the current user
     *
     * @param codenvy
     *         the codenvy object used to retrieve the data
     * @return the list of projects
     */
    protected List<UserProject> getProjects(Codenvy codenvy) {
        List<UserProject> projects = new ArrayList<>();

        // For each workspace, search the project and compute

        WorkspaceClient workspaceClient = codenvy.workspace();
        Request<List<? extends Workspace>> request = workspaceClient.all();
        List<? extends Workspace> readWorkspaces = request.execute();

        for (Workspace workspace : readWorkspaces) {
            WorkspaceReference ref = workspace.workspaceReference();
            // Now skip all temporary workspaces
            if (ref.isTemporary()) {
                continue;
            }

            DefaultUserWorkspace defaultUserWorkspace = new DefaultUserWorkspace(codenvy, ref);

            List<? extends Project> readProjects = codenvy.project().getWorkspaceProjects(ref.id()).execute();
            for (Project readProject : readProjects) {
                DefaultUserProject project = new DefaultUserProject(codenvy, readProject, defaultUserWorkspace);
                projects.add(project);
            }
        }
        return projects;
    }


    /**
     * Allows to search a project
     */
    protected UserProject getProject(String shortId) {
        if (shortId == null || shortId.length() < 2) {
            throw new IllegalArgumentException("The identifier should at least contain two digits");
        }


        // get all projects
        List<UserProject> projects = getProjects();

        // no projects
        if (projects.size() == 0) {
            return null;
        }

        // now search in the given projects
        List<UserProject> matchingProjects = new ArrayList<>();
        for (UserProject project : projects) {
            // match
            if (project.shortId().startsWith(shortId)) {
                matchingProjects.add(project);
            }
        }

        // No matching project
        if (matchingProjects.size() == 0) {
            return null;
        } else if (matchingProjects.size() == 1) {
            // one matching project
            return matchingProjects.get(0);
        } else {
            throw new IllegalArgumentException("Too many matching projects. Try with a longer identifier");
        }


    }

    public boolean hasAvailableEnvironments() {
        return !availableEnvironments.isEmpty();
    }

    public boolean hasReadyEnvironments() {
        return !readyEnvironments.isEmpty();
    }

    public Collection<String> getEnvironmentNames() {
        return availableEnvironments.keySet();
    }
    public Map<String, Environment> getAvailableEnvironments() {
        return availableEnvironments;
    }

    public String listEnvironments() {
        Ansi buffer = Ansi.ansi();
        buffer.a(INTENSITY_BOLD).a("ENVIRONMENTS\n").a(INTENSITY_BOLD_OFF);
        buffer.reset();

        Map<String, Environment> envs = getAvailableEnvironments();
        if (envs.size() == 1) {
            buffer.a("There is ").a(envs.size()).a(" Codenvy environment:");
        } else if (envs.size() > 1) {
            buffer.a("There are ").a(envs.size()).a(" Codenvy environments:");
        } else {
            buffer.a("There is no Codenvy environment.");
        }
        buffer.a(System.lineSeparator());
        Iterator<Map.Entry<String, Environment>> it = envs.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Environment> entry = it.next();
            buffer.a(entry.getKey()).a("  [").a(entry.getValue().getUrl()).a("]");
            if (entry.getValue().isDefaultEnv()) {
                buffer.a("*");
            }
            buffer.a(System.lineSeparator());
        }
        return buffer.toString();
    }


    public boolean addRemote(String name, String url) {
        // check env doesn't exists
        if (getEnvironmentNames().contains(name)) {
            System.out.println("The environment with name '" + name + "' already exists");
            return false;
        }

        Preferences preferencesEnvironments = globalPreferences.path("environments");


        // add the new environment
        Environment environment = new Environment();
        environment.setUrl(url);
        preferencesEnvironments.put(name, environment);

        // refresh current links
        refresh();

        return true;

    }

    public Environment getEnvironment(String environmentName) {
        // it exists, get it
        Preferences preferencesEnvironments = globalPreferences.path("environments");


        if (!getEnvironmentNames().contains(environmentName)) {
            return null;
        }

        return preferencesEnvironments.get(environmentName, Environment.class);
    }


    public Environment getDefaultEnvironment() {
        Map preferencesEnvironments = globalPreferences.get("environments", Map.class);
        if (preferencesEnvironments != null) {
            Iterator<String> environmentIterator = preferencesEnvironments.keySet().iterator();
            Preferences environmentsPreferences = globalPreferences.path("environments");
            while (environmentIterator.hasNext()) {
                String environment = environmentIterator.next();

                Environment tmpEnv = environmentsPreferences.get(environment, Environment.class);
                if (tmpEnv.isDefaultEnv()) {
                    return tmpEnv;
                }
            }
        }
        return null;
    }

    public String getDefaultEnvironmentName() {
        Map preferencesEnvironments = globalPreferences.get("environments", Map.class);
        if (preferencesEnvironments != null) {
            Iterator<String> environmentIterator = preferencesEnvironments.keySet().iterator();
            Preferences environmentsPreferences = globalPreferences.path("environments");
            while (environmentIterator.hasNext()) {
                String environment = environmentIterator.next();

                Environment tmpEnv = environmentsPreferences.get(environment, Environment.class);
                if (tmpEnv.isDefaultEnv()) {
                    return environment;
                }
            }
        }
        return null;
    }


    public boolean login(String environmentName, String username, String password) {

        // get URL of the environment
        Environment environment;

        if (environmentName == null) {
            environment = getDefaultEnvironment();
            if (environment == null) {
                System.out.println("No default environment found'");
                return false;
            }
            environmentName = getDefaultEnvironmentName();
        } else {
            environment = getEnvironment(environmentName);
        }

        if (environment == null) {
            System.out.println("Unable to find the given environment '" + environmentName + "'");
            return false;
        }


        String url = environment.getUrl();

        TokenRetrieverDatastore tokenRetrieverDatastore = new TokenRetrieverDatastore();

        // check that this is valid
        Credentials codenvyCredentials = codenvyClient.newCredentialsBuilder()
                                                      .withUsername(username)
                                                      .withPassword(password)
                                                      .build();
        Codenvy codenvy = codenvyClient.newCodenvyBuilder(url, username)
                                       .withCredentials(codenvyCredentials)
                                       .withCredentialsStoreFactory(tokenRetrieverDatastore)
                                       .build();

        // try to connect to the remote side
        try {
            codenvy.user().current().execute();
        } catch (CodenvyException e) {
            System.out.println("Unable to authenticate for the given credentials on URL '" + url + "'. Check the username and password.");
            // invalid login so we don't add env
            return false;
        }

        // get token
        Token token = tokenRetrieverDatastore.getToken();
        if (token == null) {
            System.out.println("Unable to get token for the given credentials on URL '" + url + "'");
            // invalid login so we don't add env
            return false;
        }


        // Save credentials
        Preferences preferencesEnvironments = globalPreferences.path("environments");
        EnvironmentCredentials environmentCredentials = new EnvironmentCredentials();
        environmentCredentials.setToken(token.value());
        environmentCredentials.setUsername(username);
        // by merging
        preferencesEnvironments.merge(environmentName, environmentCredentials);

        // refresh current links
        refresh();

        return true;
    }


    protected void refresh() {
        init();
    }


    protected boolean removeEnvironment(String name) {
        // check env does exists
        if (!getEnvironmentNames().contains(name)) {
            System.out.println("The environment with name '" + name + "' does not exists");
            return false;
        }

        // it exists, remove it
        Preferences preferencesEnvironments = globalPreferences.path("environments");

        // delete
        preferencesEnvironments.delete(name);

        // refresh current links
        refresh();

        // OK
        return true;
    }

    public List<UserBuilderStatus> findBuilders(String builderID) {
        // first collect all processes
        List<UserProject> projects = getProjects();

        List<UserBuilderStatus> matchingStatuses = new ArrayList<>();

        // then for each project, gets the builds IDs
        for (UserProject userProject : projects) {
            final List<? extends BuilderStatus> builderStatuses = userProject.getCodenvy().builder().builds(userProject.getInnerProject()).execute();
            for (BuilderStatus builderStatus : builderStatuses) {

                UserBuilderStatus tmpStatus = new DefaultUserBuilderStatus(builderStatus, userProject);
                if (tmpStatus.shortId().startsWith(builderID)) {
                    matchingStatuses.add(tmpStatus);
                }
            }
        }
        return matchingStatuses;
    }


    public List<UserRunnerStatus> findRunners(String runnerID) {
        List<UserProject> projects = getProjects();

        List<UserRunnerStatus> matchingStatuses = new ArrayList<>();

        // then for each project, gets the process IDs
        for (UserProject userProject : projects) {
            final List<? extends RunnerStatus> runnerStatuses =
                    userProject.getCodenvy().runner().processes(userProject.getInnerProject()).execute();
            for (RunnerStatus runnerStatus : runnerStatuses) {

                UserRunnerStatus tmpStatus = new DefaultUserRunnerStatus(runnerStatus, userProject);
                if (tmpStatus.shortId().startsWith(runnerID)) {
                    matchingStatuses.add(tmpStatus);
                }
            }
        }

        return matchingStatuses;
    }

    public List<UserRunnerStatus> getRunners(UserProject userProject) {
        List<UserRunnerStatus> statuses = new ArrayList<>();
        final List<? extends RunnerStatus> runnerStatuses = userProject.getCodenvy().runner().processes(userProject.getInnerProject()).execute();
        for (RunnerStatus runnerStatus : runnerStatuses) {

            UserRunnerStatus tmpStatus = new DefaultUserRunnerStatus(runnerStatus, userProject);
                statuses.add(tmpStatus);
        }
        return statuses;
    }

    public List<UserBuilderStatus> getBuilders(UserProject userProject) {
        List<UserBuilderStatus> statuses = new ArrayList<>();
        final List<? extends BuilderStatus> builderStatuses = userProject.getCodenvy().builder().builds(userProject.getInnerProject()).execute();
        for (BuilderStatus builderStatus : builderStatuses) {

            UserBuilderStatus tmpStatus = new DefaultUserBuilderStatus(builderStatus, userProject);
            statuses.add(tmpStatus);
        }
        return statuses;
    }


    protected static <T> T checkOnlyOne(List<T> list, String id, String textNoIdentifier, String textTooManyIDs) {
        if (list.size() == 0) {
            errorNoIdentifier(id, textNoIdentifier);
            return null;
        } else if (list.size() > 1) {
            errorTooManyIdentifiers(id, textTooManyIDs);
            return null;
        }

        return list.get(0);
    }


    /**
     * Display error if there are too many identifiers that have been found
     * @param text a description of the identifier
     */
    protected static void errorTooManyIdentifiers(String id, String text) {
        Ansi buffer = Ansi.ansi();
        buffer.fg(RED);
        buffer.a("Too many ").a(text).a(" have been found with identifier '").a(id).a("'. Please add extra data to the identifier");
        buffer.reset();
        System.out.println(buffer.toString());
    }

    /**
     * Display error if no identifier has been found
     * @param text a description of the identifier
     */
    protected static void errorNoIdentifier(String id, String text) {
        Ansi buffer = Ansi.ansi();
        buffer.fg(RED);
        buffer.a("No ").a(text).a(" found with identifier '").a(id).a("'.");
        buffer.reset();
        System.out.println(buffer.toString());
    }



}