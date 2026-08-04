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

  /**
   * Generic runtime key-value store — survives page reloads automatically.
   * Write: session.setState('reservationId', id)
   * Read:  session.getState('reservationId')
   * Any key is valid; no schema changes needed for future values.
   */
  state: {},

  setState(key, value) {
    this.state[key] = value;
    const stored = JSON.parse(sessionStorage.getItem('session') ?? '{}');
    stored.state = { ...stored.state, [key]: value };
    sessionStorage.setItem('session', JSON.stringify(stored));
  },

  getState(key, defaultValue = null) {
    return key in this.state ? this.state[key] : defaultValue;
  },

  clearState(key) {
    delete this.state[key];
    const stored = JSON.parse(sessionStorage.getItem('session') ?? '{}');
    if (stored.state) delete stored.state[key];
    sessionStorage.setItem('session', JSON.stringify(stored));
  },

  save(data) {
    this.appToken    = data.appToken;
    this.accessToken = data.accessToken;
    this.username    = data.username;
    this.config      = data.config ?? null;
    this.state       = data.state  ?? {};
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
      this.state       = d.state  ?? {};
      return true;
    }
    return false;
  },

  clear() {
    this.appToken = this.accessToken = this.username = this.config = null;
    this.state = {};
    sessionStorage.removeItem('session');
  },
};
