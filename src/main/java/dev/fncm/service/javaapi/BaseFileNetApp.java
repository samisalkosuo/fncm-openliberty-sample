package dev.fncm.service.javaapi;

import com.filenet.api.authentication.Credentials;
import com.filenet.api.authentication.OpenTokenCredentials;
import com.filenet.api.authentication.UsernameCredentials;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;

import dev.fncm.service.javaapi.service.ConnectionTest;

import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.*;
import java.security.PrivilegedExceptionAction;
import java.security.cert.X509Certificate;

/**
 * Base abstract class for FileNet CLI applications.
 * Handles connection, authentication, disconnection, and SSL configuration.
 * Subclasses implement the doWork() method to perform specific tasks.
 * 
 * Usage pattern:
 * 1. Extend this class
 * 2. Implement doWork(ObjectStore objectStore) method
 * 3. Call run() from main method
 */
public abstract class BaseFileNetApp<T> {

    private static final Logger LOGGER = Logger.getLogger(BaseFileNetApp.class.getName());

    protected Connection connection;
    protected Domain domain;
    protected ObjectStore objectStore;
    protected FileNetConfig config;
    protected String userName;
    protected String oauthToken;
    private Credentials credentials;  // active Credentials for doAs() wrapping
    
    /**
     * Main execution method. Handles the complete lifecycle:
     * 1. Load configuration
     * 2. Configure SSL
     * 3. Connect to FileNet
     * 4. Authenticate
     * 5. Call doWork() (implemented by subclass)
     * 6. Disconnect and cleanup
     */
    public T run(String[] args) throws Exception{
        try {
            //printHeader();
            
            // Load and validate configuration
            // Validation is the responsibility of loadConfiguration() and its overrides.
            config = loadConfiguration(args);
            
            // Configure SSL
            configureTrustAllSSL();
            
            // Authenticate — stores credentials for doAs() below
            LOGGER.info("Doing authentication");
            authenticate();

            // All CE API calls must run inside credentials.doAs() so the token
            // is attached to every WSI HTTP request (mirrors oauth-snippet.java).
            // connect() is also inside doAs because the Connection must be
            // obtained within the credentials scope to carry the auth context.
            final String[] appArgs = extractApplicationArgs(args);
            T result = credentials.doAs((PrivilegedExceptionAction<T>) () -> {

                // Connect and get domain and object store
                connect();
                fetchDomainAndObjectStore();

                // Execute subclass-specific work
                LOGGER.info("=================================================");
                LOGGER.info("Executing Application Logic");
                LOGGER.info("=================================================");
                LOGGER.info("");

                return doWork(objectStore, appArgs);
            });
            
            LOGGER.info("");
            LOGGER.info("✓ Application completed successfully!");
            return result;
            
        } catch (Exception e) {
            LOGGER.severe("");
            LOGGER.severe("=================================================");
            LOGGER.severe("ERROR: Application failed");
            LOGGER.severe("=================================================");
            LOGGER.severe("Error Message: " + e.getMessage());
            LOGGER.severe("");
            LOGGER.log(Level.SEVERE,e.getMessage(), e);
            throw e;            
            
            
        } finally {
            disconnect();
        }
    }
    
    /**
     * Extract application-specific arguments from the command line.
     *
     * If 4+ arguments are provided, the first 4 are configuration (url, objectStore, username, password)
     * and the rest are application-specific.
     *
     * If fewer than 4 arguments are provided, configuration comes from the config file,
     * and ALL arguments are application-specific.
     *
     * @param args All command-line arguments
     * @return Application-specific arguments (empty array if none)
     */
    protected String[] extractApplicationArgs(String[] args) {
        String[] appArgs;
        
        if (args.length >= 4) {
            // First 4 args are configuration, rest are application args
            if (args.length > 4) {
                appArgs = new String[args.length - 4];
                System.arraycopy(args, 4, appArgs, 0, appArgs.length);
            } else {
                appArgs = new String[0];
            }
        } else {
            // All args are application args (config comes from file)
            appArgs = args.clone();
        }
        
        if (appArgs.length > 0) {
            LOGGER.info("Application arguments provided: " + appArgs.length);
            for (int i = 0; i < appArgs.length; i++) {
                LOGGER.info("  arg[" + i + "]: " + appArgs[i]);
            }
            LOGGER.info("");
        }
        
        return appArgs;
    }
    
    /**
     * Load configuration from file, environment variables, and command-line arguments.
     * Can be overridden by subclasses for custom configuration loading.
     */
    protected FileNetConfig loadConfiguration(String[] args) {
        LOGGER.info("Loading configuration...");
        FileNetConfig config = FileNetConfig.load();
        
        // Only override with command-line arguments if at least 4 args provided
        // (url, objectStore, username, password)
        // Otherwise, args are application-specific, not configuration
        if (args.length >= 4) {
            config.overrideFromArgs(args);
            LOGGER.info("✓ Configuration overridden with command-line arguments");
        }
        
        config.validate();
        LOGGER.info("✓ Configuration loaded");
        LOGGER.info("");
        return config;
    }
    
    /**
     * Connect to FileNet Content Engine.
     */
    protected void connect() throws Exception {
        LOGGER.info("Connecting to FileNet Content Engine...");
        LOGGER.info("URL: " + config.getUrl());
        connection = Factory.Connection.getConnection(config.getUrl());
        LOGGER.info("✓ Connection established");
        LOGGER.info("");
    }
    
    /**
     * Authenticate with FileNet.
     * Stores the Credentials instance in this.credentials; all CE API calls
     * must be wrapped in credentials.doAs() — see run() and oauth-snippet.java.
     */
    protected void authenticate() throws Exception {
            LOGGER.info("Authenticating with OAuth Bearer Token...");
            LOGGER.info("  Token length : " + oauthToken.length());
            LOGGER.info("  Token preview: " + oauthToken.substring(0, Math.min(40, oauthToken.length())) + "...");
            // realm=null matches the oauth-snippet.java example
            credentials = new OpenTokenCredentials(userName, oauthToken, null);
        LOGGER.info("✓ Authentication successful");
        LOGGER.info("");
    }
    
    /**
     * Fetch domain and object store information.
     */
    protected void fetchDomainAndObjectStore() throws Exception {
        LOGGER.info("Fetching Domain information...");        
        domain = Factory.Domain.fetchInstance(connection, config.getDomain(), null);

        LOGGER.info("Domain Name: " + domain.get_Name());
        LOGGER.info("Domain ID: " + domain.get_Id());
        LOGGER.info("");
        
        LOGGER.info("Fetching Object Store information...");
        LOGGER.info("Object Store Name: " + config.getObjectStore());
        objectStore = Factory.ObjectStore.fetchInstance(domain, config.getObjectStore(), null);
        LOGGER.info("Object Store Display Name: " + objectStore.get_DisplayName());
        LOGGER.info("Object Store ID: " + objectStore.get_Id());
        LOGGER.info("");
    }
    
    /**
     * Disconnect and cleanup resources.
     */
    protected void disconnect() {
        if (connection != null) {
            try {
                LOGGER.info("");
                LOGGER.info("Closing connection...");
                credentials = null;
                LOGGER.info("✓ Connection closed");
            } catch (Exception e) {
                LOGGER.severe("Warning: Error closing connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Configure SSL to trust all certificates.
     * WARNING: This is for development/testing only. Do not use in production!
     */
    protected void configureTrustAllSSL() {
        try {
            LOGGER.info("Configuring SSL to trust all certificates...");
            LOGGER.info("WARNING: This is for development only!");
            
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };
            
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
            
            LOGGER.info("✓ SSL configured");
            LOGGER.info("");
            
        } catch (Exception e) {
            LOGGER.severe("ERROR: Failed to configure SSL: " + e.getMessage());
            throw new RuntimeException("SSL configuration failed", e);
        }
    }
    
    /**
     * Print application header.
     * Can be overridden by subclasses for custom headers.
     */
    protected void printHeader() {
        LOGGER.info("=================================================");
        LOGGER.info(getApplicationName());
        LOGGER.info("=================================================");
        LOGGER.info("");
    }
    
    /**
     * Print usage information.
     * Can be overridden by subclasses for custom usage messages.
     */
    protected void printUsage() {
        LOGGER.info("Usage: java " + this.getClass().getSimpleName() + " [url] [objectStore] [username] [password]");
        LOGGER.info("");
        LOGGER.info("Configuration can be provided via:");
        LOGGER.info("  1. Configuration file: filenet.properties");
        LOGGER.info("  2. Environment variables: FILENET_URL, FILENET_OBJECTSTORE, FILENET_USERNAME, FILENET_PASSWORD");
        LOGGER.info("  3. Command-line arguments (overrides above)");
        LOGGER.info("");
        LOGGER.info("Example:");
        LOGGER.info("  java " + this.getClass().getSimpleName() + " https://server/wsi/FNCEWS40MTOM/ OS01 admin password");
    }
    
    /**
     * Get the application name for display.
     * Should be overridden by subclasses.
     */
    protected String getApplicationName() {
        return "FileNet Application";
    }
    
    /**
     * Perform the actual work with the FileNet ObjectStore.
     * This method must be implemented by subclasses.
     *
     * @param objectStore The connected and authenticated ObjectStore instance
     * @param args Application-specific arguments (after configuration args)
     * @throws Exception if any error occurs during work execution
     */
    protected abstract T doWork(ObjectStore objectStore, String[] args) throws Exception;
}

// Made with Bob
