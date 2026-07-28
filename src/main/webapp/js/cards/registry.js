// cards/registry.js — card registration and mounting
//
// Cards call registerCard(definition) at module load time.
// main.js calls mountAllCards(gridEl) once the DOM is ready.
// Display order follows import order in main.js.

const _cards = [];

/**
 * Register a card definition. Called by each card module.
 * @param {{ id: string, size?: string, html: () => string, init: () => void, runAfterLogin?: boolean, run?: () => void | Promise<void> }} definition
 *   id            — feature slug, e.g. "connection-test"
 *   size          — optional grid span: "wide" | "tall" | "large" | "full" (default: normal)
 *   html          — function returning the card's inner HTML string
 *   init          — function that wires up event listeners (called after the HTML is in the DOM)
 *   runAfterLogin — optional flag to auto-run the card after authentication is ready
 *   run           — optional shared action callable by both lifecycle hooks and UI events
 */
export function registerCard(definition) {
  _cards.push(definition);
}

/**
 * Inject all registered card HTML into gridEl, then run each card's init().
 * @param {HTMLElement} gridEl
 */
export function mountAllCards(gridEl) {
  for (const card of _cards) {
    const wrapper = document.createElement('div');
    wrapper.innerHTML = card.html().trim();
    const cardEl = wrapper.firstElementChild;

    // Apply grid-size modifier declared by the card
    if (card.size && card.size !== 'normal') {
      cardEl.dataset.size = card.size;
    }

    gridEl.appendChild(cardEl);
    card.init();
  }
}

export function runPostLoginCards() {
  for (const card of _cards) {
    if (card.runAfterLogin && card.run) {
      card.run();
    }
  }
}
