package dev.fncm.service.javaapi;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Scanner;

/**
 * Configuration class for FileNet connection settings.
 * Supports loading from:
 * 1. Configuration file (filenet.properties)
 * 2. Environment variables (overrides file settings)
 * 3. Command-line arguments (overrides both)
 */
public class FileNetConfig {
    private static final String CONFIG_FILE = "filenet-cli.properties";
    
    // Configuration keys
    private static final String KEY_URL = "filenet.url";
    private static final String KEY_DOMAIN = "filenet.domain";
    private static final String KEY_OBJECT_STORE = "filenet.objectstore";
    private static final String KEY_USERNAME = "filenet.username";
    private static final String KEY_PASSWORD = "filenet.password";
    private static final String KEY_STANZA = "filenet.stanza";
    private static final String KEY_IAMHOST = "filenet.iamhost";
    private static final String KEY_CP4BAHOST = "filenet.cp4bahost";

    // Environment variable names
    private static final String ENV_URL = "FILENET_URL";
    private static final String ENV_DOMAIN = "FILENET_DOMAIN";
    private static final String ENV_OBJECT_STORE = "FILENET_OBJECTSTORE";
    private static final String ENV_USERNAME = "FILENET_USERNAME";
    private static final String ENV_PASSWORD = "FILENET_PASSWORD";
    private static final String ENV_STANZA = "FILENET_STANZA";
    private static final String ENV_IAMHOST = "FILENET_IAMHOST";
    private static final String ENV_CP4BAHOST = "FILENET_CP4BAHOST";

    private String url;
    private String domain;
    private String objectStore;
    private String username;
    private String password;
    private String stanza;
    private String iamhost;
    private String cp4bahost;
    
    /**
     * Load configuration from file and environment variables.
     * Environment variables override file settings.
     */
    public static FileNetConfig load() {
        FileNetConfig config = new FileNetConfig();
        
        // Load from properties file if it exists
        Properties props = new Properties();
        try (InputStream input = FileNetConfig.class.getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            props.load(input);
            System.out.println("✓ Loaded configuration from " + CONFIG_FILE);
        } catch (IOException e) {
            System.out.println("ℹ Configuration file not found: " + CONFIG_FILE);
            System.out.println("  Using environment variables or command-line arguments");
        }
        
        // Load settings (environment variables override file settings)
        config.url = getConfigValue(props, KEY_URL, ENV_URL);
        config.objectStore = getConfigValue(props, KEY_OBJECT_STORE, ENV_OBJECT_STORE);
        config.username = getConfigValue(props, KEY_USERNAME, ENV_USERNAME);
        config.password = getConfigValue(props, KEY_PASSWORD, ENV_PASSWORD);
        config.stanza = getConfigValue(props, KEY_STANZA, ENV_STANZA, "FileNetP8WSI");
        config.iamhost = getConfigValue(props, KEY_IAMHOST, ENV_IAMHOST);
        config.cp4bahost = getConfigValue(props, KEY_CP4BAHOST, ENV_CP4BAHOST);
        config.domain = getConfigValue(props, KEY_DOMAIN, ENV_DOMAIN); 

        return config;
    }


    /**
     * Get configuration value from environment variable or properties file.
     * Environment variable takes precedence.
     */
    private static String getConfigValue(Properties props, String propKey, String envKey) {
        return getConfigValue(props, propKey, envKey, null);
    }
    
    /**
     * Get configuration value with default fallback.
     * Trims whitespace and removes surrounding quotes from values.
     */
    private static String getConfigValue(Properties props, String propKey, String envKey, String defaultValue) {
        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isEmpty()) {
            return cleanValue(envValue);
        }
        
        String propValue = props.getProperty(propKey);
        if (propValue != null && !propValue.isEmpty()) {
            return cleanValue(propValue);
        }
        
        return defaultValue;
    }
    
    /**
     * Clean configuration value by trimming whitespace and removing surrounding quotes.
     */
    private static String cleanValue(String value) {
        if (value == null) {
            return null;
        }
        
        // Trim whitespace
        value = value.trim();
        
        // Remove surrounding quotes (both single and double)
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            if (value.length() >= 2) {
                value = value.substring(1, value.length() - 1);
            }
        }
        
        return value;
    }
    
    /**
     * Override configuration with command-line arguments.
     */
    public void overrideFromArgs(String[] args) {
        if (args.length >= 1 && args[0] != null && !args[0].isEmpty()) {
            this.url = args[0];
        }
        if (args.length >= 2 && args[1] != null && !args[1].isEmpty()) {
            this.objectStore = args[1];
        }
        if (args.length >= 3 && args[2] != null && !args[2].isEmpty()) {
            this.username = args[2];
        }
        if (args.length >= 4 && args[3] != null && !args[3].isEmpty()) {
            this.password = args[3];
        }
    }
    
    /**
     * Validate that all required configuration is present.
     * When iamhost+cp4bahost are set the token flow is used and username/password
     * are still required (they are the credentials sent to the IAM endpoint).
     */
    public void validate() throws IllegalStateException {
        if (url == null || url.isEmpty()) {
            throw new IllegalStateException("FileNet URL is required. Set via config file, environment variable " + ENV_URL + ", or command-line argument.");
        }
        if (objectStore == null || objectStore.isEmpty()) {
            throw new IllegalStateException("Object Store name is required. Set via config file, environment variable " + ENV_OBJECT_STORE + ", or command-line argument.");
        }
        if (username == null || username.isEmpty()) {
            throw new IllegalStateException("Username is required. Set via config file, environment variable " + ENV_USERNAME + ", or command-line argument.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalStateException("Password is required. Set via config file, environment variable " + ENV_PASSWORD + ", or command-line argument.");
        }
    }

    /**
     * Validate configuration for OAuth/token-based flows where a password is not needed.
     * Requires url, objectStore, and username only.
     */
    public void validateForOAuth() throws IllegalStateException {
        if (url == null || url.isEmpty()) {
            throw new IllegalStateException("FileNet URL is required. Set via config file, environment variable " + ENV_URL + ", or command-line argument.");
        }
        if (objectStore == null || objectStore.isEmpty()) {
            throw new IllegalStateException("Object Store name is required. Set via config file, environment variable " + ENV_OBJECT_STORE + ", or command-line argument.");
        }
        if (username == null || username.isEmpty()) {
            throw new IllegalStateException("Username is required. Set via config file, environment variable " + ENV_USERNAME + ", or command-line argument.");
        }
    }
    
    // Getters
    public String getUrl() { return url; }
    public String getObjectStore() { return objectStore; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getStanza() { return stanza; }
    public String getIamhost() { return iamhost; }
    public String getCp4bahost() { return cp4bahost; }
    public String getDomain() { return domain;}

    // Setters (for programmatic configuration)
    public void setUrl(String url) { this.url = url; }
    public void setObjectStore(String objectStore) { this.objectStore = objectStore; }
    public void setUsername(String username) { this.username = username; }
    public void setPassword(String password) { this.password = password; }
    public void setStanza(String stanza) { this.stanza = stanza; }
    public void setIamhost(String iamhost) { this.iamhost = iamhost; }
    public void setCp4bahost(String cp4bahost) { this.cp4bahost = cp4bahost; }

    @Override
    public String toString() {
        return "FileNetConfig{" +
                "url='" + url + '\'' +
                ", domain='" + domain + '\'' +
                ", objectStore='" + objectStore + '\'' +
                ", username='" + username + '\'' +
                ", password='***'" +
                ", stanza='" + stanza + '\'' +
                ", iamhost='" + iamhost + '\'' +
                ", cp4bahost='" + cp4bahost + '\'' +
                '}';
    }
}

// Made with Bob
