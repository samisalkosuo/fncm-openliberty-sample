// session.js — save / load / clear session state
export const session = {
  appToken:    null,
  accessToken: null,
  username:    null,
  /**
   * Non-sensitive server configuration, populated after login.
   * Properties:
   *   config.repositoryIdentifier  — FileNet object store name (filenet.objectstore)
   *   config.domain                — FileNet domain name     (filenet.domain)
   *   config.stanza                — FileNet stanza name     (filenet.stanza)
   *
   * Usage in a card:
   *   import { session } from '../session.js';
   *   const vars = { repositoryIdentifier: session.config.repositoryIdentifier };
   */
  config: null,

  save(data) {
    this.appToken    = data.appToken;
    this.accessToken = data.accessToken;
    this.username    = data.username;
    this.config      = data.config ?? null;
    sessionStorage.setItem('session', JSON.stringify(data));
  },

  load() {
    const raw = sessionStorage.getItem('session');
    if (raw) {
      const d = JSON.parse(raw);
      this.appToken    = d.appToken;
      this.accessToken = d.accessToken;
      this.username    = d.username;
      this.config      = d.config ?? null;
      return true;
    }
    return false;
  },

  clear() {
    this.appToken = this.accessToken = this.username = this.config = null;
    sessionStorage.removeItem('session');
  },
};
