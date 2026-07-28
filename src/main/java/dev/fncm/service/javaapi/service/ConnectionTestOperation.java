package dev.fncm.service.javaapi.service;

import com.filenet.api.core.ObjectStore;

import dev.fncm.model.ConnectionTestResult;
import dev.fncm.service.javaapi.FileNetOperation;

import java.util.logging.Logger;

/**
 * Tests connectivity by verifying that domain and object-store are reachable.
 * Returns a typed {@link ConnectionTestResult} serialised to JSON by Liberty JSON-B.
 */
public class ConnectionTestOperation implements FileNetOperation<ConnectionTestResult> {

    private static final Logger LOGGER = Logger.getLogger(ConnectionTestOperation.class.getName());

    @Override
    public ConnectionTestResult execute(ObjectStore os, String username) throws Exception {
        String domain = os.get_Domain().get_Name();
        String storeName = os.get_DisplayName();

        LOGGER.info("=================================================");
        LOGGER.info("Connection Summary");
        LOGGER.info("=================================================");
        LOGGER.info("Status: CONNECTED");
        LOGGER.info("Domain: " + domain);
        LOGGER.info("Object Store: " + storeName);
        LOGGER.info("User: " + username);
        LOGGER.info("=================================================");
        LOGGER.info("FileNet connection test completed successfully!");

        return new ConnectionTestResult("CONNECTED", domain, storeName, username);
    }
}

// Made with Bob
