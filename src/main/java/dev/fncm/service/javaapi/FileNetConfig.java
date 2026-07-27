package dev.fncm.service.javaapi;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * FileNet connection settings, sourced from MicroProfile Config.
 *
 * All properties are read from microprofile-config.properties (or overridden
 * by the corresponding environment variable — MicroProfile Config converts
 * property keys automatically, e.g. {@code FILENET_CPE_URL} overrides
 * {@code filenet.cpe.url}).
 */
@ApplicationScoped
public class FileNetConfig {

    @Inject
    @ConfigProperty(name = "filenet.cpe.url")
    String cpeUrl;

    @Inject
    @ConfigProperty(name = "filenet.domain")
    String domain;

    @Inject
    @ConfigProperty(name = "filenet.objectstore")
    String objectStore;

    @Inject
    @ConfigProperty(name = "filenet.stanza", defaultValue = "FileNetP8WSI")
    String stanza;

    // Getters
    public String getUrl()         { return cpeUrl; }
    public String getDomain()      { return domain; }
    public String getObjectStore() { return objectStore; }
    public String getStanza()      { return stanza; }

    @Override
    public String toString() {
        return "FileNetConfig{"
                + "cpeUrl='" + cpeUrl + '\''
                + ", domain='" + domain + '\''
                + ", objectStore='" + objectStore + '\''
                + ", stanza='" + stanza + '\''
                + '}';
    }
}

// Made with Bob
