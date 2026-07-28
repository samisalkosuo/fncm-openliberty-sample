// app-header.js — custom element for the application header
// Usage: <app-header app-name="My App"></app-header>
// To change the title, update the app-name attribute in index.html.
class AppHeader extends HTMLElement {
  connectedCallback() {
    const appName = this.getAttribute('app-name') ?? 'FileNet App';
    this.innerHTML = `
      <header>
        <h1>${appName}</h1>
        <span id="user-info" class="hidden">
          <span id="user-name"></span>
          &nbsp;|&nbsp;
          <button id="logout-btn">Log out</button>
        </span>
      </header>`;
  }
}

customElements.define('app-header', AppHeader);
