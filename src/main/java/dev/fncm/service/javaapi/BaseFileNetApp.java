package dev.fncm.service.javaapi;

import com.filenet.api.authentication.OpenTokenCredentials;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.*;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509Certificate;

/**
 * Base abstract class for FileNet applications.
 * Handles connection, authentication, disconnection, and SSL configuration.
 * Subclasses implement the doWork() method to perform specific tasks.
 *
 * {@link FileNetConfig} is injected via CDI; no file I/O is performed here.
 */
public abstract class BaseFileNetApp<T> {

    private static final Logger LOGGER = Logger.getLogger(BaseFileNetApp.class.getName());

    protected FileNetConfig config;

    protected Connection connection;
    protected Domain domain;
    protected ObjectStore objectStore;
    protected String userName;
    protected String oauthToken;
    private com.filenet.api.authentication.Credentials credentials;

    /**
     * Main execution method. Handles the complete lifecycle:
     * 1. Configure SSL
     * 2. Authenticate
     * 3. Connect to FileNet and call doWork()
     * 4. Disconnect and cleanup
     */
    public T run(String[] args) throws Exception {
        try {
            configureTrustAllSSL();

            LOGGER.info("Doing authentication");
            authenticate();

            T result = credentials.doAs((PrivilegedExceptionAction<T>) () -> {
                connect();
                fetchDomainAndObjectStore();

                LOGGER.info("=================================================");
                LOGGER.info("Executing Application Logic");
                LOGGER.info("=================================================");

                return doWork(objectStore, args);
            });

            LOGGER.info("✓ Application completed successfully!");
            return result;

        } catch (Exception e) {
            LOGGER.severe("=================================================");
            LOGGER.severe("ERROR: Application failed");
            LOGGER.severe("=================================================");
            LOGGER.severe("Error Message: " + e.getMessage());
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        } finally {
            disconnect();
        }
    }

    /**
     * Connect to FileNet Content Engine.
     */
    protected void connect() throws Exception {
        LOGGER.info("Connecting to FileNet Content Engine...");
        LOGGER.info("URL: " + config.getUrl());
        connection = Factory.Connection.getConnection(config.getUrl());
        LOGGER.info("✓ Connection established");
    }

    /**
     * Authenticate using the OAuth Bearer token stored in {@link #oauthToken}.
     * Stores the Credentials instance for doAs() wrapping.
     */
    protected void authenticate() throws Exception {
        LOGGER.info("Authenticating with OAuth Bearer Token...");
        LOGGER.info("  Token length : " + oauthToken.length());
        LOGGER.info("  Token preview: " + oauthToken.substring(0, Math.min(40, oauthToken.length())) + "...");
        credentials = new OpenTokenCredentials(userName, oauthToken, null);
        LOGGER.info("✓ Authentication successful");
    }

    /**
     * Fetch domain and object store information.
     */
    protected void fetchDomainAndObjectStore() throws Exception {
        LOGGER.info("Fetching Domain information...");
        domain = Factory.Domain.fetchInstance(connection, config.getDomain(), null);
        LOGGER.info("Domain Name: " + domain.get_Name());

        LOGGER.info("Fetching Object Store information...");
        LOGGER.info("Object Store Name: " + config.getObjectStore());
        objectStore = Factory.ObjectStore.fetchInstance(domain, config.getObjectStore(), null);
        LOGGER.info("Object Store Display Name: " + objectStore.get_DisplayName());
    }

    /**
     * Disconnect and cleanup resources.
     */
    protected void disconnect() {
        if (connection != null) {
            credentials = null;
            LOGGER.info("✓ Connection closed");
        }
    }

    /**
     * Configure SSL to trust all certificates.
     * WARNING: This is for development/testing only.
     */
    protected void configureTrustAllSSL() {
        try {
            LOGGER.info("Configuring SSL to trust all certificates...");
            LOGGER.warning("WARNING: This is for development only!");

            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return null; }
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                }
            };

            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            LOGGER.info("✓ SSL configured");

        } catch (Exception e) {
            LOGGER.severe("ERROR: Failed to configure SSL: " + e.getMessage());
            throw new RuntimeException("SSL configuration failed", e);
        }
    }

    /**
     * Perform the actual work with the FileNet ObjectStore.
     * Implemented by subclasses.
     *
     * @param objectStore The connected and authenticated ObjectStore instance
     * @param args        Application-specific arguments
     * @throws Exception if any error occurs during work execution
     */
    protected abstract T doWork(ObjectStore objectStore, String[] args) throws Exception;
}

// Made with Bob
