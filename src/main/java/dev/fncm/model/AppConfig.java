package dev.fncm.model;

/**
 * Non-sensitive server configuration values included in the login response.
 * These are safe to expose to the browser; credentials and internal URLs are
 * never included.
 *
 * Available in JavaScript as {@code session.config} after a successful login:
 * <pre>
 *   session.config.repositoryIdentifier  // filenet.objectstore
 *   session.config.domain                // filenet.domain
 *   session.config.stanza                // filenet.stanza
 * </pre>
 */
public class AppConfig {

    private String repositoryIdentifier;
    private String domain;
    private String stanza;

    public AppConfig() {}

    public AppConfig(String repositoryIdentifier, String domain, String stanza) {
        this.repositoryIdentifier = repositoryIdentifier;
        this.domain = domain;
        this.stanza = stanza;
    }

    public String getRepositoryIdentifier() { return repositoryIdentifier; }
    public String getDomain()               { return domain; }
    public String getStanza()               { return stanza; }

    public void setRepositoryIdentifier(String repositoryIdentifier) { this.repositoryIdentifier = repositoryIdentifier; }
    public void setDomain(String domain)                             { this.domain = domain; }
    public void setStanza(String stanza)                             { this.stanza = stanza; }
}
