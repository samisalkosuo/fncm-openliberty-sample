package dev.fncm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Initialized;
import jakarta.enterprise.event.Observes;
import java.util.logging.Logger;

/**
 * Application-scoped initializer that runs once when the application starts.
 * Logs startup confirmation; FileNetConfig is now a CDI bean managed automatically.
 */
@ApplicationScoped
public class AppInitializer {

    private static final Logger logger = Logger.getLogger(AppInitializer.class.getName());

    /**
     * Eagerly triggered at application startup via CDI application context
     * initialization event — fires before any request is served.
     */
    public void onStart(@Observes @Initialized(ApplicationScoped.class) Object event) {
        logger.info("AppInitializer: starting up...");
    }
}

// Made with Bob
