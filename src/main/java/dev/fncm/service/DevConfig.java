package dev.fncm.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

/**
 * Optional dev-only credentials sourced from MicroProfile Config.
 *
 * Set {@code DEV_USER_NAME} and {@code DEV_USER_PASSWORD} environment variables
 * to have the login form pre-filled automatically in the browser. Both are
 * absent by default so normal (non-dev) deployments are unaffected.
 */
@ApplicationScoped
public class DevConfig {

    @Inject
    @ConfigProperty(name = "dev.username")
    Optional<String> username;

    @Inject
    @ConfigProperty(name = "dev.password")
    Optional<String> password;

    public Optional<String> getUsername() { return username; }
    public Optional<String> getPassword() { return password; }
}
