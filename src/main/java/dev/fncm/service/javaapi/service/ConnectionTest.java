package dev.fncm.service.javaapi.service;


import java.util.logging.Logger;

import com.filenet.api.core.ObjectStore;

import dev.fncm.service.javaapi.BaseFileNetApp;
import dev.fncm.service.javaapi.FileNetConfig;

public class ConnectionTest extends BaseFileNetApp<String> {
    
    private static final Logger logger = Logger.getLogger(ConnectionTest.class.getName());
    
    
    public ConnectionTest(String userName, String oauthToken) {
        logger.info("ConnectionTest initializer");
        this.userName = userName;
        this.oauthToken = oauthToken;
    }

    /**
     * Override loadConfiguration to use OAuth validation — password is not required
     * because this class authenticates via an IAM/OAuth token, not a password.
     */
    @Override
    protected FileNetConfig loadConfiguration(String[] args) {
        logger.info("Loading configuration...");
        FileNetConfig cfg = FileNetConfig.load();
        if (args.length >= 4) {
            cfg.overrideFromArgs(args);
            logger.info("✓ Configuration overridden with command-line arguments");
        }
        cfg.validateForOAuth();
        logger.info("✓ Configuration loaded (OAuth mode — password not required)");
        return cfg;
    }
    
    @Override
    protected String doWork(ObjectStore objectStore, String[] args) throws Exception {
        
        StringBuilder sb = new StringBuilder();
        
        
        sb.append("Status: CONNECTED");
        sb.append('\n');
        sb.append("Domain: " + domain.get_Name());
        sb.append('\n');
        sb.append("Object Store: " + objectStore.get_DisplayName());        
        sb.append('\n');
        sb.append("User: " + this.userName);
        sb.append('\n');

        // Display connection summary
        logger.info("=================================================");
        logger.info("Connection Summary");
        logger.info("=================================================");
        logger.info("Status: CONNECTED");
        logger.info("Domain: " + domain.get_Name());
        logger.info("Object Store: " + objectStore.get_DisplayName());        
        logger.info("User: " + this.userName);
        logger.info("=================================================");
        logger.info("");
        
        // Display any additional arguments passed
        if (args.length > 0) {
            logger.info("Additional arguments received:");
            for (int i = 0; i < args.length; i++) {
                logger.info("  [" + i + "]: " + args[i]);
            }
            logger.info("");
        }
        
        // You can add more work here, such as:
        // - Listing documents
        // - Querying for specific content
        // - Creating folders
        // - etc.
        
        logger.info("✓ FileNet connection test completed successfully!");
        return sb.toString();
    }
}

// Made with Bob
