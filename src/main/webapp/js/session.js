// session.js — save / load / clear session state
export const session = {
  appToken:    null,
  accessToken: null,
  username:    null,

  save(data) {
    this.appToken    = data.appToken;
    this.accessToken = data.accessToken;
    this.username    = data.username;
    sessionStorage.setItem('session', JSON.stringify(data));
  },

  load() {
    const raw = sessionStorage.getItem('session');
    if (raw) {
      const d = JSON.parse(raw);
      this.appToken    = d.appToken;
      this.accessToken = d.accessToken;
      this.username    = d.username;
      return true;
    }
    return false;
  },

  clear() {
    this.appToken = this.accessToken = this.username = null;
    sessionStorage.removeItem('session');
  },
};
