package dev.fncm;

import dev.fncm.service.javaapi.FileNetConfig;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Produces;
import java.util.logging.Logger;

/**
 * Application-scoped initializer that runs once when the application starts.
 * Loads FileNetConfig and makes it available for CDI injection.
 */
@ApplicationScoped
public class AppInitializer {

    private static final Logger logger = Logger.getLogger(AppInitializer.class.getName());

    private FileNetConfig fileNetConfig;

    /**
     * Eagerly triggered at application startup via CDI application context
     * initialization event — fires before any request is served.
     */
    public void onStart(@Observes @Initialized(ApplicationScoped.class) Object event) {
        logger.info("AppInitializer: starting up...");
        fileNetConfig = FileNetConfig.load();
        logger.info("AppInitializer: " + fileNetConfig);
    }

    @Produces
    @ApplicationScoped
    public FileNetConfig getFileNetConfig() {
        return fileNetConfig;
    }
}

// Made with Bob
