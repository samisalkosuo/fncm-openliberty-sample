package dev.fncm.service.javaapi;

import com.filenet.api.authentication.OpenTokenCredentials;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.ObjectStore;

import dev.fncm.auth.TokenContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import dev.fncm.utils.SslUtil;

import java.security.GeneralSecurityException;
import java.security.PrivilegedExceptionAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central service that owns the FileNet JACE connection lifecycle:
 * SSL configuration, authentication, connect, credentials.doAs(), and disconnect.
 * <p>
 * Resource classes inject this bean and call {@link #run(FileNetOperation, TokenContext)}.
 * They never touch JACE directly.
 */
@ApplicationScoped
public class FileNetService {

    private static final Logger LOGGER = Logger.getLogger(FileNetService.class.getName());

    @Inject
    FileNetConfig config;

    /**
     * Executes {@code op} inside a fully-authenticated FileNet session.
     * Handles SSL, connect, {@code credentials.doAs()}, and disconnect.
     *
     * @param op  the operation to run
     * @param ctx the current request's token context (provides username + zen token)
     * @return the result of {@code op.execute()}
     * @throws Exception if the operation or any lifecycle step fails
     */
    public <T> T run(FileNetOperation<T> op, TokenContext ctx) throws Exception {
        String username = ctx.getUsername();
        String zenToken = ctx.getZenToken();

        try {
            configureTrustAllSSL();

            LOGGER.info("Authenticating with OAuth Bearer Token...");
            OpenTokenCredentials credentials = new OpenTokenCredentials(username, zenToken, null);
            LOGGER.info("✓ Authentication successful");

            T result = credentials.doAs((PrivilegedExceptionAction<T>) () -> {
                LOGGER.info("Connecting to FileNet Content Engine: " + config.getUrl());
                Connection conn = Factory.Connection.getConnection(config.getUrl());
                LOGGER.info("✓ Connection established");

                LOGGER.info("Fetching domain: " + config.getDomain());
                Domain domain = Factory.Domain.fetchInstance(conn, config.getDomain(), null);

                LOGGER.info("Fetching object store: " + config.getObjectStore());
                ObjectStore os = Factory.ObjectStore.fetchInstance(domain, config.getObjectStore(), null);

                LOGGER.info("=================================================");
                LOGGER.info("Executing operation: " + op.getClass().getSimpleName());
                LOGGER.info("=================================================");

                return op.execute(os, username);
            });

            LOGGER.info("✓ Operation completed successfully");
            return result;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Operation failed: " + e.getMessage(), e);
            throw e;
        }
    }

    // ------------------------------------------------------------------
    // Private helpers
    // ------------------------------------------------------------------

    private void configureTrustAllSSL() {
        try {
            SslUtil.configureGlobalTrustAll(LOGGER);
        } catch (GeneralSecurityException e) {
            throw new RuntimeException("SSL configuration failed", e);
        }
    }
}

// Made with Bob
