package dev.fncm.service.javaapi.service;

import java.util.logging.Logger;

import com.filenet.api.core.ObjectStore;

import dev.fncm.service.javaapi.BaseFileNetApp;
import dev.fncm.service.javaapi.FileNetConfig;

public class ConnectionTest extends BaseFileNetApp<String> {

    private static final Logger logger = Logger.getLogger(ConnectionTest.class.getName());

    public ConnectionTest(String userName, String oauthToken, FileNetConfig config) {
        logger.info("ConnectionTest initializer");
        this.userName = userName;
        this.oauthToken = oauthToken;
        this.config = config;
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

        logger.info("=================================================");
        logger.info("Connection Summary");
        logger.info("=================================================");
        logger.info("Status: CONNECTED");
        logger.info("Domain: " + domain.get_Name());
        logger.info("Object Store: " + objectStore.get_DisplayName());
        logger.info("User: " + this.userName);
        logger.info("=================================================");

        logger.info("✓ FileNet connection test completed successfully!");
        return sb.toString();
    }
}

// Made with Bob
